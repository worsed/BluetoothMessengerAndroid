package com.btmessenger.android.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btmessenger.android.bluetooth.BluetoothService
import com.btmessenger.android.data.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MessengerViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()
    
    private val _connectedDevices = MutableStateFlow<List<String>>(emptyList())
    val connectedDevices: StateFlow<List<String>> = _connectedDevices.asStateFlow()
    
    private val _nearbyDevices = MutableStateFlow<List<String>>(emptyList())
    val nearbyDevices: StateFlow<List<String>> = _nearbyDevices.asStateFlow()
    
    private val _statusText = MutableStateFlow("Инициализация...")
    val statusText: StateFlow<String> = _statusText.asStateFlow()
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private var bluetoothService: BluetoothService? = null
    
    fun initialize(context: Context) {
        bluetoothService = BluetoothService(context).apply {
            onMessageReceived = { text, senderName ->
                viewModelScope.launch {
                    val message = Message(
                        text = text,
                        isOutgoing = false,
                        senderName = senderName
                    )
                    _messages.value = _messages.value + message
                }
            }
            
            onStatusChanged = { status ->
                viewModelScope.launch {
                    _statusText.value = status
                }
            }
            
            onConnectionChanged = { connected ->
                viewModelScope.launch {
                    _isConnected.value = connected
                }
            }
            
            onDevicesChanged = { devices ->
                viewModelScope.launch {
                    _connectedDevices.value = devices
                }
            }
            
            startServer()
        }
    }
    
    fun sendMessage(text: String) {
        bluetoothService?.sendMessage(text)
        
        val message = Message(
            text = text,
            isOutgoing = true
        )
        _messages.value = _messages.value + message
    }
    
    fun startDiscovery() {
        bluetoothService?.startDiscovery { devices ->
            viewModelScope.launch {
                _nearbyDevices.value = devices
            }
        }
    }
    
    fun connectToDevice(deviceName: String) {
        bluetoothService?.connectToDevice(deviceName)
    }
    
    override fun onCleared() {
        super.onCleared()
        bluetoothService?.cleanup()
    }
}
