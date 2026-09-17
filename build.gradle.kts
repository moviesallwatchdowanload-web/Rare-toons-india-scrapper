// Top-level build file
buildscript {
            repositories {
                        google()
                                mavenCentral()
                                        maven("https://jitpack.io")
            }
                dependencies {
                                classpath("com.android.tools.build:gradle:8.1.4")
                                        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
                                                classpath("com.lagradost:cloudstream3:pre-release")
                }
}

allprojects {
            repositories {
                        google()
                                mavenCentral()
                                        maven("https://jitpack.io")
            }
}

tasks.register<Delete>("clean") {
            delete(rootProject.buildDir)
}