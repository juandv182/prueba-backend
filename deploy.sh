#!/bin/bash
# Verifica si Docker está instalado
if ! [ -x "$(command -v docker)" ]; then
  echo 'Docker no está instalado. Instalando Docker...'
  sudo apt-get update
  sudo apt-get -y install apt-transport-https ca-certificates curl software-properties-common
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
  sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(. /etc/os-release; echo "$UBUNTU_CODENAME") stable"
  sudo apt-get update
  sudo apt-get -y install docker-ce docker-compose
  sudo usermod -aG docker $USER
  # Refresca los permisos de grupo para el usuario actual
  exec sg docker newgrp "$(id -gn)"
  echo 'Docker instalado correctamente.'
else
  echo 'Docker ya está instalado.'
fi

# Verifica e inicia el servicio de Docker si no está corriendo
if [ "$(systemctl is-active docker)" != "active" ]; then
  echo 'Iniciando el servicio de Docker...'
  sudo systemctl start docker
  echo 'Servicio de Docker iniciado.'
else
  echo 'El servicio de Docker ya está corriendo.'
fi

# Ejecuta los comandos Docker Compose
docker-compose build
docker-compose down
docker-compose up -d

# Muestra los contenedores en ejecución
docker ps
