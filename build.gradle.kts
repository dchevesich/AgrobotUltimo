// Top-level build file where you can add configuration options common to all sub-projects/modules.
// build.gradle (Project: AgroBot) - Archivo de nivel de proyecto en la raíz

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
<<<<<<< HEAD
=======
    // >>> ¡FALTA ESTA LÍNEA! Añádela aquí: <<<
>>>>>>> 832033815585fcd98a06696f8373f47427069da9
    id("com.google.gms.google-services") version "4.4.1" apply false // ### Verifica la última versión ###
}