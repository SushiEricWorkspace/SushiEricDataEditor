plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.javafx)
    application
}

group = "io.github.rs0325.sushiericdataeditor2"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(project(":Common"))

    // GUI 関連ライブラリ
    implementation("org.controlsfx:controlsfx:11.2.1")
    implementation("org.kordamp.ikonli:ikonli-javafx:12.3.1")
    implementation("org.kordamp.ikonli:ikonli-fontawesome5-pack:12.3.1")

    // データ・通信関連
    implementation("com.hierynomus:sshj:0.39.0")
    implementation("org.yaml:snakeyaml:2.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // ロギング
    implementation("org.slf4j:slf4j-api:2.0.12")
    implementation("ch.qos.logback:logback-classic:1.5.3")

    implementation("org.bouncycastle:bcprov-jdk18on:1.78")

    testImplementation(kotlin("test"))
}

kotlin { jvmToolchain(21) }

javafx {
    version = libs.versions.javafx.get()
    modules("javafx.controls", "javafx.fxml")
}

application {
    mainClass.set("io.github.rs0325.sushiericdataeditor2.app.Launcher")
}

tasks.test {
    useJUnitPlatform()
}

val appName = "SushiEricDataEditor"

// GitHub Releases、update.json、AppVersion.CURRENTと合わせるアプリ側のバージョン。
val releaseVersion = "0.2.0"

// jpackageに渡すパッケージ用バージョン。
// macOSのjpackageでは、最初の数字を0にできないため1以上にする。
val packageVersion = "1.2.0"

val mainJarName = "SushiEricDataEditor2-1.0-SNAPSHOT.jar"
val mainClassName = "io.github.rs0325.sushiericdataeditor2.app.Launcher"

// jpackageに渡す入力フォルダ。
// installDistで生成されたlibフォルダを指定する。
val packageInputDir = "SushiEricDataEditor2/build/install/SushiEricDataEditor2/lib"

// 配布物の出力先。
val appImageOutputDir = rootProject.layout.buildDirectory.dir("release").get().asFile.absolutePath
val installerOutputDir = rootProject.layout.buildDirectory.dir("installer").get().asFile.absolutePath
val releaseInstallerOutputDir = rootProject.layout.buildDirectory.dir("release-installer").get().asFile.absolutePath

// Windows用アイコン。
// exe本体、ショートカット、スタートメニューのアイコンに使われる。
val windowsIconPath = "SushiEricDataEditor2/src/main/resources/icon/app.ico"

// macOS用アイコン。
// .app、.dmg作成時のアプリアイコンに使われる。
val macIconPath = "SushiEricDataEditor2/src/main/resources/icon/app.icns"

// jpackageが生成するWindowsインストーラー名。
// 基本的に「アプリ名-パッケージバージョン.exe」になる。
val windowsInstallerBaseName = "$appName-$packageVersion.exe"

// GitHub Releasesなどに置くためのリリース用インストーラー名。
val windowsInstallerReleaseName = "$appName-$releaseVersion-Windows-Installer.exe"

// jpackageが生成するmacOS dmg名。
// 基本的に「アプリ名-パッケージバージョン.dmg」になる。
val macDmgBaseName = "$appName-$packageVersion.dmg"

// GitHub Releasesなどに置くためのリリース用dmg名。
val macDmgReleaseName = "$appName-$releaseVersion-macOS-Installer.dmg"
/**
 * 現在のアプリ名のapp-image出力だけを削除する。
 *
 * appNameを変更した場合、古い名前のフォルダは残るため、
 * その場合は手動で削除する。
 */
tasks.register<Delete>("cleanAppImageOutput") {
    group = "release"
    description = "現在のアプリ名のapp-image出力を削除します"

    delete("$appImageOutputDir/$appName")
}

/**
 * Windows用インストーラーの出力先を削除する。
 *
 * 古いインストーラーが残っていると紛らわしいため、
 * インストーラー作成前に削除する。
 */
tasks.register<Delete>("cleanWindowsInstallerOutput") {
    group = "release"
    description = "Windows用インストーラー出力を削除します"

    delete(installerOutputDir)
    delete(releaseInstallerOutputDir)
}

/**
 * macOS用インストーラーの出力先を削除する。
 *
 * 古いdmgが残っていると紛らわしいため、
 * dmg作成前に削除する。
 */
tasks.register<Delete>("cleanMacInstallerOutput") {
    group = "release"
    description = "macOS用インストーラー出力を削除します"

    delete(installerOutputDir)
    delete(releaseInstallerOutputDir)
}

/**
 * Windows用のインストール不要アプリフォルダを作成する。
 *
 * 出力例:
 * build/release/SushiEricDataEditor/SushiEricDataEditor.exe
 *
 * これはインストーラーではなく、フォルダごと配布する形式。
 * 動作確認やzip配布に使う。
 */
tasks.register<Exec>("packageWindowsAppImage") {
    group = "release"
    description = "Windows用のJavaランタイム同梱アプリフォルダを作成します"

    dependsOn("cleanAppImageOutput", "installDist")

    workingDir = rootProject.projectDir

    commandLine(
        "jpackage",
        "--type", "app-image",
        "--name", appName,
        "--app-version", packageVersion,
        "--input", packageInputDir,
        "--main-jar", mainJarName,
        "--main-class", mainClassName,
        "--dest", appImageOutputDir,
        "--icon", windowsIconPath
    )
}

