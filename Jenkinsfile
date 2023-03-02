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
        sh './mvnw -V deploy checkstyle:checkstyle spotbugs:spotbugs -Dmaven.test.failure.ignore -Dcheckstyle.failOnViolation=false -Dspotbugs.failOnError=false'
      }

      post {
        always {
          discoverGitReferenceBuild referenceJob: 'main'
          junit (
            allowEmptyResults: false,
            testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml'
          )
          publishCoverage adapters: [jacocoAdapter(mergeToOneReport: true, path: '**/target/site/**/jacoco.xml')]
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
        buildDockerAndPublishImage('plugin-health-scoring', [dockerfile: 'war/src/main/docker/Dockerfile', unstash: 'binary'])
      }
    }
  }
}
