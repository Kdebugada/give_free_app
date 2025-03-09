package com.example.fileshare

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fileshare.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var deviceAdapter: DeviceAdapter
    private val deviceManager = DeviceManager()
    private var selectedDevice: Device? = null
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            startDeviceDiscovery()
        } else {
            Toast.makeText(this, "Необходимы разрешения для работы приложения", Toast.LENGTH_LONG).show()
        }
    }
    
    private val selectFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { sendFile(it) }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupRecyclerView()
        setupButtons()
        checkPermissions()
    }
    
    private fun setupRecyclerView() {
        deviceAdapter = DeviceAdapter { device ->
            selectedDevice = device
            binding.selectedDeviceText.text = "Выбрано: ${device.name}"
        }
        
        binding.devicesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = deviceAdapter
        }
    }
    
    private fun setupButtons() {
        binding.refreshButton.setOnClickListener {
            startDeviceDiscovery()
        }
        
        binding.sendFileButton.setOnClickListener {
            if (selectedDevice == null) {
                Toast.makeText(this, "Сначала выберите устройство", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            selectFileLauncher.launch("*/*")
        }
    }
    
    private fun checkPermissions() {
        val requiredPermissions = arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest)
        } else {
            startDeviceDiscovery()
        }
    }
    
    private fun startDeviceDiscovery() {
        binding.progressBar.show()
        deviceManager.discoverDevices { devices ->
            runOnUiThread {
                deviceAdapter.updateDevices(devices)
                binding.progressBar.hide()
            }
        }
    }
    
    private fun sendFile(uri: Uri) {
        selectedDevice?.let { device ->
            binding.progressBar.show()
            val fileTransferManager = FileTransferManager(this)
            fileTransferManager.sendFile(uri, device) { success ->
                runOnUiThread {
                    binding.progressBar.hide()
                    if (success) {
                        Toast.makeText(this, "Файл успешно отправлен", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Ошибка при отправке файла", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
} 