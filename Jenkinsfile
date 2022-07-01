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
        sh './mvnw -V verify checkstyle:check spotbugs:check -Dmaven.test.failure.ignore -Dcheckstyle.failOnViolation=false -Dspotbugs.failOnError=false'
      }

      post {
        always {
          junit (
            allowEmptyResults: true,
            testResults: './target/surefire-reports/*.xml'
          )
          junit (
            allowEmptyResults: true,
            testResults: './target/failsafe-reports/*.xml'
          )
          recordIssues(
            enabledForFailure: true,
            aggregatingResults: true,
            tools: [
              java(),
              checkStyle(pattern: './target/checkstyle-result.xml', reportEncoding: 'UTF-8'),
              spotBugs(pattern: './target/spotbugsXml.xml')
            ]
          )
        }
      }
    }
  }
}
