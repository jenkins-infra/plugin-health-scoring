FROM eclipse-temurin:21-jdk AS builder

WORKDIR /src

COPY war/target/plugin-health-scoring.jar /src/plugin-health-scoring.jar
RUN jar -xf plugin-health-scoring.jar

FROM eclipse-temurin:21-jre

ENV HOME=/app

ARG user=phs
ARG uid=4242
ARG group=phs
ARG gid=4242

ARG server_port=8080
ARG spring_profile=production

RUN groupadd -r -g ${gid} ${group} &&\
  useradd -l -r -d ${HOME} -u ${uid} -g ${gid} ${user}
USER ${user}:${group}

WORKDIR ${HOME}

ENV SPRING_PROFILES_ACTIVE=${spring_profile}
ENV SERVER_PORT=${server_port}

ENTRYPOINT ["/bin/bash", "/app/docker-entrypoint.sh"]

EXPOSE ${server_port}

COPY --chown=${user}:${group} war/src/main/docker/docker-entrypoint.sh .

COPY --chown=${user}:${group} --from=builder /src/BOOT-INF/lib ./lib
COPY --chown=${user}:${group} --from=builder /src/META-INF ./META-INF
COPY --chown=${user}:${group} --from=builder /src/BOOT-INF/classes ./
