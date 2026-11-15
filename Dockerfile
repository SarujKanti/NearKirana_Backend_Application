# =====================================================================
# Builds and runs the NearKirana API in a container (used by Render).
#
# Stage 1 "build": has Maven + full JDK, compiles the code into one .jar
# Stage 2 "run":   has only a small Java runtime + that .jar (smaller, safer)
# =====================================================================

# ---------- Stage 1: build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Download dependencies first. Docker caches this layer, so later builds
# are fast when only the Java code changes (not pom.xml).
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
# Tests run on your laptop (they need a local database), so skip them here.
RUN mvn -q -B package -DskipTests

# ---------- Stage 2: run ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Do not run as root inside the container.
RUN useradd --system --uid 1001 appuser
USER appuser

COPY --from=build /app/target/*.jar app.jar

# Render's free plan has 512 MB RAM: let Java use at most 75% of it.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"

# Render sets PORT (default 10000); application.properties reads it.
EXPOSE 10000
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
