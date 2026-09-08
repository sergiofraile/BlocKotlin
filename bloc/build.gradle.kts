plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.vanniktech.publish)
}

kotlin {
    // Warn on any public declaration that lacks an explicit visibility modifier.
    // Upgrade to explicitApi() once all warnings are resolved.
    explicitApiWarning()
}

dokka {
    moduleName.set("BlocKotlin")

    dokkaSourceSets.configureEach {
        includes.from("dokka/MODULE.md")
    }

    pluginsConfiguration.html {
        // A custom logo-icon.svg also becomes the site favicon (Dokka serves it
        // from images/logo-icon.svg and references it in every page's <head>).
        customAssets.from(
            "dokka/logo-icon.svg",
            rootProject.file("assets/banner.png"),
        )
        footerMessage.set("© 2026 Sergio Fraile — Apache 2.0")
    }
}

android {
    namespace = "dev.bloc"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
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
        version    = "1.1.2",
    )

    pom {
        name.set("BlocKotlin")
        description.set("A Kotlin Bloc state-management library for Android, mirroring the API of flutter_bloc and BlocSwift.")
        // Project homepage — Maven Central and mvnrepository render this as the
        // "Project URL" link and derive the artifact icon from its favicon.
        url.set("https://blockotlin.thewalkingpuffin.com")
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
