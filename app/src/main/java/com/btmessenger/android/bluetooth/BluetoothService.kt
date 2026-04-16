package com.btmessenger.android.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class BluetoothService(private val context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    
    private val serviceUUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
    private val serviceName = "BluetoothMessenger"
    
    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    
    private val connectedDevices = mutableListOf<String>()
    
    var onMessageReceived: ((String, String) -> Unit)? = null
    var onStatusChanged: ((String) -> Unit)? = null
    var onConnectionChanged: ((Boolean) -> Unit)? = null
    var onDevicesChanged: ((List<String>) -> Unit)? = null
    
    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    if (checkBluetoothPermission()) {
                        val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        device?.let {
                            discoveredDevices.add(it.name ?: it.address)
                        }
                    }
                }
            }
        }
    }
    
    private val discoveredDevices = mutableListOf<String>()
    
    fun startServer() {
        if (!checkBluetoothPermission()) {
            onStatusChanged?.invoke("Нет разрешений Bluetooth")
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(serviceName, serviceUUID)
                onStatusChanged?.invoke("Сервер запущен, ожидание подключений...")
                
                while (true) {
                    try {
                        val socket = serverSocket?.accept()
                        socket?.let {
                            handleConnection(it)
                        }
                    } catch (e: IOException) {
                        break
                    }
                }
            } catch (e: Exception) {
                onStatusChanged?.invoke("Ошибка сервера: ${e.message}")
            }
        }
    }
    
    fun connectToDevice(deviceName: String) {
        if (!checkBluetoothPermission()) return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val device = bluetoothAdapter?.bondedDevices?.find { it.name == deviceName }
                if (device == null) {
                    onStatusChanged?.invoke("Устройство не найдено")
                    return@launch
                }
                
                onStatusChanged?.invoke("Подключение к $deviceName...")
                val socket = device.createRfcommSocketToServiceRecord(serviceUUID)
                socket.connect()
                
                handleConnection(socket)
            } catch (e: Exception) {
                onStatusChanged?.invoke("Ошибка подключения: ${e.message}")
            }
        }
    }
    
    private fun handleConnection(socket: BluetoothSocket) {
        clientSocket = socket
        outputStream = socket.outputStream
        inputStream = socket.inputStream
        
        val deviceName = if (checkBluetoothPermission()) {
            socket.remoteDevice.name ?: socket.remoteDevice.address
        } else {
            "Unknown"
        }
        
        connectedDevices.add(deviceName)
        onConnectionChanged?.invoke(true)
        onDevicesChanged?.invoke(connectedDevices.toList())
        onStatusChanged?.invoke("Подключено к $deviceName")
        
        // Слушаем входящие сообщения
        CoroutineScope(Dispatchers.IO).launch {
            val buffer = ByteArray(1024)
            while (true) {
                try {
                    val bytes = inputStream?.read(buffer) ?: break
                    if (bytes > 0) {
                        val message = String(buffer, 0, bytes)
                        onMessageReceived?.invoke(message, deviceName)
                    }
                } catch (e: IOException) {
                    onStatusChanged?.invoke("Соединение разорвано")
                    onConnectionChanged?.invoke(false)
                    break
                }
            }
        }
    }
    
    fun sendMessage(text: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                outputStream?.write(text.toByteArray())
                outputStream?.flush()
            } catch (e: IOException) {
                onStatusChanged?.invoke("Ошибка отправки: ${e.message}")
            }
        }
    }
    
    fun startDiscovery(onDevicesFound: (List<String>) -> Unit) {
        if (!checkBluetoothPermission()) return
        
        discoveredDevices.clear()
        
        // Регистрируем receiver
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(discoveryReceiver, filter)
        
        // Запускаем поиск
        bluetoothAdapter?.startDiscovery()
        
        onStatusChanged?.invoke("Поиск устройств...")
        
        // Через 12 секунд останавливаем и возвращаем результаты
        CoroutineScope(Dispatchers.Main).launch {
            kotlinx.coroutines.delay(12000)
            bluetoothAdapter?.cancelDiscovery()
            context.unregisterReceiver(discoveryReceiver)
            onDevicesFound(discoveredDevices.toList())
            onStatusChanged?.invoke("Найдено устройств: ${discoveredDevices.size}")
        }
    }
    
    fun cleanup() {
        try {
            clientSocket?.close()
            serverSocket?.close()
        } catch (e: IOException) {
            // Ignore
        }
    }
    
    private fun checkBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
