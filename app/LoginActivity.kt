package com.example.agrobot // <-- ASEGÚRATE de que este sea el nombre exacto de tu paquete

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge // Mantenlo si usas la configuración de borde a borde
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.agrobot.ui.theme.AgroBotTheme // <-- ASEGÚRATE de que este sea el nombre exacto de tu tema

// =================================================================
// ESTE ES EL CONTENIDO COMPLETO PARA EL ARCHIVO LoginActivity.kt
// =================================================================

// ========= Clase LoginActivity =========
// Esta clase es la Actividad de Android que se encargará de mostrar la pantalla de login.
class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge() // Descomenta si usas esta configuración

        setContent {
            AgroBotTheme { // Aplica el tema visual de tu aplicación
                // Llama a la función Composable que dibuja la pantalla de login.
                // Le pasamos una función (lambda) que se ejecutará cuando el login sea exitoso.
                LoginScreen(
                    onLoginSuccess = { user, pass ->
                        // --- ACCIÓN AL INICIAR SESIÓN CON ÉXITO ---
                        // Crea un Intent para navegar a la MainActivity
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        // Inicia la MainActivity
                        startActivity(intent)
                        // Cierra la LoginActivity para que el usuario no pueda volver atrás con el botón físico o gestual
                        finish()
                        // Muestra un mensaje de éxito temporal
                        Toast.makeText(this@LoginActivity, "Inicio de sesión exitoso!", Toast.LENGTH_SHORT).show()
                        // --- FIN ACCIÓN AL INICIAR SESIÓN CON ÉXITO ---
                    }
                )
            }
        }
    }
}

// ========= Función Composable para la Pantalla de Login =========
// Esta función define la interfaz de usuario de la pantalla de inicio de sesión.
// Es una buena práctica ponerla en el mismo archivo que la actividad que la usa si es específica de ella,
// o en un archivo de UI separado si es reutilizable.
@Composable
fun LoginScreen(
    onLoginSuccess: (String, String) -> Unit // Función (lambda) que se llama desde el botón al hacer login exitoso
) {
    // Estados en Compose para mantener el texto de los campos y el mensaje de error.
    // 'remember' ayuda a que el estado persista a través de las redibujadas (recomposiciones).
    // 'mutableStateOf' crea un estado que, cuando su valor cambia, hace que Compose redibuje la parte afectada de la UI.
    var usuario by remember { mutableStateOf("") } // Estado para el texto del campo de usuario
    var contrasena by remember { mutableStateOf("") } // Estado para el texto del campo de contraseña
    var mensajeError by remember { mutableStateOf<String?>(null) } // Estado para el mensaje de error (inicialmente nulo, no se muestra)

    // Layout principal de la pantalla: una columna centrada.
    Column(
        modifier = Modifier
            .fillMaxSize() // Ocupa todo el ancho y alto de la pantalla disponible
            .padding(16.dp), // Añade un espacio (margen interno) de 16dp alrededor del contenido
        horizontalAlignment = Alignment.CenterHorizontally, // Centra los elementos hijos horizontalmente
        verticalArrangement = Arrangement.Center // Centra los elementos hijos verticalmente (los distribuye uniformemente si hay más espacio del necesario)
    ) {
        // Título de la pantalla de login
        Text(
            text = "Inicio de Sesión",
            style = MaterialTheme.typography.headlineMedium // Aplica un estilo de texto predefinido por el tema
        )

        // Espacio vertical
        Spacer(modifier = Modifier.height(24.dp))

        // Campo de texto para Usuario o Email
        OutlinedTextField( // Un campo de texto con un borde delineado
            value = usuario, // Enlaza el valor del campo con el estado 'usuario'
            onValueChange = { newValue -> usuario = newValue }, // Cuando el usuario escribe, actualiza el estado 'usuario'
            label = { Text("Usuario o Email") }, // Texto flotante o etiqueta
            modifier = Modifier.fillMaxWidth() // Hace que el campo ocupe todo el ancho disponible
        )

        // Espacio vertical
        Spacer(modifier = Modifier.height(8.dp))

        // Campo de texto para Contraseña
        OutlinedTextField(
            value = contrasena,
            onValueChange = { newValue -> contrasena = newValue },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(), // Transforma el texto a puntos para ocultar la contraseña
            modifier = Modifier.fillMaxWidth()
        )

        // Espacio vertical
        Spacer(modifier = Modifier.height(16.dp))

        // Botón de Iniciar Sesión
        Button(
            onClick = {
                // --- Lógica de Autenticación (Ejemplo simple "quemado") ---
                // Esta lógica se ejecuta cuando se toca el botón.
                if (usuario.isEmpty() || contrasena.isEmpty()) {
                    // Si los campos están vacíos, muestra un error.
                    mensajeError = "Por favor, ingresa usuario y contraseña."
                } else if (usuario == "miUsuario" && contrasena == "miContrasena123") {
                    // Si las credenciales coinciden (en este ejemplo simple), indica éxito.
                    mensajeError = null // Limpia cualquier mensaje de error anterior
                    onLoginSuccess(usuario, contrasena) // Llama a la función lambda que se pasó al LoginScreen (la que inicia MainActivity)
                } else {
                    // Si las credenciales no coinciden, muestra un error.
                    mensajeError = "Usuario o contraseña incorrectos."
                }
                // --- Fin Lógica de Autenticación ---
            },
            modifier = Modifier.fillMaxWidth() // Hace que el botón ocupe todo el ancho disponible
        ) {
            Text("Iniciar Sesión") // Texto dentro del botón
        }

        // Mostrar mensaje de error si el estado 'mensajeError' no es nulo
        mensajeError?.let { // 'let' es una función de alcance que solo se ejecuta si 'mensajeError' no es nulo
            Text(
                text = it, // 'it' es el valor de 'mensajeError' dentro del bloque 'let'
                color = MaterialTheme.colorScheme.error, // Usa el color de error definido en tu tema Material3
                modifier = Modifier.padding(top = 8.dp) // Añade espacio encima del mensaje
            )
        }
    }
}

// ========= Vista Previa para LoginScreen =========
// Esta función Composable te permite ver el diseño de LoginScreen en la ventana Preview de Android Studio.
@Preview(showBackground = true, name = "Login Screen Preview")
@Composable
fun PreviewLoginScreen() {
    AgroBotTheme { // Envuelve la vista previa con tu tema para ver cómo se aplica
        LoginScreen(onLoginSuccess = { user, pass ->
            // Esta es una función de marcador de posición solo para el Preview.
            // En la aplicación real, onLoginSuccess iniciaría la MainActivity.
        })
    }
}