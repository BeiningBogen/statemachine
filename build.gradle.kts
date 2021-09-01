plugins {
    kotlin("multiplatform") version "1.5.30"
    id("maven-publish")
    id("com.android.library")
}

group = "no.beiningbogen"
version = "1.2.2-native-mt"

repositories {
    google()
    mavenCentral()
}

publishing {
    repositories {
        maven {
            url = uri("https://maven.pkg.jetbrains.space/beiningbogen/p/stmn/maven")
            name = "stateMachine"
            credentials(PasswordCredentials::class)
        }
    }
}

kotlin {
    android {
        publishLibraryVariants("release", "debug")
    }
    iosX64("ios") {
        binaries {
            framework {
                baseName = "SharedModels"
            }
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1-native-mt")
            }
        }
        val commonTest by getting

        val androidMain by getting
        val androidTest by getting {
            dependencies {
                implementation("app.cash.turbine:turbine:0.6.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.5.1")
                implementation(kotlin("test"))
            }
        }

        val iosMain by getting
        val iosTest by getting
    }
}

android {
    compileSdkVersion(30)

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets.getByName("main") {
        manifest.srcFile("src/androidMain/AndroidManifest.xml")
        java.srcDirs("src/androidMain/kotlin")
        res.srcDirs("src/androidMain/res")
    }
    sourceSets.getByName("test") {
        java.srcDirs("src/androidTest/kotlin")
        res.srcDirs("src/androidTest/res")
    }

    defaultConfig {
        minSdkVersion(24)
        targetSdkVersion(30)
    }
}
