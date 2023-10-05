#!/usr/bin/env groovy

pipeline {
  agent {
    // 'docker' is the (legacy) label used on ci.jenkins.io for "Docker Linux AMD64" while 'linux-amd64-docker' is the label used on infra.ci.jenkins.io
    label 'docker || linux-amd64-docker'
  }
  options {
    buildDiscarder(logRotator(numToKeepStr: '10'))
    skipStagesAfterUnstable()
    timestamps()
  }

  stages {
    stage('Build') {
      environment {
        JAVA_HOME = '/opt/jdk-17/'
      }
      steps {
        script {
          infra.withArtifactCachingProxy() {
            def OPTS = env.MAVEN_SETTINGS ? "-s ${MAVEN_SETTINGS}" : ''
            sh """
              ./mvnw -V \
                --no-transfer-progress \
                ${OPTS} \
                verify \
                checkstyle:checkstyle \
                spotbugs:spotbugs \
                -Dmaven.test.failure.ignore \
                -Dcheckstyle.failOnViolation=false \
                -Dspotbugs.failOnError=false
            """
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
        buildDockerAndPublishImage('plugin-health-scoring', [dockerfile: 'war/src/main/docker/Dockerfile', unstash: 'binary', targetplatforms: 'linux/amd64,linux/arm64'])
      }
    }
  }
}
