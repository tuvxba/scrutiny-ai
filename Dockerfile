# Build stage
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests -B


# Run stage
FROM eclipse-temurin:25-jre AS run
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar
COPY --from=build /app/src/main/resources/kafka/kafka-truststore.jks /app/kafka-truststore.jks

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]