import java.net.URI
import java.security.MessageDigest

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// 카카오 네이티브 앱 키. local.properties(커밋 대상 아님) 또는 환경변수에서 읽는다.
// providers API를 쓰는 이유는 configuration cache가 입력을 추적하게 하기 위함이다.
// 키가 없어도 빌드는 통과시키고, 실패는 런타임 KakaoSdk 호출에서만 나게 한다.
/**
 * 백엔드 base URL. local.properties나 환경변수로 덮어쓸 수 있다.
 * 기본값은 배포된 dev 서버이며, 로컬 서버를 붙일 때 바꾼다.
 */
val backendBaseUrl: String =
    providers
        .fileContents(rootProject.layout.projectDirectory.file("local.properties"))
        .asText
        .map { text ->
            text
                .lineSequence()
                .map(String::trim)
                .firstOrNull { it.startsWith("BACKEND_BASE_URL=") }
                ?.substringAfter("=")
                ?.trim()
                .orEmpty()
        }
        .filter(String::isNotBlank)
        .orElse(providers.environmentVariable("BACKEND_BASE_URL"))
        .getOrElse("https://d3f36x6ccm838d.cloudfront.net/")

val kakaoNativeAppKey: String =
    providers
        .fileContents(rootProject.layout.projectDirectory.file("local.properties"))
        .asText
        .map { text ->
            text
                .lineSequence()
                .map(String::trim)
                .firstOrNull { it.startsWith("KAKAO_NATIVE_APP_KEY=") }
                ?.substringAfter("=")
                ?.trim()
                .orEmpty()
        }
        // local.properties가 있어도 키 줄이 없으면 빈 문자열이 되므로,
        // filter로 값을 비워 환경변수(CI) 폴백이 실제로 동작하게 한다.
        .filter(String::isNotBlank)
        .orElse(providers.environmentVariable("KAKAO_NATIVE_APP_KEY"))
        .getOrElse("")

/**
 * 온디바이스 엔진 묶음을 내려받아 푼다.
 *
 * **저장소에 커밋하지 않는다.** arm64 라이브러리만 50MB가 넘고, 우리가 만든 코드가 아니라 AI
 * 트랙이 llama.cpp 업스트림을 Snapdragon 툴체인으로 빌드해 릴리즈로 올린 산출물이다. 태그와
 * SHA256을 버전 카탈로그에 박아 두면 어느 빌드가 어느 바이너리를 쓴 것인지 남는다. 해시가
 * 다르면 실패시킨다. 빌드가 통과했다는 것이 그 바이너리였다는 뜻이어야 한다.
 */
abstract class FetchOnDeviceEngine : DefaultTask() {
    @get:Input
    abstract val tag: Property<String>

    @get:Input
    abstract val sha256: Property<String>

    /** `jniLibs/arm64-v8a`로 갈 arm64 라이브러리. */
    @get:Input
    abstract val nativeLibraries: ListProperty<String>

    /** 자산으로 갈 Hexagon 이미지. */
    @get:Input
    abstract val htpImages: ListProperty<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    /**
     * 내려받은 묶음을 두는 곳.
     *
     * 출력 폴더 밖에 둔다. `build/`에 두면 clean마다 62MB를 다시 받고, CI는 잡마다 새로
     * 받는다. Gradle 사용자 홈의 캐시 아래면 CI의 Gradle 캐시가 그대로 안고 간다.
     */
    @get:Internal
    abstract val archiveFile: RegularFileProperty

    @get:Inject
    abstract val archives: ArchiveOperations

    @get:Inject
    abstract val fs: FileSystemOperations

    @TaskAction
    fun fetch() {
        val root = outputDir.get().asFile
        root.mkdirs()
        val archive = archiveFile.get().asFile
        archive.parentFile.mkdirs()

        if (!archive.isFile || digestOf(archive) != sha256.get()) {
            val name = tag.get()
            val url = "https://github.com/Medical-Mate/AI/releases/download/$name/$name.tar.gz"
            logger.lifecycle("온디바이스 엔진을 내려받는다: $url")
            URI(url).toURL().openStream().use { input -> archive.outputStream().use(input::copyTo) }
        }

        val actual = digestOf(archive)
        check(actual == sha256.get()) { "엔진 묶음 해시가 다르다. 기대 ${sha256.get()}, 실제 $actual" }

        val tree = archives.tarTree(archives.gzip(archive))
        unpack(tree, nativeLibraries.get().map { "**/lib/$it" }, File(root, "jniLibs/arm64-v8a"))
        unpack(tree, htpImages.get().map { "**/lib/$it" }, File(root, "assets/ondevice/htp"))
        unpack(tree, listOf("**/include/*.h"), File(root, "include"))
    }

    /** 묶음 안 경로를 버리고 파일만 꺼낸다. CMake와 AGP가 평평한 폴더를 본다. */
    private fun unpack(tree: FileTree, patterns: List<String>, into: File) {
        fs.copy {
            from(tree) {
                include(patterns)
                eachFile { path = name }
            }
            into(into)
            includeEmptyDirs = false
        }
    }

