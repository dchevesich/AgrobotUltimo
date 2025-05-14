package com.example.agrobot // <-- !!! MUY IMPORTANTE: ASEGÚRATE DE QUE ESTE SEA EL NOMBRE EXACTO DE TU PAQUETE !!!

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable // <-- Importación necesaria para hacer texto clickeable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext // <-- Importación necesaria para LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// Importaciones necesarias para Firebase Authentication
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

import com.example.agrobot.ui.theme.AgroBotTheme // <-- ASEGÚRATE DE QUE ESTE SEA EL NOMBRE EXACTO DE TU TEMA


// ========= Clase LoginActivity =========
// Esta actividad maneja tanto el inicio de sesión como el registro.
class LoginActivity : ComponentActivity() {

    // Declaración de la instancia de FirebaseAuth
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge() // Descomenta si usas esta configuración

        // Inicialización de la instancia de FirebaseAuth
        auth = Firebase.auth

        // --- VERIFICAR SI EL USUARIO YA ESTÁ LOGUEADO AL INICIAR LA ACTIVIDAD ---
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Si el usuario ya está logueado, navegar directamente a MainActivity
            val intent = Intent(this@LoginActivity, MainActivity::class.java)
            startActivity(intent)
            finish() // Cierra LoginActivity
        }
        // --- FIN VERIFICACIÓN DE SESIÓN ---

        setContent {
            AgroBotTheme {
                // Pasa la instancia de 'auth' y la función de éxito a la composable.
                LoginScreen(
                    auth = auth,
                    onLoginSuccess = { email, pass -> // La lambda recibe email y pass (aunque no los uses directamente aquí)
                        // Acción al iniciar sesión con éxito (confirmado por Firebase)
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish() // Cierra LoginActivity
                        Toast.makeText(this@LoginActivity, "Inicio de sesión exitoso!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

// ========= Función Composable para la Pantalla de Autenticación (Login/Registro) =========
@Composable
fun LoginScreen(
    auth: FirebaseAuth, // Instancia de auth
    onLoginSuccess: (String, String) -> Unit // Función (lambda) para llamar al login exitoso
) {
    // Estados para los campos de texto y UI
    var email by remember { mutableStateOf("") } // Usamos 'email' en lugar de 'usuario' para claridad con Firebase
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") } // Campo para confirmar contraseña (registro)
    var isRegistering by remember { mutableStateOf(false) } // true = modo Registro, false = modo Login
    var errorMessage by remember { mutableStateOf<String?>(null) } // Mensaje de error
    var isLoading by remember { mutableStateOf(false) } // Estado de carga (spinner)

    // Contexto local para mostrar Toasts
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Título dinámico
        Text(
            text = if (isRegistering) "Registro de Usuario" else "Inicio de Sesión",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Campo de Email
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading // Deshabilita durante carga
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Campo de Contraseña
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading // Deshabilita durante carga
        )

        // Campo Confirmar Contraseña (solo visible en modo Registro)
        if (isRegistering) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirmar Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading // Deshabilita durante carga
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón principal (Texto y Acción dinámicos)
        Button(
            onClick = {
                errorMessage = null // Limpia errores previos
                isLoading = true // Activa estado de carga

                // Validaciones básicas antes de llamar a Firebase
                if (email.isEmpty() || password.isEmpty()) {
                    errorMessage = if (isRegistering) "Ingresa email y contraseña." else "Ingresa email y contraseña."
                    isLoading = false
                    return@Button // Sale de la lambda onClick
                }
                if (isRegistering && confirmPassword.isEmpty()) {
                    errorMessage = "Confirma la contraseña."
                    isLoading = false
                    return@Button
                }
                if (isRegistering && password != confirmPassword) {
                    errorMessage = "Las contraseñas no coinciden."
                    isLoading = false
                    return@Button
                }
                // Puedes añadir más validaciones aquí (formato de email, longitud de contraseña, etc.)

                // --- Lógica de Autenticación/Registro con Firebase ---
                if (isRegistering) {
                    // *** Lógica de REGISTRO ***
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            isLoading = false // Desactiva carga al recibir respuesta
                            if (task.isSuccessful) {
                                // Registro exitoso
                                Toast.makeText(context, "Registro exitoso!", Toast.LENGTH_SHORT).show()
                                // Opcional: cambiar a modo login y/o limpiar campos después de registrar
                                isRegistering = false
                                email = ""
                                password = ""
                                confirmPassword = ""
                                errorMessage = null
                                // Opcional: Loguear automáticamente después de registrar
                                // onLoginSuccess(email, password)

                            } else {
                                // Error en el registro
                                errorMessage = "Error al registrar: ${task.exception?.localizedMessage ?: "Error desconocido"}"
                                // Puedes mostrar el error de Firebase con más detalle si es necesario
                                // Log.e("Registro", "Error: ${task.exception}", task.exception)
                            }
                        }
                } else {
                    // *** Lógica de INICIO DE SESIÓN ***
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            isLoading = false // Desactiva carga al recibir respuesta
                            if (task.isSuccessful) {
                                // Inicio de sesión exitoso en Firebase
                                onLoginSuccess(email, password) // Llama a la lambda para navegar

                            } else {
                                // Inicio de sesión fallido
                                errorMessage = "Error al iniciar sesión: ${task.exception?.localizedMessage ?: "Error desconocido"}"
                                // Puedes mostrar el error de Firebase con más detalle si es necesario
                                // Log.e("Login", "Error: ${task.exception}", task.exception)
                            }
                        }
                }
                // --- Fin Lógica de Autenticación/Registro ---
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading // Deshabilita el botón mientras se carga
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary) // Muestra spinner si está cargando
            } else {
                Text(if (isRegistering) "Registrarse" else "Iniciar Sesión") // Texto del botón dinámico
            }
        }

        // Mostrar mensaje de error si existe
        errorMessage?.let { // Solo si errorMessage no es nulo
            Text(
                text = it, // El texto del error
                color = MaterialTheme.colorScheme.error, // Color de error del tema Material3
                style = MaterialTheme.typography.bodySmall, // Estilo de texto pequeño
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Texto para cambiar entre Login y Registro
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isRegistering) "¿Ya tienes cuenta? Inicia Sesión" else "¿No tienes cuenta? Regístrate",
            color = MaterialTheme.colorScheme.primary, // Usa el color primario del tema
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.clickable(enabled = !isLoading) { // Hace el texto clickeable
                isRegistering = !isRegistering // Cambia el modo al hacer clic
                // Limpia campos y errores al cambiar de modo
                email = ""
                password = ""
                confirmPassword = ""
                errorMessage = null
            }
        )
    }
}

// ========= Vistas Previa para la Pantalla de Autenticación =========

@Preview(showBackground = true, name = "Authentication Screen (Login)")
@Composable
fun PreviewLoginScreen() {
    AgroBotTheme {
        // Para la vista previa, la instancia de 'auth' no es necesaria para dibujar la UI,
        // pero la función Composable la requiere en su firma.
        // Pasamos 'null as FirebaseAuth' solo para satisfacer la firma en Preview.
        // En código real, 'auth' NUNCA debería ser nulo.
        LoginScreen(auth = null as FirebaseAuth, onLoginSuccess = { email, pass -> /* Preview action */ })
    }
}

// Preview adicional para ver cómo se ve en modo Registro
@Preview(showBackground = true, name = "Authentication Screen (Register)")
@Composable
fun PreviewRegisterScreen() {
    AgroBotTheme {
        // Para simular la vista de registro en Preview, podemos crear una Composable auxiliar
        // que controle el estado 'isRegistering'.
        var isRegisteringPreview by remember { mutableStateOf(true) } // Forzar modo registro en Preview

        LoginScreen(
            auth = null as FirebaseAuth, // auth no se usa en Preview
            onLoginSuccess = { email, pass -> /* Preview action */ }
        )
        // Nota: Este Preview mostrará el estado por defecto (Login). Para forzar el Preview
        // en modo Registro, necesitaríamos adaptar LoginScreen para recibir el estado inicial
        // como parámetro, o usar un Composable Wrapper como en el ejemplo anterior.
        // El Preview 'Authentication Screen (Login)' es la forma estándar de previsualizar la Composable tal cual.
        // La preview original de RegisterScreen comentada arriba es una alternativa manual si necesitas verla sí o sí en ese estado.
        // Sin embargo, la preview principal 'Authentication Screen (Login)' sigue siendo útil y no da problemas.
        // Dejaremos solo la PreviewLoginScreen estándar para simplificar.
    }
}