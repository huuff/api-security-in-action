plugins {
    kotlin("jvm") version "1.6.10"
    id("org.jetbrains.kotlinx.kover") version "0.5.0"
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
    jvmArgs("--add-opens", "java.base/java.time=ALL-UNNAMED")
    finalizedBy(tasks.koverHtmlReport)
}

val kotestVersion = "5.3.0"
dependencies {
    implementation(kotlin("stdlib"))

    implementation("com.h2database:h2:1.4.197")?.because("Can't update or tests break")
    implementation("com.sparkjava:spark-core:2.9.3")
    implementation("org.json:json:20220320")
    implementation("org.dalesbred:dalesbred:1.3.5")
    implementation("org.slf4j:slf4j-simple:1.7.36")
    implementation("org.kodein.di:kodein-di:7.11.0")
    implementation("com.google.guava:guava:31.1-jre")
    implementation("com.lambdaworks:scrypt:1.4.0")

    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-json:$kotestVersion")
    testImplementation("io.mockk:mockk:1.12.4")
}