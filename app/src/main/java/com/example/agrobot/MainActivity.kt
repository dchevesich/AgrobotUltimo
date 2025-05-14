package com.example.agrobot // <-- !!! ASEGÚRATE DE QUE ESTE SEA EL NOMBRE EXACTO DE TU PAQUETE !!!

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.* // Importaciones para remember, mutableStateOf, etc.
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign // Importación para TextAlign
import androidx.compose.ui.tooling.preview.Preview // Importación para @Preview
import androidx.compose.ui.unit.dp
import com.example.agrobot.ui.theme.AgroBotTheme // <-- ASEGÚRATE DE QUE ESTE SEA EL NOMBRE EXACTO DE TU TEMA

// Importaciones de Firebase Authentication
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

// Importaciones para Bluetooth (Las usaremos mañana para la lógica, pero las declaramos aquí)
// Necesitarás añadir las dependencias de Bluetooth en Gradle para que esto funcione mañana.
// import android.bluetooth.BluetoothAdapter
// import android.bluetooth.BluetoothManager
// import android.bluetooth.BluetoothDevice
// import android.bluetooth.BluetoothSocket
// import java.io.IOException
// import java.io.InputStream
// import java.io.OutputStream
// import java.util.*
// import kotlinx.coroutines.* // Si usas Coroutines para Bluetooth


// =================================================================
// !!! ESTE ES EL CONTENIDO COMPLETO PARA EL ARCHIVO MainActivity.kt !!!
// !!! COPIA TODO ESTO Y PÉGALO PARA REEMPLAZAR EL CONTENIDO ACTUAL !!!
// =================================================================

// ========= Clase MainActivity =========
// Esta actividad muestra el contenido principal e interactuará con el dispositivo IoT.
class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth // Instancia de Firebase Auth

    // Aquí podrías declarar variables para el Bluetooth si las manejas directamente en la Activity
    // private var bluetoothAdapter: BluetoothAdapter? = null
    // private var bluetoothDevice: BluetoothDevice? = null
    // private var bluetoothSocket: BluetoothSocket? = null
    // private var outputStream: OutputStream? = null
    // private var inputStream: InputStream? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth // Inicializa FirebaseAuth

        // Aquí podrías añadir la inicialización y verificación del Bluetooth
        // val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        // bluetoothAdapter = bluetoothManager.adapter
        // if (bluetoothAdapter == null) {
        //     // Dispositivo no soporta Bluetooth
        //     Toast.makeText(this, "Este dispositivo no soporta Bluetooth", Toast.LENGTH_LONG).show()
        //     // Considerar deshabilitar funcionalidades Bluetooth o cerrar la app
        // } else if (!bluetoothAdapter!!.isEnabled) {
        //     // Bluetooth no está activado, pedir al usuario que lo active
        //     val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        //     startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT) // Define REQUEST_ENABLE_BT = un int
        // }


        setContent {
            AgroBotTheme { // Aplica el tema
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Llama a la función Composable que define la UI principal
                    MainContentScreen(
                        onLogoutClick = {
                            // --- LÓGICA PARA CERRAR SESIÓN CON FIREBASE ---
                            auth.signOut() // Cierra la sesión
                            // Navegar de regreso a LoginActivity y borrar la pila de actividades
                            val intent = Intent(this@MainActivity, LoginActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            startActivity(intent)
                            // --- FIN LÓGICA CERRAR SESIÓN ---
                        },
                        // Aquí pasarías las funciones para enviar comandos si se manejan en la Activity
                        onSendCommand = { command ->
                            // Lógica para enviar comando Bluetooth (la implementaremos mañana)
                            // sendBluetoothCommand(command)
                            println("Comando enviado conceptualmente: $command") // Placeholder
                        }
                        // Aquí pasarías el estado actual de los datos recibidos
                        // gasValue = currentGasValueState.value,
                        // humidityStatus = currentHumidityStatusState.value,
                        // statusMessage = currentStatusMessageState.value
                    )
                }
            }
        }
    }

    // Puedes añadir el onActivityResult para manejar la respuesta de pedir activar Bluetooth
    // override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    //     super.onActivityResult(requestCode, resultCode, data)
    //     if (requestCode == REQUEST_ENABLE_BT) {
    //         if (resultCode == RESULT_OK) {
    //             // Bluetooth activado por el usuario
    //             Toast.makeText(this, "Bluetooth activado.", Toast.LENGTH_SHORT).show()
    //             // Aquí podrías iniciar la búsqueda o conexión
    //         } else {
    //             // Usuario denegó activar Bluetooth
    //             Toast.makeText(this, "Bluetooth necesario para esta app.", Toast.LENGTH_LONG).show()
    //             // Considerar cerrar la app o deshabilitar funcionalidades
    //         }
    //     }
    // }

    // Define esta constante si usas onActivityResult
    // companion object {
    //     private const val REQUEST_ENABLE_BT = 1
    // }

    // Métodos para enviar/recibir datos Bluetooth (los implementaremos mañana)
    // private fun sendBluetoothCommand(command: String) { /* ... */ }
    // private fun startReceivingBluetoothData() { /* ... */ }
    // private fun stopReceivingBluetoothData() { /* ... */ }


    // override fun onDestroy() {
    //     super.onDestroy()
    //     // Asegurarse de cerrar la conexión Bluetooth al destruir la actividad
    //     // stopReceivingBluetoothData()
    //     // bluetoothSocket?.close()
    // }
}

