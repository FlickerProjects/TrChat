plugins {
    `java-library`
    id("io.izzel.taboolib") version "1.34"
    id("org.jetbrains.kotlin.jvm") version "1.6.10"
}

taboolib {
    description {
        contributors {
            name("Arasple")
            name("ItsFlicker").description("Maintainer")
        }
        dependencies {
            name("PlaceholderAPI").with("bukkit")
            name("EcoEnchants").with("bukkit").optional(true)
            name("ItemsAdder").with("bukkit").optional(true)
            name("InteractiveChat").with("bukkit").optional(true)
        }
        desc("Advanced Minecraft Chat Control")
    }
    install(
        "common",
        "common-5",
        "module-chat",
        "module-configuration",
        "module-database",
        "module-database-mongodb",
        "module-kether",
        "module-lang",
        "module-metrics",
        "module-nms",
        "module-nms-util",
        "module-ui",
        "platform-bukkit",
        "platform-bungee",
        "platform-velocity",
        "expansion-command-helper",
        "expansion-javascript"
    )
    classifier = null
    version = "6.0.7-47"
}

repositories {
    maven("https://nexus.velocitypowered.com/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://jitpack.io")
    mavenCentral()
}

dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly("net.kyori:adventure-api:4.10.0")
    compileOnly("net.kyori:adventure-platform-bukkit:4.1.0")
    compileOnly("net.kyori:adventure-platform-bungeecord:4.1.0")

    compileOnly("ink.ptms.core:v11800:11800:api")
    compileOnly("ink.ptms.core:v11800:11800:mapped")
    compileOnly("ink.ptms.core:v11800:11800:universal")
    compileOnly("ink.ptms:nms-all:1.0.0")

    compileOnly("net.md-5:bungeecord-bootstrap:1.17-R0.1-SNAPSHOT")
    compileOnly("com.velocitypowered:velocity-api:3.0.0")

    compileOnly("com.willfp:eco:6.6.3") { isTransitive = false }
    compileOnly("me.clip:placeholderapi:2.10.9") { isTransitive = false }

    compileOnly(fileTree("libs"))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}