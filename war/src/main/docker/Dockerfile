FROM eclipse-temurin:17.0.6_10-jdk as builder

WORKDIR /src

COPY war/target/plugin-health-scoring.jar /src/plugin-health-scoring.jar
RUN java -Djarmode=layertools -jar plugin-health-scoring.jar extract

FROM eclipse-temurin:17.0.6_10-jre

ENV HOME /app

ARG user=phs
ARG uid=1000
ARG group=phs
ARG gid=1000

ARG server_port=8080
ARG spring_profile=production

RUN groupadd -r -g ${gid} ${group} \
    && useradd -l -r -d ${HOME} -u ${uid} -g ${gid} ${user}
USER ${user}:${group}

WORKDIR ${HOME}

ENV SPRING_PROFILES_ACTIVE ${spring_profile}
ENV SERVER_PORT ${server_port}

ENTRYPOINT ["/bin/bash", "/app/docker-entrypoint.sh"]

EXPOSE ${server_port}

COPY --chown=${user}:${group} war/src/main/docker/docker-entrypoint.sh .

COPY --from=builder --chown=${user}:${group} /src/dependencies/ ./
COPY --from=builder --chown=${user}:${group} /src/spring-boot-loader/ ./
COPY --from=builder --chown=${user}:${group} /src/snapshot-dependencies/ ./
COPY --from=builder --chown=${user}:${group} /src/application/ ./
