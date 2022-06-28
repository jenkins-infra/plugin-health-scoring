FROM openjdk:17-oracle as builder

WORKDIR /app
COPY target/plugin-health-scoring.jar /app/

RUN java -Djarmode=layertools -jar plugin-health-scoring.jar extract

FROM azul/zulu-openjdk:17.0.2-jre-headless

RUN groupadd -r phs && useradd -r -g phs phs
USER phs:phs

WORKDIR /app

COPY --from=builder /app/dependencies/ ./
COPY --from=builder /app/spring-boot-loader/ ./
COPY --from=builder /app/snapshot-dependencies/ ./
COPY --from=builder /app/application/ ./

ENTRYPOINT ["java","org.springframework.boot.loader.JarLauncher"]
