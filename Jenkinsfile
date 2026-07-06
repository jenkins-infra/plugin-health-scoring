#!/usr/bin/env groovy

// Do not rebuild daily if not on the principal branch (e.g. not on PR, not on other branches, not on tags)
final String cronPattern = env.BRANCH_IS_PRIMARY ? '@daily' : ''

// infra.ci.jenkins.io defaults to arm64 container agents while ci.jenkins.io has the default spot amd64 used by Java builds.
final String agentLabel = infra.isInfra() ? 'jnlp-linux-amd64' : 'maven-25'

pipeline {
  agent {
    label agentLabel
  }
  options {
    buildDiscarder(logRotator(numToKeepStr: '10'))
    skipStagesAfterUnstable()
    timestamps()
  }
  triggers {
    cron(cronPattern)
  }

  stages {
    stage('Build') {
      environment {
        JAVA_HOME = '/opt/jdk-25/'
      }
      steps {
        script {
          if (!env.BRANCH_IS_PRIMARY) {
            sh '''
              git remote -vvv
              git fetch origin main
            '''
          }
          infra.withArtifactCachingProxy() {
            def OPTS = env.MAVEN_SETTINGS ? "-s ${MAVEN_SETTINGS}" : ''
            // TODO enable spotless on infra when we understand why fetching origin/main is not enough
            OPTS += env.TAG_NAME || infra.isInfra() ? ' -Dspotless.check.skip=true' : ''
            withEnv(["OPTS=${OPTS}"]) {
              sh '''
                ./mvnw -V \
                  --no-transfer-progress \
                  ${OPTS} \
                  clean \
                  verify \
                  checkstyle:checkstyle \
                  spotbugs:spotbugs \
                  -Dmaven.test.failure.ignore \
                  -Dcheckstyle.failOnViolation=false \
                  -Dspotbugs.failOnError=false
              '''
            }
          }
        }
      }

      post {
        always {
          discoverGitReferenceBuild referenceJob: 'main'
          junit (
            allowEmptyResults: false,
            testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml'
          )
          recordCoverage(tools: [[parser: 'JACOCO', pattern: '**/target/site/**/jacoco.xml', mergeToOneReport: true]], sourceCodeRetention: 'MODIFIED')
          recordIssues enabledForFailure: true,
            tools: [mavenConsole(), java(), javaDoc()]
          recordIssues enabledForFailure: true,
            tool: checkStyle(),
            qualityGates: [[ threshold: 1, type: 'NEW', unstable: true ]]
          recordIssues enabledForFailure: true,
            tool: spotBugs(),
            qualityGates: [[ threshold: 1, type: 'NEW', unstable: true ]]
        }
        success {
          stash name: 'binary', includes: 'war/target/plugin-health-scoring.jar'
        }
      }
    }

    stage('Docker image') {
      steps {
        buildDockerAndPublishImage('plugin-health-scoring', [
          dockerfile: 'war/src/main/docker/Dockerfile',
          unstash: 'binary',
          disablePublication: !infra.isInfra(),
          publishToPrivateAzureRegistry: true,
          targetplatforms: 'linux/arm64',
          automaticSemanticVersioning: false,
        ])
      }
    }
  }
}
