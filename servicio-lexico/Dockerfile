FROM maven:3.9-eclipse-temurin-17 AS compilacion

WORKDIR /app
COPY pom.xml .
# Descargar dependencias primero (cache de Docker)
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app
COPY --from=compilacion /app/target/servicio-lexico-1.0.0.jar app.jar

EXPOSE 8001

CMD ["java", "-jar", "app.jar"]
