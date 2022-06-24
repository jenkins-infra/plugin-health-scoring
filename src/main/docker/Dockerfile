FROM openjdk:17-oracle
RUN groupadd -r phs && useradd -r -g phs phs
USER phs:phs
ARG DEPENDENCY=./target/dependency
COPY ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY ${DEPENDENCY}/META-INF /app/META-INF
COPY ${DEPENDENCY}/BOOT-INF/classes /app
ENTRYPOINT ["java","-cp","./target/dependency/BOOT-INF/lib:/app/lib/*","io.jenkins.pluginhealth.scoring.PluginHealthScoring"]