// ========= Función Composable para el Contenido Principal (UI) =========
// Esta función define la interfaz de usuario de la pantalla principal.
@Composable
fun MainContentScreen(
    onLogoutClick: () -> Unit, // Función para llamar al hacer clic en Cerrar Sesión
    onSendCommand: (String) -> Unit // Función para llamar al enviar un comando
    // Puedes añadir parámetros de estado aquí para los datos recibidos
    // gasValue: Int,
    // humidityStatus: String,
    // statusMessage: String?
) {
    // Puedes usar estados aquí si quieres gestionar la UI localmente en la Composable
    // var displayedGasValue by remember { mutableStateOf(0) }
    // var displayedHumidityStatus by remember { mutableStateOf("Desconocido") }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top // Alinea arriba
    ) {
        // Título de la pantalla principal
        Text(
            text = "AgroBot - Monitoreo y Control",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // >>> ELEMENTOS DE UI PARA MOSTRAR DATOS DEL ARDUINO <<<
        Text(
            text = "Estado de los Sensores:",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Placeholder para el valor del Gas
        // Aquí mostrarías el valor real recibido del Arduino
        Text(
            text = "Valor Gas: --", // Usa el estado 'gasValue' aquí: "Valor Gas: $gasValue"
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(4.dp))

        // Placeholder para el estado de Humedad
        // Aquí mostrarías el estado real recibido del Arduino
        Text(
            text = "Humedad Suelo: --", // Usa el estado 'humidityStatus' aquí: "Humedad Suelo: $humidityStatus"
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Placeholder para mensajes de estado
        Text(
            text = "Mensaje Estado: --", // Usa el estado 'statusMessage' aquí: "Mensaje Estado: ${statusMessage ?: "--"}"
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant // O un color diferente para mensajes
        )

        // >>> FIN ELEMENTOS DE UI PARA DATOS <<<

        Spacer(modifier = Modifier.height(32.dp))

        // >>> ELEMENTOS DE UI PARA ENVIAR COMANDOS AL ARDUINO <<<
        Text(
            text = "Control del Dispositivo:",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly // Distribuye los botones horizontalmente
        ) {
            // Botón para encender algo (comando '1' en tu Arduino)
            Button(onClick = { onSendCommand("1") }) { // Llama a la lambda con el comando
                Text("Encender LED") // O el texto que represente la acción
            }

            // Botón para apagar algo (comando '0' en tu Arduino)
            Button(onClick = { onSendCommand("0") }) { // Llama a la lambda con el comando
                Text("Apagar LED") // O el texto que represente la acción
            }
        }
        // >>> FIN ELEMENTOS DE UI PARA COMANDOS <<<


        // Spacer que empuja el botón de Logout hacia abajo
        Spacer(modifier = Modifier.weight(1f))

        // Botón de Cerrar Sesión (lo colocamos al final de la columna)
        Button(
            onClick = onLogoutClick, // Llama a la lambda pasada desde la Activity
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) // Usa un color distintivo para logout
        ) {
            Text("Cerrar Sesión")
        }
    }
}

// ========= Vista Previa para el Contenido Principal =========
@Preview(showBackground = true, name = "Main Content Preview")
@Composable
fun PreviewMainContentScreen() {
    AgroBotTheme {
        // Para la vista previa, pasamos lambdas vacías ya que la lógica real no se ejecuta aquí.
        MainContentScreen(
            onLogoutClick = { /* Preview action */ },
            onSendCommand = { command -> println("Preview: Send command $command") } // Puedes poner un print para ver en la consola de preview
        )
    }
}