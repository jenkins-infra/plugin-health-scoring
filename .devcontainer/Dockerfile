FROM mcr.microsoft.com/vscode/devcontainers/java:0-17.0.2

# Install sdkman
RUN curl -s "https://get.sdkman.io" | bash

# Install Java 17.0.3-ms using sdkman
RUN /bin/bash -c "source /root/.sdkman/bin/sdkman-init.sh && sdk install java 17.0.3.ms-zulu"

# Install Node.js 18.14.0 using sdkman
RUN /bin/bash -c "source /root/.sdkman/bin/sdkman-init.sh && sdk install nodejs 18.14.0"

# Install Maven 3.9.0 using sdkman
RUN /bin/bash -c "source /root/.sdkman/bin/sdkman-init.sh && sdk install maven 3.9.0"

# Install PostgreSQL client
RUN apt-get update && export DEBIAN_FRONTEND=noninteractive \
    && apt-get -y install --no-install-recommends postgresql-client

# Install Yarn
RUN npm install -g yarn
