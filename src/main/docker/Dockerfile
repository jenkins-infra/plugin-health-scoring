FROM eclipse-temurin:17.0.4.1_1-jre

RUN groupadd -r phs && useradd -r -g phs phs
USER phs:phs

WORKDIR /app

COPY target/application/ ./
COPY target/dependencies/ ./
COPY target/snapshot-dependencies/ ./
COPY target/spring-boot-loader/ ./

ENTRYPOINT ["java","org.springframework.boot.loader.JarLauncher"]
