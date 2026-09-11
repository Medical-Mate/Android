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
