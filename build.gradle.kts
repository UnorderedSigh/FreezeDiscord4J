plugins {
    application
    java
}

group = "freeze.bot"
version = "2023.12.24"

repositories {
    mavenCentral()
}

application {
    mainClass.set("freeze.bot.Simple")
}

sourceSets {
    all {
        dependencies {
            implementation("com.discord4j:discord4j-core:3.2.6")
            implementation("ch.qos.logback:logback-classic:1.2.3")
        }
    }
}

/*
Configure the sun.tools.jar.resources.jar task for our main class and so that `./gradlew build` always makes the fatjar
This boilerplate is completely removed when using Springboot
 */
tasks.jar {
    manifest {
        attributes("Main-Class" to "freeze.bot.Simple")
    }

    finalizedBy("shadowJar")
}
