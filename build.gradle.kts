plugins {
    kotlin("jvm") version "1.6.10"
}

group = "xyz.haff"
version = "0.1.0"

repositories {
    mavenCentral()
}

tasks.wrapper {
    gradleVersion = "7.4"
}

tasks.test {
    useJUnitPlatform()
}

val kotestVersion = "5.3.0"
dependencies {
    implementation(kotlin("stdlib"))

    implementation("com.h2database:h2:1.4.197")
    implementation("com.sparkjava:spark-core:2.9.2")
    implementation("org.json:json:20200518")
    implementation("org.dalesbred:dalesbred:1.3.2")
    implementation("org.slf4j:slf4j-simple:1.7.30")
    implementation("org.kodein.di:kodein-di:7.11.0")
    implementation("com.google.guava:guava:31.1-jre")

    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.mockk:mockk:1.12.4")
}