FROM eclipse-temurin:17.0.4.1_1-jre

RUN groupadd -r phs && useradd -r -g phs phs
USER phs:phs

WORKDIR /app

COPY target/plugin-health-scoring.jar /app/plugin-health-scoring.jar

ENTRYPOINT ["java", "-jar", "/app/plugin-health-scoring.jar"]
