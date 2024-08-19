import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.22"
}

group = "me.jsedwards"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation("org.bytedeco:javacv-platform:1.5.10")

    implementation("org.deeplearning4j:deeplearning4j-core:1.0.0-M2.1")
    implementation("org.nd4j:nd4j-native-platform:1.0.0-M2.1")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.17.2")
    implementation("com.fasterxml.jackson.core:jackson-core:2.17.2")

    implementation("org.apache.commons:commons-math3:3.6.1")

    implementation("net.sourceforge.tess4j:tess4j:5.12.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}