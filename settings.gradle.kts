rootProject.name = "SushiEricDataEditor"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        maven {
            name = "commonDevelopment"
            url = uri("../.common-dev-repository")
            content {
                includeModule(
                    "io.github.sushiericworkspace",
                    "sushieric-common-editor-dev"
                )
            }
        }
        mavenCentral()
    }
}
