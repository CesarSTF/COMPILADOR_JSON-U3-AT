FROM maven:3.9-eclipse-temurin-21 AS compilacion

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
COPY --from=compilacion /app/target/servicio-orquestador-1.0.0.jar app.jar

EXPOSE 8000

CMD ["java", "-jar", "app.jar"]
