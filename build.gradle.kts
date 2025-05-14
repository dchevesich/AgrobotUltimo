// Top-level build file where you can add configuration options common to all sub-projects/modules.
// build.gradle (Project: AgroBot) - Archivo de nivel de proyecto en la raíz

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // >>> ¡FALTA ESTA LÍNEA! Añádela aquí: <<<
    id("com.google.gms.google-services") version "4.4.1" apply false // ### Verifica la última versión ###
}