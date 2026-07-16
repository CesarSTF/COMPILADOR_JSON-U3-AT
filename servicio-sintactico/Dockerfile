FROM maven:3.9-eclipse-temurin-21 AS compilacion

WORKDIR /app
COPY pom.xml .
# Descargar dependencias primero (cache de Docker)
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
COPY --from=compilacion /app/target/servicio-sintactico-1.0.0.jar app.jar

EXPOSE 8002

CMD ["java", "-jar", "app.jar"]
