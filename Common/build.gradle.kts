plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(kotlin("stdlib"))

    // Adventure API (マイクラ未起動でも動作)
    val adventureVersion = "4.17.0"
    api("net.kyori:adventure-api:${adventureVersion}")
    api("net.kyori:adventure-text-minimessage:${adventureVersion}")
    api(("net.kyori:adventure-text-serializer-plain:${adventureVersion}"))

    // Configurate (YAML + Adventureサポート)
    // これを使うと YAML ⇔ Component の変換が非常に楽になります
    val configurateVersion = "4.1.2"
    implementation("org.spongepowered:configurate-yaml:$configurateVersion")
    implementation("org.spongepowered:configurate-extra-kotlin:$configurateVersion")

    // jsonの読み込み用
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}

kotlin { jvmToolchain(21) }