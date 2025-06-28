package com.example.agrobot

// Importaciones estándar de Android y Compose
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

// Importaciones de Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database

// Importaciones para Bluetooth
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

// Importaciones para Coroutines y Lifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.StringBuilder

// Importaciones para Almacenamiento Local (SharedPreferences)
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.content.SharedPreferences

// Define el UUID estándar para SPP (Serial Port Profile)
private const val SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

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

    // Constantes para SharedPreferences (Almacenamiento Local Offline)
    private val PREFS_NAME = "AgroBotOfflineData"
    private val KEY_PENDING_READINGS = "pending_readings"

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
            connectToHC06()
        }
        // --- Fin Configuración Bluetooth ---

        // Intenta sincronizar datos pendientes justo después de que la app se inicia
        // y se conecta (o si ya hay conexión a internet).
        // Se lanza en un hilo de background para no bloquear la UI.
        lifecycleScope.launch(Dispatchers.Main) { // Usamos Main para que los Toasts se muestren bien.
            // Pequeño delay si se necesita asegurar que Firebase Auth esté listo
            // o simplemente puedes llamar a esto después de que el usuario se loguea exitosamente si usas el mismo flujo
            if (FirebaseAuth.getInstance().currentUser != null) {
                syncLocalDataToFirebase()
            }
        }


        setContent {
            AgroBotTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContentScreen(
                        onLogoutClick = {
                            auth.signOut()
                            val intent = Intent(this@MainActivity, LoginActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            startActivity(intent)
                        },
                        onSendCommand = { command ->
                            sendBluetoothCommand(command)
                        },
                        onConnectClick = {
                            connectToHC06()
                        },
                        onEvaluateClick = {
                            // Cuando se presiona "Tomar Lectura y Evaluar", pide los datos al Arduino
                            sendBluetoothCommand("GET_DATA\n") // Envía el comando "GET_DATA" al Arduino
                        },
                        gasValue = gasValueState.value,
                        humidityStatus = humidityStatusState.value,
                        plantingStatus = plantingStatusState.value,
                        connectionStatus = connectionStatusState.value
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
                    runOnUiThread { connectionStatusState.value = "Desconectado" }
                    return@launch
                }

                val uuid = UUID.fromString(SPP_UUID)
                bluetoothSocket = hc06Device.createInsecureRfcommSocketToServiceRecord(uuid)
                runOnUiThread { Toast.makeText(this@MainActivity, "Intentando conectar a HC-06...", Toast.LENGTH_SHORT).show() }
                runOnUiThread { connectionStatusState.value = "Conectando..." }

                bluetoothSocket?.connect()
                outputStream = bluetoothSocket?.outputStream
                inputStream = bluetoothSocket?.inputStream

                runOnUiThread { Toast.makeText(this@MainActivity, "¡Conectado a HC-06!", Toast.LENGTH_SHORT).show() }
                runOnUiThread { connectionStatusState.value = "Conectado" }
                // Iniciar la escucha de datos DESPUÉS de una conexión exitosa
                startListeningForBluetoothData()
                // Opcional: Intentar sincronizar datos pendientes al conectar Bluetooth
                syncLocalDataToFirebase()

            } catch (e: IOException) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Error de conexión Bluetooth: ${e.message}", Toast.LENGTH_LONG).show() }
                runOnUiThread { connectionStatusState.value = "Desconectado" }
                closeBluetoothConnection()
            } catch (e: SecurityException) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Error de seguridad Bluetooth: ${e.message}", Toast.LENGTH_LONG).show() }
                runOnUiThread { connectionStatusState.value = "Desconectado" }
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
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                outputStream?.write(command.toByteArray())
                runOnUiThread { Toast.makeText(this@MainActivity, "Comando '$command' enviado.", Toast.LENGTH_SHORT).show() }
            } catch (e: IOException) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Error al enviar comando: ${e.message}", Toast.LENGTH_LONG).show() }
                runOnUiThread { connectionStatusState.value = "Desconectado" }
                closeBluetoothConnection()
            }
        }
    }

    // --- Función para iniciar la escucha de datos del módulo Bluetooth ---
    private fun startListeningForBluetoothData() {
        if (inputStream == null) return

        lifecycleScope.launch(Dispatchers.IO) {
            val buffer = ByteArray(1024)
            val readMessage = StringBuilder()

            while (true) {
                try {
                    val bytes = inputStream!!.read(buffer)
                    val incomingData = String(buffer, 0, bytes)
                    readMessage.append(incomingData)

                    val indexOfNewline = readMessage.indexOf('\n')
                    if (indexOfNewline != -1) {
                        val fullMessage = readMessage.substring(0, indexOfNewline).trim()
                        processReceivedData(fullMessage)
                        readMessage.delete(0, indexOfNewline + 1)
                    }
                } catch (e: IOException) {
                    runOnUiThread { Toast.makeText(this@MainActivity, "Conexión Bluetooth perdida o cerrada.", Toast.LENGTH_LONG).show() }
                    runOnUiThread { connectionStatusState.value = "Desconectado" }
                    closeBluetoothConnection()
                    break
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
            runOnUiThread { connectionStatusState.value = "Desconectado" }
        }
    }

    // --- Se llama cuando la actividad está a punto de ser destruida ---
    override fun onDestroy() {
        super.onDestroy()
        closeBluetoothConnection()
    }

    // --- Función para verificar si hay conexión a Internet ---
    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    // --- Función para guardar lecturas de sensor en Firebase Realtime Database ---
    fun saveSensorReading(gas: Int, humidity: Int) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            runOnUiThread { Toast.makeText(this, "Debe iniciar sesión para guardar datos.", Toast.LENGTH_LONG).show() }
            return
        }

        val newReading = SensorReading(
            gasValue = gas,
            humidityValue = humidity,
            userId = userId
        )

        database.child("users").child(userId).child("readings").push().setValue(newReading)
            .addOnSuccessListener { runOnUiThread { Toast.makeText(this@MainActivity, "Lectura guardada en Firebase!", Toast.LENGTH_SHORT).show() } }
            .addOnFailureListener { e -> runOnUiThread { Toast.makeText(this@MainActivity, "Error al guardar lectura en Firebase: ${e.message}", Toast.LENGTH_LONG).show() } }
    }

    // --- NUEVA Función para guardar datos localmente (SharedPreferences) ---
    private fun saveLocally(gas: Int, humidity: Int) {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentPendingData = sharedPref.getString(KEY_PENDING_READINGS, "")
        val newEntry = "G:$gas,H:$humidity,T:${System.currentTimeMillis()}\n"
        with (sharedPref.edit()) {
            putString(KEY_PENDING_READINGS, currentPendingData + newEntry)
            apply()
        }
        runOnUiThread { Toast.makeText(this, "Datos guardados localmente (sin Internet).", Toast.LENGTH_SHORT).show() }
    }

    // --- NUEVA Función para sincronizar datos guardados localmente con Firebase ---
    private fun syncLocalDataToFirebase() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (!isInternetAvailable()) {
                runOnUiThread { Toast.makeText(this@MainActivity, "No hay Internet para sincronizar datos pendientes.", Toast.LENGTH_SHORT).show() }
                return@launch
            }

            val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val pendingReadingsString = sharedPref.getString(KEY_PENDING_READINGS, "")

            if (pendingReadingsString.isNullOrEmpty()) {
                return@launch
            }

            val readingsToSync = pendingReadingsString.split('\n').filter { it.isNotBlank() }
            if (readingsToSync.isEmpty()) {
                return@launch
            }

            runOnUiThread { Toast.makeText(this@MainActivity, "Sincronizando ${readingsToSync.size} lectura(s) pendiente(s)...", Toast.LENGTH_LONG).show() }

            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Debe iniciar sesión para sincronizar datos.", Toast.LENGTH_LONG).show() }
                return@launch
            }

            var allSyncedSuccessfully = true

            for (entry in readingsToSync) {
                try {
                    val parts = entry.split(",")
                    var gas = 0
                    var humidity = 0
                    var timestamp = System.currentTimeMillis()

                    for (part in parts) {
                        if (part.startsWith("G:")) gas = part.substring(2).toIntOrNull() ?: 0
                        else if (part.startsWith("H:")) humidity = part.substring(2).toIntOrNull() ?: -1
                        else if (part.startsWith("T:")) timestamp = part.substring(2).toLongOrNull() ?: System.currentTimeMillis()
                    }

                    val newReading = SensorReading(
                        timestamp = timestamp,
                        gasValue = gas,
                        humidityValue = humidity,
                        userId = userId
                    )
                    database.child("users").child(userId).child("readings").push().setValue(newReading)
                        .addOnFailureListener { e ->
                            runOnUiThread { Toast.makeText(this@MainActivity, "Error al sincronizar lectura: ${e.message}", Toast.LENGTH_SHORT).show() }
                            allSyncedSuccessfully = false
                        }
                } catch (e: Exception) {
                    runOnUiThread { Toast.makeText(this@MainActivity, "Error al procesar y subir entrada pendiente: ${e.message}", Toast.LENGTH_SHORT).show() }
                    allSyncedSuccessfully = false
                }
            }

            if (allSyncedSuccessfully && readingsToSync.isNotEmpty()) {
                with (sharedPref.edit()) {
                    remove(KEY_PENDING_READINGS)
                    apply()
                }
                runOnUiThread { Toast.makeText(this@MainActivity, "Sincronización de datos pendientes completada.", Toast.LENGTH_SHORT).show() }
            } else if (!allSyncedSuccessfully) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Sincronización con errores. Reintentará en próxima conexión.", Toast.LENGTH_LONG).show() }
            }
        }
    }

    // --- Modifica tu función processReceivedData para usar la lógica offline ---
    private fun processReceivedData(data: String) {
        runOnUiThread {
            try {
                val parts = data.split(",")
                var gasString = ""
                var humidityString = ""

                for (part in parts) {
                    if (part.startsWith("G:")) {
                        gasString = part.substring(2)
                    } else if (part.startsWith("H:")) {
                        humidityString = part.substring(2)
                    }
                }

                val gas = gasString.toIntOrNull() ?: 0
                val humidity = humidityString.toIntOrNull() ?: -1

                gasValueState.value = "Gas: $gas"
                val humidityText = when (humidity) {
                    0 -> "Seca"
                    1 -> "Húmeda"
                    else -> "--"
                }
                humidityStatusState.value = "Humedad: $humidityText"

                evaluateAndDisplayResult(gas, humidity)

                // --- Lógica para guardar en Firebase o localmente ---
                if (isInternetAvailable()) {
                    saveSensorReading(gas, humidity)
                    syncLocalDataToFirebase()
                } else {
                    saveLocally(gas, humidity)
                }

            } catch (e: Exception) {
                Toast.makeText(this, "Error al procesar datos del Arduino: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // --- Función para evaluar la aptitud y guardar datos (mantienes tu lógica original de evaluación) ---
    private fun evaluateAndDisplayResult(gas: Int, humidity: Int) {
        var statusMessage = ""

        // AJUSTA ESTOS VALORES SEGÚN TUS PRUEBAS Y CRITERIOS REALES
        val gasThresholdForGoodAir = 300 // Ejemplo: El gas es "bueno" si la lectura es MAYOR a 300
        val humidityGood = 0             // 0 = seco (apto), 1 = húmedo (no apto)

        if (gas > gasThresholdForGoodAir && humidity == humidityGood) {
            statusMessage = "¡Condiciones Aptas para Plantado!"
        } else {
            statusMessage = "Condiciones No Aptas para Plantado. Revisar."
            if (gas <= gasThresholdForGoodAir) {
                statusMessage += "\n- Nivel de gas demasiado alto."
            }
            if (humidity != humidityGood) {
                statusMessage += "\n- Humedad del suelo no adecuada (requiere ser seco)."
            }
        }

        runOnUiThread {
            plantingStatusState.value = statusMessage
        }
        // La llamada a saveSensorReading o saveLocally se maneja ahora en processReceivedData
    }
}

// ========= Función Composable para el Contenido Principal (UI) =========
@Composable
fun MainContentScreen(
    onLogoutClick: () -> Unit,
    onSendCommand: (String) -> Unit,
    onConnectClick: () -> Unit,
    onEvaluateClick: () -> Unit,
    gasValue: String,
    humidityStatus: String,
    plantingStatus: String,
    connectionStatus: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "AgroBot - Monitoreo y Control",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Estado Bluetooth: $connectionStatus",
            style = MaterialTheme.typography.titleMedium,
            color = when (connectionStatus) {
                "Conectado" -> MaterialTheme.colorScheme.primary
                "Conectando..." -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.error
            },
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onConnectClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Conectar Bluetooth (HC-06)")
        }

        Spacer(modifier = Modifier.height(24.dp))

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

        Text(
            text = "Evaluación para Plantado:",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = plantingStatus,
            style = MaterialTheme.typography.bodyLarge,
            color = if (plantingStatus.contains("Aptas")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onEvaluateClick,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Tomar Lectura y Evaluar")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Control del Dispositivo:",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { onSendCommand("1\n") }) {
                Text("Encender LED")
            }

            Button(onClick = { onSendCommand("0\n") }) {
                Text("Apagar LED")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onLogoutClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Cerrar Sesión")
        }
    }
}

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
            connectionStatus = "Conectado"
        )
    }
}