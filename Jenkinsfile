#!/usr/bin/env groovy

pipeline {
  agent {} // TODO
  options {
    buildDiscarder(logRotator(numToKeepStr: '5'))
    timestamps()
  }

  stages {
    stage('Build') {
      steps {
        sh './mvnw verify checkstyle:check spotbugs:check -Dmaven.test.failure.ignore -Dcheckstyle.failOnViolation=false'
      }

      post {
        always {
          junit (
            allowEmptyResults: false,
            testResults: './target/surefire-reports/*.xml'
          )
          recordIssues(
            enabledForFailure: true,
            aggregatingResults: true,
            tools: [
              java(),
              checkStyle(pattern: './target/checkstyle-result.xml', reportEncoding: 'UTF-8'),
              spotbugs(pattern: './target/spotbugsXml.xml')
            ]
          )
        }
      }
    }
  }
}
