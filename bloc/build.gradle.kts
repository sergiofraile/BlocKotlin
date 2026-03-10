plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dokka)
    `maven-publish`
}

group = "com.github.sergiofraile.BlocKotlin"
version = "1.0.0"

android {
    namespace = "dev.bloc"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // Compose runtime — only for the compose/ integration package
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.sergiofraile.BlocKotlin"
                artifactId = "bloc"
                version = project.version.toString()

                pom {
                    name.set("BlocKotlin")
                    description.set("A Kotlin Bloc state-management library for Android, mirroring the API of flutter_bloc and BlocSwift.")
                    url.set("https://github.com/sergiofraile/BlocKotlin")
                    licenses {
                        license {
                            name.set("Apache License 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0")
                        }
                    }
                    developers {
                        developer {
                            id.set("sergiofraile")
                            name.set("Sergio Fraile")
                            url.set("https://github.com/sergiofraile")
                        }
                    }
                    scm {
                        connection.set("scm:git:github.com/sergiofraile/BlocKotlin.git")
                        developerConnection.set("scm:git:ssh://github.com/sergiofraile/BlocKotlin.git")
                        url.set("https://github.com/sergiofraile/BlocKotlin/tree/main")
                    }
                }
            }
        }
    }
}
