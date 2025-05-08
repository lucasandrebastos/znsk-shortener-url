FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY . /app

RUN ./mvnw clean package -DskipTests

CMD ["java", "-jar", "target/znsk-shortener-url-0.0.1-SNAPSHOT.jar"]
