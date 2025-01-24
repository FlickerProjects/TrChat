taboolib {
    description {
        name(rootProject.name)
        desc("Advanced Minecraft Chat Control")
        links {
            name("homepage").url("https://trchat.trixey.cc/")
        }
        contributors {
            name("Arasple")
            name("ItsFlicker")
        }
        dependencies {
            name("PlaceholderAPI").with("bukkit")
            name("DiscordSRV").with("bukkit").optional(true)
            name("EcoEnchants").with("bukkit").optional(true)
            name("ItemsAdder").with("bukkit").optional(true)
            name("Nova").with("bukkit").optional(true)
            name("Multiverse-Core").with("bukkit").loadafter(true)
            name("Geyser-Spigot").with("bukkit").loadafter(true)
        }
    }
    relocate("com.eatthepath.uuid.", "${rootProject.group}.library.uuid.")
//    relocate("com.electronwill.nightconfig", "com.electronwill.nightconfig_3_6_7")
}

dependencies {
    taboo("com.eatthepath:fast-uuid:0.2.0")
}

tasks {
    jar {
        // 构件名
        archiveBaseName.set(rootProject.name)
        // 打包子项目源代码
        rootProject.subprojects.forEach { from(it.sourceSets["main"].output) }
    }
    sourcesJar {
        // 构件名
        archiveBaseName.set(rootProject.name)
        // 打包子项目源代码
        rootProject.subprojects.forEach { from(it.sourceSets["main"].allSource) }
    }
}