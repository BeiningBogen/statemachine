plugins {
    kotlin("multiplatform") version "1.4.10"
    id("maven-publish")
}
group = "no.beiningbogen"
version = "0.3.1"

repositories {
    mavenCentral()
    jcenter()
    google()
}

publishing {
    repositories {
        maven {
            url = uri("https://maven.pkg.jetbrains.space/beiningbogen/p/stmn/maven")
            credentials {
                username = project.extra["space_usr"].toString()
                password = project.extra["space_pwd"].toString()
            }
        }
    }
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
    }
    js {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                    webpackConfig.cssSupport.enabled = true
                }
            }
        }
    }

    ios {
        binaries {
            framework {
                baseName = "SharedCode"
            }
        }
    }

    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.0-RC")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9-native-mt")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation(kotlin("test-junit"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.3.8")
                implementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
                implementation("org.mockito:mockito-inline:3.3.3")
                implementation("androidx.arch.core:core-testing:2.1.0")
                implementation("androidx.test:core:1.0.0")
                implementation("androidx.test.ext:junit:1.1.1")
                implementation("junit:junit:4.12")
            }
        }
        val jsMain by getting
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }

        val iosMain by getting
        val iosTest by getting

        val nativeMain by getting
        val nativeTest by getting
    }
}