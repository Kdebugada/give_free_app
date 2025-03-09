using System;
using System.Collections.ObjectModel;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Threading.Tasks;
using System.Windows;
using Microsoft.Win32;

namespace FileShare
{
    public partial class MainWindow : Window
    {
        private ObservableCollection<Device> devices = new ObservableCollection<Device>();
        private Device selectedDevice;
        private DeviceManager deviceManager;
        private FileTransferManager fileTransferManager;
        private string saveDirectory;

        public MainWindow()
        {
            InitializeComponent();
            
            // Инициализация менеджеров
            deviceManager = new DeviceManager();
            fileTransferManager = new FileTransferManager();
            
            // Настройка директории для сохранения файлов
            saveDirectory = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments),
                "FileShare"
            );
            
            if (!Directory.Exists(saveDirectory))
            {
                Directory.CreateDirectory(saveDirectory);
            }
            
            // Привязка списка устройств к ListView
            DevicesListView.ItemsSource = devices;
            
            // Запуск сервера для приема файлов
            StartFileReceiver();
            
            // Обнаружение устройств
            RefreshDevices();
        }

        private async void RefreshDevices()
        {
            RefreshButton.IsEnabled = false;
            ProgressBar.Visibility = Visibility.Visible;
            
            try
            {
                var discoveredDevices = await deviceManager.DiscoverDevicesAsync();
                
                devices.Clear();
                foreach (var device in discoveredDevices)
                {
                    devices.Add(device);
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show($"Ошибка при обнаружении устройств: {ex.Message}", "Ошибка", MessageBoxButton.OK, MessageBoxImage.Error);
            }
            finally
            {
                RefreshButton.IsEnabled = true;
                ProgressBar.Visibility = Visibility.Collapsed;
            }
        }

        private void StartFileReceiver()
        {
            try
            {
                int port = 8888;
                fileTransferManager.StartFileReceiver(port, saveDirectory, OnFileReceived);
                deviceManager.RegisterService(port);
            }
            catch (Exception ex)
            {
                MessageBox.Show($"Ошибка при запуске сервера: {ex.Message}", "Ошибка", MessageBoxButton.OK, MessageBoxImage.Error);
            }
        }

        private void OnFileReceived(string filePath)
        {
            Dispatcher.Invoke(() =>
            {
                MessageBox.Show($"Получен файл: {Path.GetFileName(filePath)}", "Файл получен", MessageBoxButton.OK, MessageBoxImage.Information);
            });
        }

        private void RefreshButton_Click(object sender, RoutedEventArgs e)
        {
            RefreshDevices();
        }

        private void DevicesListView_SelectionChanged(object sender, System.Windows.Controls.SelectionChangedEventArgs e)
        {
            selectedDevice = DevicesListView.SelectedItem as Device;
            
            if (selectedDevice != null)
            {
                SelectedDeviceText.Text = $"Выбрано: {selectedDevice.Name}";
                SendFileButton.IsEnabled = true;
            }
            else
            {
                SelectedDeviceText.Text = "Устройство не выбрано";
                SendFileButton.IsEnabled = false;
            }
        }

        private async void SendFileButton_Click(object sender, RoutedEventArgs e)
        {
            if (selectedDevice == null)
            {
                MessageBox.Show("Сначала выберите устройство", "Внимание", MessageBoxButton.OK, MessageBoxImage.Warning);
                return;
            }
            
            OpenFileDialog openFileDialog = new OpenFileDialog
            {
                Filter = "Все файлы (*.*)|*.*",
                Title = "Выберите файл для отправки"
            };
            
            if (openFileDialog.ShowDialog() == true)
            {
                string filePath = openFileDialog.FileName;
                
                SendFileButton.IsEnabled = false;
                ProgressBar.Visibility = Visibility.Visible;
                
                try
                {
                    bool success = await fileTransferManager.SendFileAsync(filePath, selectedDevice);
                    
                    if (success)
                    {
                        MessageBox.Show("Файл успешно отправлен", "Успех", MessageBoxButton.OK, MessageBoxImage.Information);
                    }
                    else
                    {
                        MessageBox.Show("Ошибка при отправке файла", "Ошибка", MessageBoxButton.OK, MessageBoxImage.Error);
                    }
                }
                catch (Exception ex)
                {
                    MessageBox.Show($"Ошибка при отправке файла: {ex.Message}", "Ошибка", MessageBoxButton.OK, MessageBoxImage.Error);
                }
                finally
                {
                    SendFileButton.IsEnabled = true;
                    ProgressBar.Visibility = Visibility.Collapsed;
                }
            }
        }
    }
} 