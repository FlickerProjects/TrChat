plugins {
    java
    id("io.izzel.taboolib") version "1.34"
    id("org.jetbrains.kotlin.jvm") version "1.6.10"
}

taboolib {
    description {
        contributors {
            name("Arasple")
            name("ItsFlicker")
        }
        dependencies {
            name("PlaceholderAPI").optional(true).with("bukkit")
            name("Dynmap").optional(true).with("bukkit")
            name("DiscordSRV").optional(true).with("bukkit")
            name("ItemsAdder").optional(true).with("bukkit")
            name("EcoEnchants").optional(true).with("bukkit")
        }
        desc("Advanced Minecraft Chat Control")
    }
    install("common", "common-5")
    install("module-chat")
    install("module-configuration")
    install("module-database")
    install("module-kether")
    install("module-lang")
    install("module-metrics")
    install("module-nms", "module-nms-util")
    install("module-porticus")
    install("module-ui")
    install("platform-bukkit", "platform-bungee", "platform-velocity")
    install("expansion-command-helper", "expansion-player-database", "expansion-javascript")
    classifier = null
    version = "6.0.7-41"
}

repositories {
    maven("https://nexus.velocitypowered.com/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("http://repo.mikeprimm.com/") { isAllowInsecureProtocol = true }
    maven("https://jitpack.io")
    mavenLocal()
    mavenCentral()
}

dependencies {
    compileOnly("net.md-5:bungeecord-bootstrap:1.17-R0.1-SNAPSHOT")
    compileOnly("com.velocitypowered:velocity-api:3.0.0")

    compileOnly("ink.ptms.core:v11802:11802:mapped")
    compileOnly("ink.ptms.core:v11802:11802:universal")
    compileOnly("ink.ptms:nms-all:1.0.0")

    compileOnly("us.dynmap:dynmap-api:2.5") { isTransitive = false }
    compileOnly("me.clip:placeholderapi:2.10.9") { isTransitive = false }
    compileOnly("com.willfp:eco:6.6.3") { isTransitive = false }

    compileOnly(kotlin("stdlib"))
    compileOnly(fileTree("libs"))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}