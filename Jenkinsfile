#!/usr/bin/env groovy

pipeline {
  agent {
    label 'docker && linux'
  }
  options {
    buildDiscarder(logRotator(numToKeepStr: '5'))
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
          jacoco (
            runAlways: true,
            execPattern: '**/target/jacoco*.exec'
          )
          recordIssues enabledForFailure: true, tools: [mavenConsole(), java(), javaDoc()]
          recordIssues enabledForFailure: true, tool: checkStyle()
          recordIssues enabledForFailure: true, tool: spotBugs()
        }
      }
    }
  }
}
