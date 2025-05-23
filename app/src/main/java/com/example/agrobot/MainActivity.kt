package com.example.agrobot // Esta debe ser la ÚNICA declaración de paquete

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.agrobot.ui.theme.AgroBotTheme

// Importaciones de Firebase Authentication
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

// Importaciones para Bluetooth y Coroutines
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.StringBuilder

// Importaciones para Firebase Realtime Database
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database


// Define el UUID estándar para SPP (Serial Port Profile)
private const val SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"


class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference // Instancia de Firebase Realtime Database

    // Variables para Bluetooth
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    // Códigos para solicitudes de permisos y activación de Bluetooth
    private val REQUEST_BLUETOOTH_PERMISSIONS = 100
    private val REQUEST_ENABLE_BT = 1

    // Nombre de tu módulo HC-06 (asegúrate de que sea exacto y que esté emparejado)
    private val HC06_NAME = "HC-06"

    // Data class para la estructura de las lecturas de sensor para Firebase
    data class SensorReading(
        val timestamp: Long = System.currentTimeMillis(),
        val gasValue: Int = 0,
        val humidityValue: Int = 0,
        val deviceId: String = "agrobot_main",
        val userId: String = "" // Para asociar lecturas con el usuario
    )

    // Estados para la UI (Compose) que serán observados por la pantalla
    private val gasValueState = mutableStateOf("--")
    private val humidityStatusState = mutableStateOf("--")
    private val plantingStatusState = mutableStateOf("Pulsa 'Evaluar' para verificar.")
    // Estado para el indicador de conexión Bluetooth
    private val connectionStatusState = mutableStateOf("Desconectado")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa FirebaseAuth
        auth = Firebase.auth
        // Inicializa Firebase Realtime Database
        database = Firebase.database.reference

        // --- Configuración y verificación de Bluetooth ---
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Este dispositivo no soporta Bluetooth.", Toast.LENGTH_LONG).show()
            return
        }

        // Primero verifica y solicita los permisos de Bluetooth
        checkBluetoothPermissions()
        // Luego verifica si Bluetooth está activado, si no, solicita activarlo
        if (!bluetoothAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } else {
            // Si Bluetooth ya está activado y los permisos concedidos, intenta conectar
            // Esta llamada es para que la app intente conectar automáticamente al iniciar
            connectToHC06()
        }
        // --- Fin Configuración Bluetooth ---


        setContent {
            AgroBotTheme { // Aplica el tema de tu aplicación
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Llama a la función Composable que define la UI principal
                    MainContentScreen(
                        onLogoutClick = {
                            // Lógica para cerrar sesión con Firebase
                            auth.signOut()
                            // Navegar de regreso a LoginActivity y borrar la pila de actividades
                            val intent = Intent(this@MainActivity, LoginActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            startActivity(intent)
                        },
                        onSendCommand = { command ->
                            // Llama a la función para enviar el comando Bluetooth
                            sendBluetoothCommand(command)
                        },
                        onConnectClick = {
                            // Este botón iniciará la conexión Bluetooth
                            connectToHC06()
                        },
                        onEvaluateClick = {
                            // Cuando se presiona "Tomar Lectura y Evaluar", pide los datos al Arduino
                            sendBluetoothCommand("GET_DATA\n") // Envía el comando "GET_DATA" al Arduino
                        },
                        // Pasa los estados de los valores de gas y humedad a la Composable
                        gasValue = gasValueState.value,
                        humidityStatus = humidityStatusState.value,
                        plantingStatus = plantingStatusState.value, // Pasa el estado de evaluación
                        connectionStatus = connectionStatusState.value // Pasa el estado de conexión
                    )
                }
            }
        }
    }

    // --- Manejo de resultados de solicitudes de permisos ---
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permisos Bluetooth concedidos.", Toast.LENGTH_SHORT).show()
                // Si los permisos fueron concedidos, intenta conectar
                connectToHC06()
            } else {
                Toast.makeText(this, "Permisos Bluetooth denegados. La app puede no funcionar correctamente.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // --- Manejo del resultado de la solicitud para activar Bluetooth ---
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Bluetooth activado.", Toast.LENGTH_SHORT).show()
                // Si Bluetooth fue activado por el usuario, intenta conectar
                connectToHC06()
            } else {
                Toast.makeText(this, "Bluetooth es necesario para esta app.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // --- Función para verificar y solicitar permisos de Bluetooth ---
    private fun checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Para Android 12 (API 31) y superior
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        android.Manifest.permission.BLUETOOTH_CONNECT,
                        android.Manifest.permission.BLUETOOTH_SCAN,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ),
                    REQUEST_BLUETOOTH_PERMISSIONS
                )
            }
        } else { // Para versiones anteriores
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_BLUETOOTH_PERMISSIONS
                )
            }
        }
    }

    // --- Función para conectar al módulo HC-06 ---
    private fun connectToHC06() {
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            runOnUiThread { Toast.makeText(this, "Bluetooth no disponible o no activado.", Toast.LENGTH_SHORT).show() }
            return
        }
        lifecycleScope.launch(Dispatchers.IO) { // Ejecutar en un hilo de background
            var hc06Device: BluetoothDevice? = null
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ActivityCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    runOnUiThread { Toast.makeText(this@MainActivity, "Permiso BLUETOOTH_CONNECT denegado. No se puede acceder a dispositivos emparejados.", Toast.LENGTH_LONG).show() }
                    return@launch
                }

                // Buscar el dispositivo HC-06 entre los emparejados
                for (device in bluetoothAdapter!!.bondedDevices) {
                    if (device.name == HC06_NAME) {
                        hc06Device = device
                        break
                    }
                }

                if (hc06Device == null) {
                    runOnUiThread { Toast.makeText(this@MainActivity, "Módulo HC-06 no encontrado. Asegúrate de que esté emparejado y encendido.", Toast.LENGTH_LONG).show() }
                    runOnUiThread { connectionStatusState.value = "Desconectado" } // Actualiza el estado de conexión
                    return@launch
                }

                val uuid = UUID.fromString(SPP_UUID)
                bluetoothSocket = hc06Device.createInsecureRfcommSocketToServiceRecord(uuid)
                runOnUiThread { Toast.makeText(this@MainActivity, "Intentando conectar a HC-06...", Toast.LENGTH_SHORT).show() }
                runOnUiThread { connectionStatusState.value = "Conectando..." } // Actualiza el estado de conexión

                bluetoothSocket?.connect() // Establecer la conexión
                outputStream = bluetoothSocket?.outputStream
                inputStream = bluetoothSocket?.inputStream

                runOnUiThread { Toast.makeText(this@MainActivity, "¡Conectado a HC-06!", Toast.LENGTH_SHORT).show() }
                runOnUiThread { connectionStatusState.value = "Conectado" } // Actualiza el estado de conexión a Conectado
                // Iniciar la escucha de datos DESPUÉS de una conexión exitosa
                startListeningForBluetoothData()

            } catch (e: IOException) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Error de conexión Bluetooth: ${e.message}", Toast.LENGTH_LONG).show() }
                runOnUiThread { connectionStatusState.value = "Desconectado" } // Actualiza el estado de conexión a Desconectado
                closeBluetoothConnection()
            } catch (e: SecurityException) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Error de seguridad Bluetooth: ${e.message}", Toast.LENGTH_LONG).show() }
                runOnUiThread { connectionStatusState.value = "Desconectado" } // Actualiza el estado de conexión a Desconectado
                closeBluetoothConnection()
            }
        }
    }

    // --- Función para enviar comandos al módulo Bluetooth ---
    private fun sendBluetoothCommand(command: String) {
        if (outputStream == null) {
            Toast.makeText(this, "Bluetooth no conectado. No se puede enviar el comando.", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch(Dispatchers.IO) { // Ejecutar en un hilo de background
            try {
                outputStream?.write(command.toByteArray())
                runOnUiThread { Toast.makeText(this@MainActivity, "Comando '$command' enviado.", Toast.LENGTH_SHORT).show() }
            } catch (e: IOException) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Error al enviar comando: ${e.message}", Toast.LENGTH_LONG).show() }
                runOnUiThread { connectionStatusState.value = "Desconectado" } // Actualiza el estado si hay error al enviar
                closeBluetoothConnection()
            }
        }
    }

    // --- Función para iniciar la escucha de datos del módulo Bluetooth ---
    // Esta función se activará una vez establecida la conexión
    private fun startListeningForBluetoothData() {
        if (inputStream == null) return

        lifecycleScope.launch(Dispatchers.IO) { // Ejecutar en un hilo de background
            val buffer = ByteArray(1024) // Buffer para los datos entrantes
            val readMessage = StringBuilder() // Para construir el mensaje completo

            while (true) {
                try {
                    val bytes = inputStream!!.read(buffer) // Leer bytes del flujo de entrada
                    val incomingData = String(buffer, 0, bytes) // Convertir bytes a String
                    readMessage.append(incomingData) // Añadir al StringBuilder

                    // Buscar el caracter de nueva línea para indicar el final de un mensaje
                    val indexOfNewline = readMessage.indexOf('\n')
                    if (indexOfNewline != -1) {
                        // Extraer el mensaje completo y limpiarlo
                        val fullMessage = readMessage.substring(0, indexOfNewline).trim()
                        // Procesar el mensaje recibido en el hilo principal de UI
                        processReceivedData(fullMessage)
                        // Eliminar el mensaje procesado del StringBuilder
                        readMessage.delete(0, indexOfNewline + 1)
                    }
                } catch (e: IOException) {
                    runOnUiThread { Toast.makeText(this@MainActivity, "Conexión Bluetooth perdida o cerrada.", Toast.LENGTH_LONG).show() }
                    runOnUiThread { connectionStatusState.value = "Desconectado" } // Actualiza el estado si la conexión se pierde
                    closeBluetoothConnection()
                    break // Salir del bucle de escucha
                }
            }
        }
    }

    // --- Función para cerrar la conexión Bluetooth ---
    private fun closeBluetoothConnection() {
        try {
            outputStream?.close()
            inputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            println("Error al cerrar conexión Bluetooth: ${e.message}")
        } finally {
            outputStream = null
            inputStream = null
            bluetoothSocket = null
            runOnUiThread { connectionStatusState.value = "Desconectado" } // Asegura que el estado se actualice
        }
    }

    // --- Se llama cuando la actividad está a punto de ser destruida ---
    override fun onDestroy() {
        super.onDestroy()
        // Asegurarse de cerrar la conexión Bluetooth para evitar fugas de recursos
        closeBluetoothConnection()
    }


    // --- Función para guardar lecturas de sensor en Firebase Realtime Database ---
    fun saveSensorReading(gas: Int, humidity: Int) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Debe iniciar sesión para guardar datos.", Toast.LENGTH_LONG).show()
            return
        }

        val newReading = SensorReading(
            gasValue = gas,
            humidityValue = humidity,
            userId = userId // Asocia la lectura con el usuario logueado
        )

        // Guarda la lectura bajo 'users/userId/readings/' con un ID único generado por push()
        database.child("users").child(userId).child("readings").push().setValue(newReading)
            .addOnSuccessListener { runOnUiThread { Toast.makeText(this@MainActivity, "Lectura guardada en Firebase!", Toast.LENGTH_SHORT).show() } }
            .addOnFailureListener { e -> runOnUiThread { Toast.makeText(this@MainActivity, "Error al guardar lectura en Firebase: ${e.message}", Toast.LENGTH_LONG).show() } }
    }

    // --- Función para evaluar la aptitud y guardar datos ---
    // Esta función se llamará cuando la app reciba datos del Arduino
    private fun evaluateAndDisplayResult(gas: Int, humidity: Int) {
        var statusMessage = ""
        var shouldSave = false // Variable para decidir si guardar o no

        // AJUSTA ESTOS VALORES SEGÚN TUS PRUEBAS Y CRITERIOS REALES
        // Si tu sensor de gas da VALORES BAJOS para ALTA CONCENTRACIÓN DE GAS (lo más común con MQ-135)
        val gasThresholdForGoodAir = 300 // Ejemplo: El gas es "bueno" si la lectura es MAYOR a 300
        val humidityGood = 0             // 0 = seco (apto), 1 = húmedo (no apto)

        // La condición para ser "apto"
        if (gas > gasThresholdForGoodAir && humidity == humidityGood) {
            statusMessage = "¡Condiciones Aptas para Plantado!"
            shouldSave = true // Si es apto, guardamos en Firebase
        } else {
            statusMessage = "Condiciones No Aptas para Plantado. Revisar."
            shouldSave = false // No guardamos si no es apto (puedes cambiar esto)

            // Añade detalles de por qué no es apto
            if (gas <= gasThresholdForGoodAir) { // Si el gas es bajo o igual al umbral (malo)
                statusMessage += "\n- Nivel de gas demasiado alto."
            }
            if (humidity != humidityGood) { // Si la humedad no es el valor "bueno" (0 = seco)
                statusMessage += "\n- Humedad del suelo no adecuada (requiere ser seco)."
            }
        }

        // Actualiza el estado de la UI para mostrar el resultado de la evaluación
        runOnUiThread {
            plantingStatusState.value = statusMessage
        }

        // Si se decidió guardar, llama a la función de guardado en Firebase
        if (shouldSave) {
            saveSensorReading(gas, humidity)
        }
    }

    // --- Función para procesar los datos recibidos del Arduino ---
    // Ahora, solo actualiza la UI y llama a la evaluación
    private fun processReceivedData(data: String) {
        runOnUiThread { // Asegura que las actualizaciones de UI se hagan en el hilo principal
            try {
                // Asume el formato "G:valorGas,H:valorHumedad"
                val parts = data.split(",")
                var gasString = ""
                var humidityString = ""

                for (part in parts) {
                    if (part.startsWith("G:")) {
                        gasString = part.substring(2) // Eliminar "G:"
                    } else if (part.startsWith("H:")) {
                        humidityString = part.substring(2) // Eliminar "H:"
                    }
                }

                val gas = gasString.toIntOrNull() ?: 0 // Convertir a Int, si falla, 0
                val humidity = humidityString.toIntOrNull() ?: -1 // Convertir a Int, si falla, -1

                // Actualizar los estados de la UI con los valores recibidos
                gasValueState.value = "Gas: $gas"
                // Invertimos la interpretación para la visualización (0=Seca, 1=Húmeda)
                val humidityText = when (humidity) {
                    0 -> "Seca" // Ahora 0 significa Seca en la UI
                    1 -> "Húmeda" // Y 1 significa Húmeda en la UI
                    else -> "--" // Valor por defecto
                }
                humidityStatusState.value = "Humedad: $humidityText"

                // Llama a la función de evaluación y decisión de guardado
                evaluateAndDisplayResult(gas, humidity)

            } catch (e: Exception) {
                Toast.makeText(this, "Error al procesar datos del Arduino: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}


// ========= Función Composable para el Contenido Principal (UI) =========
@Composable
fun MainContentScreen(
    onLogoutClick: () -> Unit, // Función para llamar al hacer clic en Cerrar Sesión
    onSendCommand: (String) -> Unit, // Función para llamar al enviar un comando (LED)
    onConnectClick: () -> Unit, // Función para iniciar la conexión Bluetooth
    onEvaluateClick: () -> Unit, // Función para el botón de tomar lectura y evaluar
    gasValue: String, // Estado actual del valor del gas (observado por Compose)
    humidityStatus: String, // Estado actual del estado de humedad (observado por Compose)
    plantingStatus: String, // Estado actual del resultado de la evaluación de plantado
    connectionStatus: String // Estado actual del indicador de conexión Bluetooth
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Título de la pantalla principal
        Text(
            text = "AgroBot - Monitoreo y Control",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // >>> INDICADOR DE ESTADO DE CONEXIÓN BLUETOOTH <<<
        Text(
            text = "Estado Bluetooth: $connectionStatus",
            style = MaterialTheme.typography.titleMedium,
            color = when (connectionStatus) {
                "Conectado" -> MaterialTheme.colorScheme.primary // Color verde/azul para conectado
                "Conectando..." -> MaterialTheme.colorScheme.tertiary // Color amarillo/naranja para conectando
                else -> MaterialTheme.colorScheme.error // Color rojo para desconectado
            },
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        // >>> FIN INDICADOR <<<

        // Botón para conectar Bluetooth
        Button(
            onClick = onConnectClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Conectar Bluetooth (HC-06)")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // >>> ELEMENTOS DE UI PARA MOSTRAR DATOS DEL ARDUINO Y EVALUACIÓN <<<
        Text(
            text = "Estado de los Sensores:",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = gasValue,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = humidityStatus,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Resultado de la evaluación de plantado
        Text(
            text = "Evaluación para Plantado:",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = plantingStatus, // Mostrar el estado de aptitud aquí
            style = MaterialTheme.typography.bodyLarge,
            color = if (plantingStatus.contains("Aptas")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Botón para tomar lectura y evaluar
        Button(
            onClick = onEvaluateClick, // Llama a la nueva función de evaluación
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Tomar Lectura y Evaluar")
        }

        Spacer(modifier = Modifier.height(32.dp))

        // >>> ELEMENTOS DE UI PARA ENVIAR COMANDOS AL ARDUINO <<<
        Text(
            text = "Control del Dispositivo:",
            style = MaterialTheme.typography.titleMedium // CORRECCIÓN: MaterialE a MaterialTheme
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Botón para encender algo (comando '1' en tu Arduino)
            Button(onClick = { onSendCommand("1\n") }) {
                Text("Encender LED")
            }

            // Botón para apagar algo (comando '0' en tu Arduino)
            Button(onClick = { onSendCommand("0\n") }) {
                Text("Apagar LED")
            }
        }
        // >>> FIN ELEMENTOS DE UI PARA COMANDOS <<<


        // Spacer que empuja el botón de Logout hacia abajo
        Spacer(modifier = Modifier.weight(1f))

        // Botón de Cerrar Sesión
        Button(
            onClick = onLogoutClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Cerrar Sesión")
        }
    }
}

// ========= Vista Previa para el Contenido Principal (Jetpack Compose Preview) =========
@Preview(showBackground = true, name = "Main Content Preview")
@Composable
fun PreviewMainContentScreen() {
    AgroBotTheme {
        MainContentScreen(
            onLogoutClick = { /* Preview action */ },
            onSendCommand = { command -> println("Preview: Send command $command") },
            onConnectClick = { /* Preview action */ },
            onEvaluateClick = { /* Preview action */ },
            gasValue = "Gas: 450",
            humidityStatus = "Humedad: Seca",
            plantingStatus = "¡Apto para plantado!",
            connectionStatus = "Conectado" // Estado de conexión para el preview
        )
    }
}
