package com.example.fileshare

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.net.Socket

class FileTransferManager(private val context: Context) {
    
    fun sendFile(fileUri: Uri, device: Device, callback: (Boolean) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val fileName = getFileName(fileUri)
                val fileSize = getFileSize(fileUri)
                
                Socket(device.address, device.port).use { socket ->
                    val outputStream = DataOutputStream(socket.getOutputStream())
                    
                    // Отправляем имя файла
                    outputStream.writeUTF(fileName)
                    
                    // Отправляем размер файла
                    outputStream.writeLong(fileSize)
                    
                    // Отправляем содержимое файла
                    context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                        }
                        
                        outputStream.flush()
                    }
                    
                    callback(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                callback(false)
            }
        }
    }
    
    private fun getFileName(uri: Uri): String {
        var result = "unknown"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    result = cursor.getString(nameIndex)
                }
            }
        }
        return result
    }
    
    private fun getFileSize(uri: Uri): Long {
        var result = 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    result = cursor.getLong(sizeIndex)
                }
            }
        }
        return result
    }
    
    fun startFileReceiver(port: Int, saveDir: File, onFileReceived: (File) -> Unit) {
        FileReceiverService.start(context, port, saveDir, onFileReceived)
    }
} 