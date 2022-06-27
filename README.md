# Plugin Health Scoring system

This project aims to introduce a metric system to calculate the health score of each plugin within the Jenkins ecosystem and reflect the final scores on the Plugin Site for the plugin maintainers and users.

---

This project is part of the Google Summer of Code 2022 selection for the Jenkins project.

You can find out more about the project at https://www.jenkins.io/projects/gsoc/2022/projects/plugin-health-scoring-system.

# Set up the project locally

This project is composed of 2 essential services:
1. Spring Boot Application
2. PostgreSQL Database

And in order to start up these services and keep them up and running, we'll use a containerized PostgrSQL DB and build an image for the Spring Boot App using a custom Dockerfile. All of this will be done via docker-compose.yml file.

**Steps**
- Clone this repository using:
  - *git clone https://github.com/jenkins-infra/plugin-health-scoring.git*
- Enter inside the newly cloned repo, using:
  - *cd plugin-health-scoring*
- Create a JAR file of this project inside the directory *./target/*, using:
  - *mvn package -DskipTests*
- Run the docker-compose.yml file to start both the services, using:
  - *docker compose up*


**NOTE**

This project uses a **.env** file (at the root folder) where we store our environment variables used for connecting to the database. For example, here's a list of the variables you'd need to override:

```
POSTGRES_USER=changeIt
POSTGRES_PASSWORD=changeIt
POSTGRES_DB=testdb
POSTGRES_PORT=5432
POSTGRES_HOST=localhost
```

