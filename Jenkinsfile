#!/usr/bin/env groovy

pipeline {
  agent {
    label 'docker && linux'
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
            stash name: 'binary', includes: 'target/plugin-health-scoring.jar,src/main/docker/Dockerfile'
        }
      }
    }

    stage('Docker image') {
      /* when {
          branch 'main' // TODO for now, activated for the PR
      } */
      steps {
        unstash 'binary'
        buildDockerAndPublishImage('plugin-health-scoring', [dockerfile: 'src/main/docker/Dockerfile'])
      }
    }
  }
}
