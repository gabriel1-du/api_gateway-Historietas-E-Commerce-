# Etapa 1: Construcción
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Ejecución
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# El artifactId del pom.xml es "apigateway": target/apigateway-0.0.1-SNAPSHOT.jar
COPY --from=build /app/target/apigateway-0.0.1-SNAPSHOT.jar app.jar
# La aplicación escucha en 8888 (server.port en application.properties)
EXPOSE 8888
ENTRYPOINT ["java", "-jar", "app.jar"]
