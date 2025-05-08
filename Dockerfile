FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY . /app

RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

CMD ["java", "-jar", "target/znsk-shortener-url-0.0.1-SNAPSHOT.jar"]