/**
 * Windows用exeインストーラーを作成する。
 *
 * 出力例:
 * build/installer/SushiEricDataEditor-1.0.0.exe
 *
 * --win-menu:
 * スタートメニューに登録する。
 *
 * --win-shortcut:
 * デスクトップショートカットを作成する。
 *
 * --win-per-user-install:
 * ユーザー単位インストールにする。
 * 自動アップデートでProgram Filesの権限問題を避けやすくするため。
 */
tasks.register<Exec>("packageWindowsInstaller") {
    group = "release"
    description = "Windows用exeインストーラーを作成します"

    dependsOn("cleanWindowsInstallerOutput", "installDist")

    workingDir = rootProject.projectDir

    commandLine(
        "jpackage",
        "--type", "exe",
        "--name", appName,
        "--app-version", packageVersion,
        "--input", packageInputDir,
        "--main-jar", mainJarName,
        "--main-class", mainClassName,
        "--dest", installerOutputDir,
        "--icon", windowsIconPath,
        "--win-menu",
        "--win-shortcut",
        "--win-per-user-install"
    )
}

/**
 * Windows用インストーラーをリリース用ファイル名へコピー、リネームする。
 *
 * 入力:
 * build/installer/SushiEricDataEditor-1.0.0.exe
 *
 * 出力:
 * build/release-installer/SushiEricDataEditor-0.1.0-Installer.exe
 */
tasks.register<Copy>("renameWindowsInstaller") {
    group = "release"
    description = "Windows用インストーラーをリリース用ファイル名へ変更します"

    dependsOn("packageWindowsInstaller")

    from(installerOutputDir) {
        include(windowsInstallerBaseName)
        rename {
            windowsInstallerReleaseName
        }
    }

    into(releaseInstallerOutputDir)
}

/**
 * Windows用リリースインストーラーを作成するためのまとめタスク。
 *
 * GitHub Releasesに置くWindows用インストーラーを作る場合は、
 * 基本的にこのタスクだけ実行する。
 */
tasks.register("releaseWindowsInstaller") {
    group = "release"
    description = "Windows用インストーラーを作成し、リリース用ファイル名で出力します"

    dependsOn("renameWindowsInstaller")
}

/**
 * macOS用の.appアプリを作成する。
 *
 * このタスクはMac上で実行する。
 * Windows上ではmacOS用のapp-imageは作成できない。
 *
 * 出力例:
 * build/release/SushiEricDataEditor.app
 */
tasks.register<Exec>("packageMacAppImage") {
    group = "release"
    description = "macOS用の.appアプリを作成します"

    dependsOn("cleanAppImageOutput", "installDist")

    workingDir = rootProject.projectDir

    commandLine(
        "jpackage",
        "--type", "app-image",
        "--name", appName,
        "--app-version", packageVersion,
        "--input", packageInputDir,
        "--main-jar", mainJarName,
        "--main-class", mainClassName,
        "--dest", appImageOutputDir,
        "--icon", macIconPath
    )
}

/**
 * macOS用dmgを作成する。
 *
 * このタスクはMac上で実行する。
 * GitHub Releasesに置くmacOS版は基本的にdmgを使う。
 *
 * 出力例:
 * build/installer/SushiEricDataEditor-1.0.0.dmg
 */
tasks.register<Exec>("packageMacDmg") {
    group = "release"
    description = "macOS用dmgを作成します"

    dependsOn("cleanMacInstallerOutput", "installDist")

    workingDir = rootProject.projectDir

    commandLine(
        "jpackage",
        "--type", "dmg",
        "--name", appName,
        "--app-version", packageVersion,
        "--input", packageInputDir,
        "--main-jar", mainJarName,
        "--main-class", mainClassName,
        "--dest", installerOutputDir,
        "--icon", macIconPath
    )
}

/**
 * macOS用dmgをリリース用ファイル名へコピー、リネームする。
 *
 * 入力:
 * build/installer/SushiEricDataEditor-1.0.0.dmg
 *
 * 出力:
 * build/release-installer/SushiEricDataEditor-0.1.0-macOS.dmg
 */
tasks.register<Copy>("renameMacDmg") {
    group = "release"
    description = "macOS用dmgをリリース用ファイル名へ変更します"

    dependsOn("packageMacDmg")

    from(installerOutputDir) {
        include(macDmgBaseName)
        rename {
            macDmgReleaseName
        }
    }

    into(releaseInstallerOutputDir)
}

/**
 * macOS用リリースdmgを作成するためのまとめタスク。
 *
 * MacでPullしたあと、GitHub Releasesに置くmacOS用dmgを作る場合は、
 * 基本的にこのタスクだけ実行する。
 */
tasks.register("releaseMacDmg") {
    group = "release"
    description = "macOS用dmgを作成し、リリース用ファイル名で出力します"

    dependsOn("renameMacDmg")
}

/*
Windowsで動作確認用アプリフォルダを作る:
packageWindowsAppImage

WindowsでGitHub Release用インストーラーを作る:
releaseWindowsInstaller

Macで動作確認用.appを作る:
packageMacAppImage

MacでGitHub Release用dmgを作る:
releaseMacDmg
 */