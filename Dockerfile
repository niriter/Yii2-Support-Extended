FROM eclipse-temurin:19-jdk

WORKDIR /app

COPY gradle gradle
COPY gradlew .
COPY build.gradle.kts settings.gradle gradle.properties ./

RUN chmod +x gradlew

RUN ./gradlew --no-daemon dependencies || true

COPY . .

RUN ./gradlew --no-daemon buildPlugin
