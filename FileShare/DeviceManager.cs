using System;
using System.Collections.Generic;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading.Tasks;
using Zeroconf;

namespace FileShare
{
    public class DeviceManager
    {
        private const string ServiceType = "_fileshare._tcp.local.";
        private TcpListener serviceListener;
        
        public async Task<List<Device>> DiscoverDevicesAsync()
        {
            List<Device> devices = new List<Device>();
            
            try
            {
                var results = await ZeroconfResolver.ResolveAsync(ServiceType);
                
                foreach (var result in results)
                {
                    if (result.ServiceName.Contains("FileShare"))
                    {
                        string address = null;
                        int port = 0;
                        
                        foreach (var service in result.Services)
                        {
                            if (service.Key == ServiceType)
                            {
                                address = service.Value.Addresses[0];
                                port = service.Value.Port;
                                break;
                            }
                        }
                        
                        if (!string.IsNullOrEmpty(address) && port > 0)
                        {
                            var device = new Device
                            {
                                Id = result.ServiceName,
                                Name = result.ServiceName.Replace("FileShare_", ""),
                                Address = address,
                                Port = port
                            };
                            
                            devices.Add(device);
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Ошибка при обнаружении устройств: {ex.Message}");
            }
            
            return devices;
        }
        
        public void RegisterService(int port)
        {
            try
            {
                // Запускаем TcpListener для регистрации сервиса
                serviceListener = new TcpListener(IPAddress.Any, port);
                serviceListener.Start();
                
                // Регистрируем сервис с помощью Zeroconf
                var properties = new Dictionary<string, string>
                {
                    { "device", "windows" }
                };
                
                string hostname = Environment.MachineName;
                string serviceName = $"FileShare_Windows_{hostname}";
                
                ZeroconfPublisher.RegisterService(serviceName, ServiceType, port, properties);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Ошибка при регистрации сервиса: {ex.Message}");
                throw;
            }
        }
    }
} 