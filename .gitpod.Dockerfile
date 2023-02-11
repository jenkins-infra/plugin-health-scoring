FROM gitpod/workspace-full
FROM gitpod/workspace-postgres

USER gitpod

# Set up Java environment
RUN bash -c '. /home/gitpod/.sdkman/bin/sdkman-init.sh && \
    sdk install java 17.0.3-ms && \
    sdk default java 17.0.3-ms'

# Set up Node.js environment
RUN bash -c 'VERSION="18.14.0" \
    && source $HOME/.nvm/nvm.sh && nvm install $VERSION \
    && nvm use $VERSION && nvm alias default $VERSION'
    
RUN echo "nvm use default &>/dev/null" >> ~/.bashrc.d/51-nvm-fix

# Set environment variables for connecting to PostgreSQL
ENV POSTGRES_HOST=localhost
ENV POSTGRES_PORT=5432
ENV POSTGRES_DB=postgres
ENV POSTGRES_USER=postgres
ENV POSTGRES_PASSWORD=postgres-s3cr3t

