plugins {
    java
    application
    id("com.adarshr.test-logger") version "4.0.0"
}

group = "syspro.tm.implementation"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://sysprolang.b-andrew.ru/repository")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(1, TimeUnit.HOURS)
}

dependencies {
    implementation("syspro.tm:RegexApp:+") {
        isChanging = true
    }
    testImplementation(platform("org.junit:junit-bom:5.14.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val enableNativeAccess = "--enable-native-access=syspro.tm.RegexApp"

tasks.test {
    useJUnitPlatform()
    jvmArgs(enableNativeAccess)
}

application {
    mainModule = "syspro.tm.RegexApp"
    mainClass = provider { null }
    applicationDefaultJvmArgs += listOf(enableNativeAccess)
}

tasks.register<JavaExec>("runBenchmarks") {
    group = ApplicationPlugin.APPLICATION_GROUP
    mainModule = application.mainModule
    val runTask = tasks.run.get()
    classpath = runTask.classpath
    javaLauncher = runTask.javaLauncher
    args("--no-tests")
    jvmArgs(application.applicationDefaultJvmArgs)
}