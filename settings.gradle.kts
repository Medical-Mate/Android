pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // 카카오 SDK는 Maven Central에 없다. devrepo가 유일한 배포처다.
        // 그룹을 제한해 다른 의존성이 이 외부 Nexus를 조회하지 않게 한다.
        maven {
            url = uri("https://devrepo.kakao.com/nexus/content/groups/public/")
            content { includeGroup("com.kakao.sdk") }
        }
    }
}

rootProject.name = "Medical Mate"
include(":app")
