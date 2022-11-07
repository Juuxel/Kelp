plugins {
    java
}

group = "io.github.juuxel"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains:annotations:23.0.0")
    implementation("com.squareup.moshi:moshi:1.13.0")
    implementation("info.picocli:picocli:4.6.3")
    implementation("com.google.guava:guava:31.1-jre")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.withType<JavaCompile> {
    options.release.set(17)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
