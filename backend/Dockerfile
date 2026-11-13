# Multi-stage build: compile with the full JDK + Maven, ship only the JRE and the jar.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Copy the pom first so the dependency download is cached until the pom changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
# Do not run as root inside the container.
RUN useradd --system --no-create-home taskflow
USER taskflow
COPY --from=build /app/target/taskflow-api-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