    private fun digestOf(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(1 shl 16)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

val onDeviceEngineDir: Provider<Directory> = layout.buildDirectory.dir("ondevice")

/**
 * 엔진에서 앱으로 가져오는 것.
 *
 * `libllama-common`·`libmtmd`와 `*-impl`은 뺀다. 묶음의 CLI 도구가 쓰는 것이고 앱은 추론만
 * 한다. 다 넣으면 네이티브만 170MB다.
 *
 * **`libggml-opencl`은 뺄 수 없다.** 쓰지 않는 백엔드인데도 `libggml.so`와 `libllama.so`가
 * `DT_NEEDED`로 직접 걸고 있다. 빼고 넣었더니 기기에서 `dlopen failed: library
 * "libggml-opencl.so" not found`로 엔진이 아예 열리지 않았다. 백엔드 등록은 런타임이지만
 * 링크는 빌드 때 박힌 것이라 고를 수 있는 것이 아니다.
 */
val engineNativeLibraries =
    listOf(
        "libllama.so",
        "libggml.so",
        "libggml-base.so",
        "libggml-cpu.so",
        "libggml-hexagon.so",
        "libggml-opencl.so",
    )

/**
 * HTP 이미지는 `jniLibs`에 넣을 수 없다.
 *
 * **ELF32 Hexagon(QDSP6) 바이너리다.** arm64-v8a 폴더에 두면 AGP가 arm64 strip을 걸고 로더도
 * 읽지 못한다. DSP 로더는 `ADSP_LIBRARY_PATH`가 가리키는 디렉터리에서 파일로 찾아가므로,
 * 자산으로 싣고 첫 실행 때 앱 폴더에 풀어 그 경로를 넘긴다.
 *
 * v75만 싣는다. Snapdragon 8 Gen 3이 v75다. v73·v79·v81은 다른 세대용이라 기기를 넓힐 때 더한다.
 */
val engineHtpImages = listOf("libggml-htp-v75.so")

val fetchOnDeviceEngine =
    tasks.register<FetchOnDeviceEngine>("fetchOnDeviceEngine") {
        description = "온디바이스 엔진 묶음을 내려받아 jniLibs와 자산으로 푼다."
        tag.set(libs.versions.onDeviceEngine)
        sha256.set(libs.versions.onDeviceEngineSha256)
        nativeLibraries.set(engineNativeLibraries)
        htpImages.set(engineHtpImages)
        outputDir.set(onDeviceEngineDir)
        archiveFile.set(
            File(gradle.gradleUserHomeDir, "caches/medicalmate-ondevice/${libs.versions.onDeviceEngine.get()}.tar.gz"),
        )
    }

android {
    namespace = "com.mist.medicalmate"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.mist.medicalmate"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 매니페스트의 kakao${KAKAO_NATIVE_APP_KEY} 스킴 치환용.
        manifestPlaceholders["KAKAO_NATIVE_APP_KEY"] = kakaoNativeAppKey
        // KakaoSdk.init()에 넘길 값. manifestPlaceholders와는 별개 통로다.
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$kakaoNativeAppKey\"")
        buildConfigField("String", "BACKEND_BASE_URL", "\"$backendBaseUrl\"")

        // 엔진이 arm64만 있다. 다른 ABI를 넣으면 그 기기에서 라이브러리가 비는 APK가 된다.
        ndk {
            abiFilters += "arm64-v8a"
        }
        externalNativeBuild {
            cmake {
                arguments += "-DONDEVICE_ENGINE_DIR=${onDeviceEngineDir.get().asFile.invariantSeparatorsPath}"
                // 엔진이 c++_shared로 빌드돼 있다. 기본값 c++_static을 쓰면 STL이 두 벌이 된다.
                arguments += "-DANDROID_STL=c++_shared"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    // AGP 9가 소스 디렉터리에 Provider를 받지 않는다(`android.sourceset.disallowProvider`).
    // 경로를 그대로 주고 태스크 의존은 아래 preBuild에서 따로 건다.
    sourceSets.named("main") {
        jniLibs.srcDir(onDeviceEngineDir.get().dir("jniLibs"))
        assets.srcDir(onDeviceEngineDir.get().dir("assets"))
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // minSdk 24에서 java.time을 쓰려면 필요하다. 켜지 않으면 lintDebug가
        // NewApi 오류로 막는다(API 26 요구). 이 앱은 일정·복용 알림·카드 작성일까지
        // 날짜를 계속 다뤄서 우회하면 나중에 타입을 전부 바꿔야 한다.
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
        // buildConfigField를 쓰려면 켜야 한다. AGP 8부터 기본값이 false다.
        buildConfig = true
    }
}

// 소스 디렉터리로 등록만 하면 AGP가 태스크 의존을 이어주지 않는다. 엔진이 풀리기 전에
// jniLibs·자산 병합이나 CMake 구성이 돌면 빈 폴더를 본다.
tasks.named("preBuild") {
    dependsOn(fetchOnDeviceEngine)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    parallel = true
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = JavaVersion.VERSION_11.toString()
    reports {
        html.required.set(true)
        sarif.required.set(true)
        xml.required.set(false)
        txt.required.set(false)
        md.required.set(false)
    }
}

ktlint {
    ignoreFailures.set(false)
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.hilt.android)
    implementation(libs.kakao.user)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
