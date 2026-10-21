rootProject.name = "build-logic"
include(":build-logic-utils", ":conventions")
project(":build-logic-utils").projectDir = file("utilities")
project(":conventions").projectDir = file("conventions")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
