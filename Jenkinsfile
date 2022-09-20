#!/usr/bin/env groovy
@Library('pipeline-library@pull/465/head') _

pipeline {
  agent {
    label 'linux-amd64-docker'
  }
  options {
    buildDiscarder(logRotator(numToKeepStr: '10'))
    timestamps()
  }

  stages {
    stage('Build') {
      environment {
        JAVA_HOME = '/opt/jdk-17/'
      }
      steps {
        sh './mvnw -V verify checkstyle:checkstyle spotbugs:spotbugs -Dmaven.test.failure.ignore -Dcheckstyle.failOnViolation=false -Dspotbugs.failOnError=false'
      }

      post {
        always {
          junit (
            allowEmptyResults: true,
            testResults: '**/target/surefire-reports/*.xml'
          )
          junit (
            allowEmptyResults: true,
            testResults: '**/target/failsafe-reports/*.xml'
          )
          publishCoverage adapters: [jacocoAdapter(mergeToOneReport: true, path: '**/target/site/**/jacoco.xml')]
          recordIssues enabledForFailure: true, tools: [mavenConsole(), java(), javaDoc()]
          recordIssues enabledForFailure: true, tool: checkStyle()
          recordIssues enabledForFailure: true, tool: spotBugs()
        }
        success {
            stash name: 'binary', includes: 'target/plugin-health-scoring.jar'
        }
      }
    }

    stage('Docker image') {
      steps {
        buildDockerAndPublishImage('plugin-health-scoring', [dockerfile: 'src/main/docker/Dockerfile', unstash: 'binary'])
      }
    }
  }
}
