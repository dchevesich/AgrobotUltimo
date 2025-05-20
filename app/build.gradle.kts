// build.gradle (Module :app) - Archivo dentro de la carpeta 'app'
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose) // Plugin para Jetpack Compose
    // >>> APLICA EL PLUGIN DE GOOGLE SERVICES AQUÍ <<<
    id("com.google.gms.google-services") // Este plugin leerá el google-services.json
}

android {
    namespace = "com.example.agrobot" // Tu namespace/paquete
    compileSdk = 35 // Tu versión de compileSdk (mantén la que tenías o usa 34/35)

    defaultConfig {
        applicationId = "com.example.agrobot" // Tu ID de aplicación
        minSdk = 24 // Tu versión mínima de Sdk
        targetSdk = 35 // Tu versión de targetSdk (mantén la que tenías o usa 34/35)
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        // Mantén las opciones de Java/Kotlin que ya tenías
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true // Habilita Jetpack Compose
    }
}

dependencies {
    // --- Dependencias que ya tenías y que usan libs. ---
    // Estas NO deberían dar error si ya funcionaban antes.
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)


    // --- DEPENDENCIAS DE FIREBASE Authentication ---
    // Usamos la sintaxis directa porque los alias de libs. no fueron reconocidos.

    // Declara la plataforma de Firebase (BoM - Bill of Materials)
    // Usar BoM ayuda a que todas las librerías de Firebase que uses sean compatibles entre sí.
    // SIEMPRE verifica la última versión de BoM en la documentación oficial de Firebase.
    implementation(platform("com.google.firebase:firebase-bom:32.8.1")) // <-- ### VERIFICAR ÚLTIMA VERSIÓN ###

    // Añade la dependencia específica para Firebase Authentication (con extensiones para Kotlin)
    // Al usar BoM arriba, generalmente NO necesitas especificar la versión aquí.
    implementation("com.google.firebase:firebase-auth-ktx")

    // Opcional pero recomendado: Dependencia de Google Play Services para autenticación
    // (útil para Google Sign-In, a veces requerida indirectamente por Firebase Auth)
    // Al usar BoM, no necesitas especificar la versión aquí.
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    implementation("com.google.firebase:firebase-database-ktx")

    // --- Dependencias de Test ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}