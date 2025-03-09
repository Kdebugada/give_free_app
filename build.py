import os
import subprocess
import shutil
import sys

def build_exe():
    print("Начинаю сборку EXE-файла...")
    
    # Создаем директории для шаблонов и статических файлов, если они не существуют
    os.makedirs('templates', exist_ok=True)
    os.makedirs('static', exist_ok=True)
    os.makedirs('uploads', exist_ok=True)
    
    # Проверяем, установлены ли все зависимости
    try:
        subprocess.check_call([sys.executable, '-m', 'pip', 'install', '-r', 'requirements.txt'])
        print("Зависимости установлены успешно.")
    except subprocess.CalledProcessError:
        print("Ошибка при установке зависимостей.")
        return
    
    # Запускаем PyInstaller для создания EXE
    try:
        subprocess.check_call([
            sys.executable, 
            '-m', 
            'PyInstaller',
            '--name=FileTransfer',
            '--onefile',
            '--windowed',
            '--add-data=templates;templates',
            '--add-data=static;static',
            'app.py'
        ])
        print("EXE-файл успешно создан в директории dist/")
    except subprocess.CalledProcessError:
        print("Ошибка при создании EXE-файла.")
        return
    
    # Копируем README в директорию с EXE
    try:
        shutil.copy('README.md', 'dist/')
        print("README.md скопирован в директорию dist/")
    except:
        print("Не удалось скопировать README.md")
    
    print("Сборка завершена. EXE-файл находится в директории dist/FileTransfer.exe")

if __name__ == "__main__":
    build_exe() 