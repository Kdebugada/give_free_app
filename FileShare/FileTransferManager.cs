using System;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Threading;
using System.Threading.Tasks;

namespace FileShare
{
    public class FileTransferManager
    {
        private TcpListener fileReceiver;
        private CancellationTokenSource cancellationTokenSource;
        
        public async Task<bool> SendFileAsync(string filePath, Device device)
        {
            try
            {
                using (TcpClient client = new TcpClient())
                {
                    await client.ConnectAsync(device.Address, device.Port);
                    
                    using (NetworkStream stream = client.GetStream())
                    {
                        // Отправляем имя файла
                        string fileName = Path.GetFileName(filePath);
                        byte[] fileNameBytes = System.Text.Encoding.UTF8.GetBytes(fileName);
                        
                        // Отправляем длину имени файла (4 байта)
                        byte[] fileNameLengthBytes = BitConverter.GetBytes(fileNameBytes.Length);
                        await stream.WriteAsync(fileNameLengthBytes, 0, fileNameLengthBytes.Length);
                        
                        // Отправляем имя файла
                        await stream.WriteAsync(fileNameBytes, 0, fileNameBytes.Length);
                        
                        // Получаем размер файла
                        long fileSize = new FileInfo(filePath).Length;
                        
                        // Отправляем размер файла (8 байт)
                        byte[] fileSizeBytes = BitConverter.GetBytes(fileSize);
                        await stream.WriteAsync(fileSizeBytes, 0, fileSizeBytes.Length);
                        
                        // Отправляем содержимое файла
                        using (FileStream fileStream = File.OpenRead(filePath))
                        {
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            
                            while ((bytesRead = await fileStream.ReadAsync(buffer, 0, buffer.Length)) > 0)
                            {
                                await stream.WriteAsync(buffer, 0, bytesRead);
                            }
                        }
                        
                        return true;
                    }
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Ошибка при отправке файла: {ex.Message}");
                return false;
            }
        }
        
        public void StartFileReceiver(int port, string saveDirectory, Action<string> onFileReceived)
        {
            try
            {
                cancellationTokenSource = new CancellationTokenSource();
                fileReceiver = new TcpListener(IPAddress.Any, port);
                fileReceiver.Start();
                
                Task.Run(async () =>
                {
                    try
                    {
                        while (!cancellationTokenSource.Token.IsCancellationRequested)
                        {
                            TcpClient client = await fileReceiver.AcceptTcpClientAsync();
                            
                            // Обрабатываем каждое подключение в отдельной задаче
                            _ = Task.Run(async () =>
                            {
                                try
                                {
                                    string filePath = await ReceiveFileAsync(client, saveDirectory);
                                    if (!string.IsNullOrEmpty(filePath))
                                    {
                                        onFileReceived?.Invoke(filePath);
                                    }
                                }
                                catch (Exception ex)
                                {
                                    Console.WriteLine($"Ошибка при получении файла: {ex.Message}");
                                }
                            });
                        }
                    }
                    catch (Exception ex) when (!(ex is OperationCanceledException))
                    {
                        Console.WriteLine($"Ошибка в сервере приема файлов: {ex.Message}");
                    }
                }, cancellationTokenSource.Token);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Ошибка при запуске сервера приема файлов: {ex.Message}");
                throw;
            }
        }
        
        private async Task<string> ReceiveFileAsync(TcpClient client, string saveDirectory)
        {
            try
            {
                using (NetworkStream stream = client.GetStream())
                {
                    // Получаем длину имени файла
                    byte[] fileNameLengthBytes = new byte[4];
                    await stream.ReadAsync(fileNameLengthBytes, 0, fileNameLengthBytes.Length);
                    int fileNameLength = BitConverter.ToInt32(fileNameLengthBytes, 0);
                    
                    // Получаем имя файла
                    byte[] fileNameBytes = new byte[fileNameLength];
                    await stream.ReadAsync(fileNameBytes, 0, fileNameBytes.Length);
                    string fileName = System.Text.Encoding.UTF8.GetString(fileNameBytes);
                    
                    // Получаем размер файла
                    byte[] fileSizeBytes = new byte[8];
                    await stream.ReadAsync(fileSizeBytes, 0, fileSizeBytes.Length);
                    long fileSize = BitConverter.ToInt64(fileSizeBytes, 0);
                    
                    // Создаем путь для сохранения файла
                    string filePath = Path.Combine(saveDirectory, fileName);
                    
                    // Получаем и сохраняем содержимое файла
                    using (FileStream fileStream = File.Create(filePath))
                    {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        long totalBytesRead = 0;
                        
                        while (totalBytesRead < fileSize && 
                               (bytesRead = await stream.ReadAsync(buffer, 0, buffer.Length)) > 0)
                        {
                            await fileStream.WriteAsync(buffer, 0, bytesRead);
                            totalBytesRead += bytesRead;
                        }
                    }
                    
                    return filePath;
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Ошибка при получении файла: {ex.Message}");
                return null;
            }
            finally
            {
                client.Close();
            }
        }
        
        public void StopFileReceiver()
        {
            cancellationTokenSource?.Cancel();
            fileReceiver?.Stop();
        }
    }
} 