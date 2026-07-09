# syntax=docker/dockerfile:1

# ---- Build (Maven + JDK 21) ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Cache des dépendances : on copie d'abord le pom seul.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp dependency:go-offline
COPY src ./src
# Les tests tournent dans la CI (Testcontainers) ; on ne les rejoue pas dans l'image.
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp clean package -DskipTests

# ---- Runtime (JRE 21, non-root) ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd -r app && useradd -r -g app app
COPY --from=build /app/target/*.jar app.jar
USER app
EXPOSE 8284
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
