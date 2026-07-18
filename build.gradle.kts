plugins {
    id("java-library")
    id("maven-publish")
}

group = "dev.rajce"
version = "1.0.0"

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    // Používáme compileOnly, aby se Paper API necpalo do výsledného JARu knihovny
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
}

// Konfigurace Java kompilace (Folia striktně vyžaduje Javu 17 nebo novější)
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(17)
}

// Klíčový blok pro JitPack, který mu říká, jak má knihovnu správně vypublikovat
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}