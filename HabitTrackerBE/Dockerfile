# Build stage: Maven 3.10.0 matches .mvn/wrapper/maven-wrapper.properties.
FROM maven:3.10.0-eclipse-temurin-25 AS build

WORKDIR /build

# Dependencies first so a source-only change reuses this layer.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src/ src/
RUN mvn -B -q -DskipTests package \
    && mv target/HabitTracker-*.jar /build/app.jar

# Runtime stage: JRE only, no Maven and no build cache in the image.
FROM eclipse-temurin:25-jre

# Entry dates are stamped from the server clock, so the zone has to be the
# user's, not UTC. Override with TZ if that changes.
ENV TZ=Asia/Kolkata

WORKDIR /app

# The H2 file is a disposable working copy; the durable copy is the encrypted
# snapshot in the private data repository.
RUN useradd --create-home --uid 10001 --shell /usr/sbin/nologin habit \
    && mkdir -p /app/data \
    && chown -R habit:habit /app

COPY --from=build --chown=habit:habit /build/app.jar /app/app.jar

USER habit

# Render injects PORT; application.properties reads it, this is for local runs.
EXPOSE 8080

# Exec form keeps the JVM as PID 1 so Render's SIGTERM reaches the shutdown
# hook that pushes the final snapshot.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70.0", "-XX:+UseSerialGC", "-jar", "/app/app.jar"]
