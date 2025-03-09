import os
import socket
import webbrowser
from flask import Flask, request, jsonify, send_from_directory, render_template
from flask_cors import CORS
from werkzeug.utils import secure_filename
import logging
import threading
import time

# Настройка логирования
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__, static_folder='static', template_folder='templates')
CORS(app)

# Конфигурация
UPLOAD_FOLDER = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'uploads')
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'mp4', 'mov', 'avi', 'mkv'}
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER
app.config['MAX_CONTENT_LENGTH'] = 500 * 1024 * 1024  # 500 MB максимальный размер файла

# Создаем папку для загрузок, если она не существует
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

def get_local_ip():
    try:
        # Получаем локальный IP-адрес
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception as e:
        logger.error(f"Ошибка при получении IP-адреса: {e}")
        return "127.0.0.1"

@app.route('/')
def index():
    return render_template('index.html', ip_address=get_local_ip(), port=app.config.get('PORT', 5000))

@app.route('/upload', methods=['POST'])
def upload_file():
    if 'file' not in request.files:
        return jsonify({'error': 'Файл не найден'}), 400
    
    file = request.files['file']
    
    if file.filename == '':
        return jsonify({'error': 'Файл не выбран'}), 400
    
    if file and allowed_file(file.filename):
        filename = secure_filename(file.filename)
        # Добавляем временную метку к имени файла, чтобы избежать перезаписи
        base, ext = os.path.splitext(filename)
        timestamp = int(time.time())
        filename = f"{base}_{timestamp}{ext}"
        
        file_path = os.path.join(app.config['UPLOAD_FOLDER'], filename)
        file.save(file_path)
        
        return jsonify({
            'success': True,
            'filename': filename,
            'message': 'Файл успешно загружен'
        })
    
    return jsonify({'error': 'Недопустимый тип файла'}), 400

@app.route('/files', methods=['GET'])
def list_files():
    files = []
    for filename in os.listdir(app.config['UPLOAD_FOLDER']):
        file_path = os.path.join(app.config['UPLOAD_FOLDER'], filename)
        if os.path.isfile(file_path) and allowed_file(filename):
            file_size = os.path.getsize(file_path)
            file_type = 'video' if filename.rsplit('.', 1)[1].lower() in {'mp4', 'mov', 'avi', 'mkv'} else 'image'
            files.append({
                'name': filename,
                'size': file_size,
                'type': file_type
            })
    
    return jsonify(files)

@app.route('/files/<filename>', methods=['GET'])
def get_file(filename):
    return send_from_directory(app.config['UPLOAD_FOLDER'], filename)

@app.route('/files/<filename>', methods=['DELETE'])
def delete_file(filename):
    file_path = os.path.join(app.config['UPLOAD_FOLDER'], filename)
    if os.path.exists(file_path):
        os.remove(file_path)
        return jsonify({'success': True, 'message': 'Файл успешно удален'})
    return jsonify({'error': 'Файл не найден'}), 404

def open_browser(ip, port):
    # Даем серверу время на запуск
    time.sleep(1.5)
    url = f"http://{ip}:{port}"
    webbrowser.open(url)
    logger.info(f"Открываю браузер по адресу: {url}")

def run_app(host='0.0.0.0', port=5000):
    app.config['PORT'] = port
    ip = get_local_ip()
    logger.info(f"Сервер запущен на http://{ip}:{port}")
    logger.info(f"Для доступа с телефона откройте в браузере: http://{ip}:{port}")
    
    # Запускаем браузер в отдельном потоке
    threading.Thread(target=open_browser, args=(ip, port), daemon=True).start()
    
    app.run(host=host, port=port, debug=False, threaded=True)

if __name__ == '__main__':
    run_app() 