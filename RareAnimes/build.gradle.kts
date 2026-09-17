version = 1

cloudstream {
        language = "hi"
            description = "RareAnimes — Hindi Dubbed Anime & Cartoon"
                authors = listOf("megix")
                    status = 1
                        tvTypes = listOf("Anime", "TvSeries", "Movie", "Cartoon")
                            iconUrl = "https://www.rareanimes.mov/favicon.ico"
}

android {
        namespace = "com.megix"
            compileSdk = 34

                defaultConfig {
                            minSdk = 21
                }

                    compileOptions {
                                sourceCompatibility = JavaVersion.VERSION_1_8
                                        targetCompatibility = JavaVersion.VERSION_1_8
                    }

                        kotlinOptions {
                                    jvmTarget = "1.8"
                                            freeCompilerArgs += listOf(
                                                            "-Xno-call-assertions",
                                                                        "-Xno-param-assertions",
                                                                                    "-Xno-receiver-assertions"
                                            )
                        }
}

dependencies {
        val cloudstream by configurations
            cloudstream("com.lagradost:cloudstream3:pre-release")

                implementation(kotlin("stdlib"))
                    implementation("com.github.Blatzar:NiceHttp:0.4.18")
                        implementation("org.jsoup:jsoup:1.17.2")
                            implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1")
                                implementation("com.squareup.okhttp3:okhttp:4.12.0")
                                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
                                        implementation("org.mozilla:rhino:1.7.14")
                                            implementation("androidx.annotation:annotation:1.7.0")
                                                implementation("androidx.browser:browser:1.8.0")
                                                    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}
