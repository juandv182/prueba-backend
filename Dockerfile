# Utiliza la imagen baseCOPY pom.xml /app de OpenJDK 17
FROM maven:3.8-openjdk-17

# Establece el directorio de trabajo dentro del contenedor
WORKDIR /app

# Primero, copia solo el archivo pom.xml
COPY pom.xml /app

# Descarga todas las dependencias
RUN mvn dependency:go-offline

# Ahora, copia el resto del código fuente
COPY src /app/src

# Construye la aplicación y salta los test para ahorrar tiempo
RUN mvn clean package -DskipTests

# Indica que el contenedor escucha en el puerto 8080
EXPOSE 8080

# Especifica el comando para iniciar la aplicación
ENTRYPOINT ["java", "-Xmx5000M", "-jar", "/app/target/backend-0.0.1-SNAPSHOT.jar"]
