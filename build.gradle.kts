plugins {
    java
}

group = "net.pluginsmith"
version = "1.0.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io") // For VaultAPI
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") // For PlaceholderAPI
    maven("https://repo.discordsrv.com/") // For DiscordSRV
    maven("https://repo.luckperms.net/") // For LuckPerms
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6") // PlaceholderAPI
    compileOnly("com.discordsrv:DiscordSRV:1.26.0") // DiscordSRV
    compileOnly("net.luckperms:api:5.4") // LuckPerms API
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1") // VaultAPI

    // GSON for JSON serialization/deserialization
    implementation("com.google.code.gson:gson:2.10.1")
}

tasks.jar {
    archiveBaseName.set("Monetization")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}