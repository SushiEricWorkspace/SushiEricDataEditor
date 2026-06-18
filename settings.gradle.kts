rootProject.name = "SushiEricServerWorkspace"

include("Common")
include("SushiEricDataEditor2")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    // 「プロジェクト全体のリポジトリ設定を優先する」という設定
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }
}