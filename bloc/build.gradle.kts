plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.vanniktech.publish)
}

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

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates(
        groupId    = "io.github.sergiofraile",
        artifactId = "bloc",
        version    = "1.0.0",
    )

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
            url.set("https://github.com/sergiofraile/BlocKotlin")
            connection.set("scm:git:git://github.com/sergiofraile/BlocKotlin.git")
            developerConnection.set("scm:git:ssh://git@github.com/sergiofraile/BlocKotlin.git")
        }
    }
}
