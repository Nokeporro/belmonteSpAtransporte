import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// google-services.json queda fuera de Git; sin él la app compila sin Firebase (push llega en HU14).
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
}

// Claves locales (fuera de Git): PLACES_API_KEY va a BuildConfig.
val propiedadesLocales = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}

// URL del backend para release: -Pbelmonte.apiBaseUrl=http://<ip-del-anfitrion>:8080/
val apiBaseUrlRelease = providers.gradleProperty("belmonte.apiBaseUrl")

// Firma de release: rutas y claves en ~/.gradle/gradle.properties, nunca en el repositorio.
val keystoreRelease = providers.gradleProperty("belmonte.keystore.archivo")

android {
    namespace = "cl.belmontespa.myapplication"
    // Las versiones fijadas de AndroidX (core 1.19.1, Compose BOM 2026.09.00…) exigen compilar
    // contra 37; targetSdk sigue en 36 como pide el README.
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "cl.belmontespa.myapplication"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "cl.belmontespa.myapplication.HiltTestRunner"

        buildConfigField("boolean", "MODO_LOCAL", "false")
        buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080/\"")
        buildConfigField(
            "String",
            "PLACES_API_KEY",
            "\"${propiedadesLocales.getProperty("PLACES_API_KEY", "")}\"",
        )
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
        release {
            optimization {
                enable = true
            }
            buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrlRelease.getOrElse("")}\"")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
        // Build demo: la app completa sobre Room, sin backend (Prueba 2).
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
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Compilar release sin la URL del anfitrión es un error, pero solo al pedir release.
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

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    debugImplementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.places)
    implementation(libs.zxing.android.embedded)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.work.testing)
}
