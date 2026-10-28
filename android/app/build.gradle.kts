plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val apiBaseUrlRelease = providers.gradleProperty("belmonte.apiBaseUrl")

val keystoreRelease = providers.gradleProperty("belmonte.keystore.archivo")

android {
    namespace = "cl.belmontespa.myapplication"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "cl.belmontespa.myapplication"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("boolean", "MODO_LOCAL", "false")
        buildConfigField("boolean", "CON_SEMILLA", "false")
        buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080/\"")
    }

    signingConfigs {
        if (keystoreRelease.isPresent) {
            create("release") {
                storeFile = file(keystoreRelease.get())
                storePassword = providers.gradleProperty("belmonte.keystore.password").get()
                keyAlias = providers.gradleProperty("belmonte.key.alias").get()
                keyPassword = providers.gradleProperty("belmonte.key.password").get()
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "CON_SEMILLA", "true")
        }
        release {
            optimization {
                enable = true
            }
            buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrlRelease.getOrElse("")}\"")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
        create("demo") {
            initWith(getByName("debug"))
            buildConfigField("boolean", "MODO_LOCAL", "true")
            matchingFallbacks += "debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    sourceSets.getByName("androidTest").assets.directories.add("schemas")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    val url = apiBaseUrlRelease
    doFirst {
        check(url.isPresent) {
            "Falta -Pbelmonte.apiBaseUrl=http://<ip-del-anfitrion>:8080/ para compilar release."
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)


    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.play.services.code.scanner)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
