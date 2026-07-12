plugins {
    id("java")
    id("io.github.goooler.shadow") version("8.1.8")
    id("xyz.jpenilla.run-paper") version("3.0.2")
    checkstyle
}

group = "dev.loki"
version = "1.3.3"
description = "LoParkour - Advanced parkour plugin for Minecraft"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    
    maven {
        name = "spigot-repo"
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }
    
    maven {
        name = "jitpack"
        url = uri("https://jitpack.io")
    }
    
    maven {
        name = "maven-central"
        url = uri("https://oss.sonatype.org/content/groups/public")
    }
    
    maven {
        name = "placeholderapi"
        url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }
    
    maven {
        name = "mv-repo"
        url = uri("https://repo.onarandombox.com/content/groups/public/")
    }
    
    maven {
        name = "dmulloy2-repo"
        url = uri("https://repo.dmulloy2.net/repository/public/")
    }
    
    maven {
        name = "codemc-repo"
        url = uri("https://repo.codemc.io/repository/maven-public/")
    }
    
    maven {
        name = "opencollab-snapshot-repo"
        url = uri("https://repo.opencollab.dev/maven-snapshots/")
    }
    
    // For ConfigUpdater dependency
    maven {
        name = "codemc-snapshots"
        url = uri("https://repo.codemc.io/repository/maven-snapshots/")
    }

    maven {
        name = "enginehub"
        url = uri("https://maven.enginehub.org/repo/")
    }
}

dependencies {
    // Spigot API
    compileOnly("org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT")
    
    // Adventure API (for Component)
    compileOnly("net.kyori:adventure-api:4.14.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.14.0")
    
    // LoLib Core
    implementation(files("libs/lolib-3.0.0.jar"))
    
    // Shaded dependencies
    implementation("io.papermc:paperlib:1.0.7")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.bstats:bstats-bukkit:3.2.1")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("com.mysql:mysql-connector-j:8.3.0")
    compileOnly("com.sk89q.worldedit:worldedit-core:7.3.6")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.6")

    // Provided dependencies (plugins)
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    compileOnly("me.filoghost.holographicdisplays:holographicdisplays-api:3.0.0-SNAPSHOT")
    compileOnly("org.geysermc.floodgate:api:2.0-SNAPSHOT")
    
    // Annotations
    compileOnly("org.jetbrains:annotations:24.1.0")
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0-M1")
    testImplementation("org.mockito:mockito-core:5.10.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.10.0")
    testImplementation("org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveFileName.set("LoParkour-${project.version}.jar")
    
    // Relocate shaded dependencies
    relocate("dev.lolib", "dev.loki.loparkour.lib.lolib")
    relocate("io.papermc.lib", "dev.loki.loparkour.lib.paperlib")
    relocate("com.google.gson", "dev.loki.loparkour.lib.gson")
    relocate("org.bstats", "dev.loki.loparkour.lib.bstats")

    // Don't minimize - causes issues with Caffeine's dynamically generated classes
    // minimize {
    //     exclude(dependency("com.zaxxer:HikariCP:.*"))
    //     exclude(dependency("com.mysql:mysql-connector-j:.*"))
    //     exclude(dependency(files("libs/lolib-3.0.0.jar")))
    // }
}

tasks.named("build") {
    dependsOn(tasks.named("shadowJar"))
}

tasks.withType<ProcessResources> {
    filesMatching(listOf("plugin.yml", "config.yml")) {
        expand(
            "version" to project.version,
            "description" to project.description
        )
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release = 21
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}

// Run paper server via gradlew runServer
tasks {
    runServer {
        minecraftVersion("1.21.4")
        jvmArgs("-Xms2G", "-Xmx2G")
        runDirectory.set(layout.projectDirectory.dir("run"))

        downloadPlugins {
            modrinth("placeholderapi", "2.11.6")
        }
    }
}

checkstyle {
    toolVersion = "10.21.4"
    configFile = file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = true
    maxErrors = 0
    maxWarnings = 400
}

tasks.withType<Checkstyle>().configureEach {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

// Default task
defaultTasks("clean", "build")
