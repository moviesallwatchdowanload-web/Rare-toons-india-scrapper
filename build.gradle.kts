// Top-level build file
buildscript {
        repositories {
                    google()
                            mavenCentral()
                                    maven("https://jitpack.io")
        }
            dependencies {
                        classpath("com.android.tools.build:gradle:8.1.4")
                                classpath("com.github.recloudstream:gradle:81b1d424d2")
                                        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
            }
}

allprojects {
        repositories {
                    google()
                            mavenCentral()
                                    maven("https://jitpack.io")
        }
}

subprojects {
        afterEvaluate {
                    if (plugins.hasPlugin("com.android.library")) {
                                    extensions.findByName("android")?.let { ext ->
                                                    try {
                                                                            val extObj = ext as com.android.build.gradle.LibraryExtension
                                                                                                extObj.namespace = "com.megix"
                                                                                                                    extObj.compileSdk = 34
                                                                                                                                        extObj.defaultConfig.minSdk = 21
                                                    } catch (e: Exception) {
                                                                            // ignore
                                                    }
                                    }
                    }
        }
}

tasks.register<Delete>("clean") {
        delete(rootProject.buildDir)
}
                                                    