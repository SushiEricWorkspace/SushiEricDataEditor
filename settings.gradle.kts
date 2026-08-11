rootProject.name = "SushiEricDataEditor"

includeBuild("../Common")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }
}