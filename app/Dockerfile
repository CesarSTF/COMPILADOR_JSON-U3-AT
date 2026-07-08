# Usar una imagen oficial de Python ligera
FROM python:3.11-slim

# Establecer directorio de trabajo
WORKDIR /app

# Instalar dependencias del sistema requeridas (opcional si necesitas compilar algo)
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Copiar el archivo de dependencias y ejecutarlas
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copiar todo el código de la aplicación (lexico, sintactico, semantico, main)
COPY . .

# Exponer el puerto donde corre FastAPI internamente
EXPOSE 8000

# Comando para ejecutar Uvicorn
CMD ["uvicorn", "adapters.in_web.main:app", "--host", "0.0.0.0", "--port", "8000"]
