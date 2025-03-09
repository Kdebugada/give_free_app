package com.example.fileshare

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.content.Context
import java.net.InetAddress

class DeviceManager(private val context: Context) {
    
    private val nsdManager: NsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val discoveredDevices = mutableListOf<Device>()
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    
    fun discoverDevices(callback: (List<Device>) -> Unit) {
        discoveredDevices.clear()
        
        stopDiscovery()
        
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                // Обнаружение запущено
            }
            
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceName.contains("FileShare")) {
                    nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            // Ошибка разрешения сервиса
                        }
                        
                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            val device = Device(
                                id = serviceInfo.serviceName,
                                name = serviceInfo.serviceName.replace("FileShare_", ""),
                                address = serviceInfo.host.hostAddress,
                                port = serviceInfo.port
                            )
                            
                            if (!discoveredDevices.contains(device)) {
                                discoveredDevices.add(device)
                                callback(discoveredDevices.toList())
                            }
                        }
                    })
                }
            }
            
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                val index = discoveredDevices.indexOfFirst { it.id == serviceInfo.serviceName }
                if (index != -1) {
                    discoveredDevices.removeAt(index)
                    callback(discoveredDevices.toList())
                }
            }
            
            override fun onDiscoveryStopped(serviceType: String) {
                // Обнаружение остановлено
            }
            
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                stopDiscovery()
            }
            
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                nsdManager.stopServiceDiscovery(this)
            }
        }
        
        nsdManager.discoverServices(
            "_fileshare._tcp",
            NsdManager.PROTOCOL_DNS_SD,
            discoveryListener
        )
    }
    
    fun stopDiscovery() {
        discoveryListener?.let {
            try {
                nsdManager.stopServiceDiscovery(it)
            } catch (e: Exception) {
                // Игнорируем ошибки при остановке обнаружения
            }
            discoveryListener = null
        }
    }
    
    fun registerService(port: Int) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "FileShare_Android_${android.os.Build.MODEL}"
            serviceType = "_fileshare._tcp"
            this.port = port
        }
        
        nsdManager.registerService(
            serviceInfo,
            NsdManager.PROTOCOL_DNS_SD,
            object : NsdManager.RegistrationListener {
                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    // Ошибка регистрации
                }
                
                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    // Ошибка отмены регистрации
                }
                
                override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                    // Сервис зарегистрирован
                }
                
                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                    // Регистрация сервиса отменена
                }
            }
        )
    }
} 