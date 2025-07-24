plugins {
    application
    id("org.graalvm.buildtools.native") version "0.10.6"
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(libs.guava)
    implementation("org.slf4j:slf4j-api:2.0.13")
    implementation("org.slf4j:slf4j-simple:2.0.13")
    implementation("org.projectlombok:lombok:1.18.34")
    implementation("io.github.bonede:tree-sitter:0.25.3")
    implementation("io.github.bonede:tree-sitter-java:0.23.4")
    implementation("info.picocli:picocli:4.7.7")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")

    annotationProcessor("info.picocli:picocli-codegen:4.7.7")
    annotationProcessor("org.projectlombok:lombok:1.18.34")

    testCompileOnly("org.projectlombok:lombok:1.18.34")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.34")
    testImplementation(libs.junit.jupiter)

    compileOnly("org.projectlombok:lombok:1.18.34")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass = "io.github.syntaxpresso.core.java.Main"
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
        events("passed", "skipped", "failed")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(22))
        vendor.set(JvmVendorSpec.GRAAL_VM)
    }
}

graalvmNative {
    testSupport.set(true)
    binaries {
        named("main") {
            mainClass.set("io.github.syntaxpresso.core.java.Main")
            buildArgs.add("--enable-native-access=ALL-UNNAMED")
        }
    }
    toolchainDetection.set(true)
}
