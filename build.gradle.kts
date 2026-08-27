plugins {
    id("java")
}

group = "com.cachorrovascaino"
version = "0.0.5"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(files("libs/HytaleServer.jar"))
    compileOnly(files("libs/MultipleHUD-1.0.8.jar"))
    implementation("org.yaml:snakeyaml:2.2")
}

tasks.jar {
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("libs/**")
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}


tasks.test {
    useJUnitPlatform()
}