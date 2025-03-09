package com.example.fileshare

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.DataInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicBoolean

class FileReceiverService : Service() {
    
    private var serverSocket: ServerSocket? = null
    private val isRunning = AtomicBoolean(false)
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val port = intent?.getIntExtra(EXTRA_PORT, DEFAULT_PORT) ?: DEFAULT_PORT
        val saveDirPath = intent?.getStringExtra(EXTRA_SAVE_DIR)
        
        if (saveDirPath != null) {
            val saveDir = File(saveDirPath)
            if (!saveDir.exists()) {
                saveDir.mkdirs()
            }
            
            startFileServer(port, saveDir)
        }
        
        return START_STICKY
    }
    
    private fun startFileServer(port: Int, saveDir: File) {
        if (isRunning.get()) return
        
        isRunning.set(true)
        
        GlobalScope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(port)
                val deviceManager = DeviceManager(this@FileReceiverService)
                deviceManager.registerService(port)
                
                while (isRunning.get()) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        
                        GlobalScope.launch(Dispatchers.IO) {
                            try {
                                clientSocket.use { socket ->
                                    val inputStream = DataInputStream(socket.getInputStream())
                                    
                                    // Получаем имя файла
                                    val fileName = inputStream.readUTF()
                                    
                                    // Получаем размер файла
                                    val fileSize = inputStream.readLong()
                                    
                                    // Создаем файл для сохранения
                                    val file = File(saveDir, fileName)
                                    
                                    // Сохраняем содержимое файла
                                    FileOutputStream(file).use { fileOutputStream ->
                                        val buffer = ByteArray(8192)
                                        var bytesRead: Int
                                        var totalBytesRead = 0L
                                        
                                        while (totalBytesRead < fileSize && 
                                               inputStream.read(buffer).also { bytesRead = it } != -1) {
                                            fileOutputStream.write(buffer, 0, bytesRead)
                                            totalBytesRead += bytesRead
                                        }
                                        
                                        fileOutputStream.flush()
                                    }
                                    
                                    // Уведомляем о получении файла
                                    val intent = Intent(ACTION_FILE_RECEIVED)
                                    intent.putExtra(EXTRA_FILE_PATH, file.absolutePath)
                                    sendBroadcast(intent)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    } catch (e: Exception) {
                        if (isRunning.get()) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                serverSocket?.close()
                isRunning.set(false)
            }
        }
    }
    
    override fun onDestroy() {
        isRunning.set(false)
        serverSocket?.close()
        super.onDestroy()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    companion object {
        private const val DEFAULT_PORT = 8888
        private const val EXTRA_PORT = "extra_port"
        private const val EXTRA_SAVE_DIR = "extra_save_dir"
        private const val EXTRA_FILE_PATH = "extra_file_path"
        const val ACTION_FILE_RECEIVED = "com.example.fileshare.FILE_RECEIVED"
        
        private var fileReceivedCallback: ((File) -> Unit)? = null
        
        fun start(context: Context, port: Int, saveDir: File, onFileReceived: (File) -> Unit) {
            fileReceivedCallback = onFileReceived
            
            val intent = Intent(context, FileReceiverService::class.java).apply {
                putExtra(EXTRA_PORT, port)
                putExtra(EXTRA_SAVE_DIR, saveDir.absolutePath)
            }
            
            context.startService(intent)
        }
    }
} 