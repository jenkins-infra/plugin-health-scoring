FROM mcr.microsoft.com/devcontainers/java:0-17

ARG INSTALL_MAVEN="true"
ARG MAVEN_VERSION="3.9.0"

RUN if [ "${INSTALL_MAVEN}" = "true" ]; then su vscode -c "umask 0002 && . /usr/local/sdkman/bin/sdkman-init.sh && sdk install maven \"${MAVEN_VERSION}\""; fi


