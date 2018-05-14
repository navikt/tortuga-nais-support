#!/usr/bin/env groovy
@Library('peon-pipeline') _

node {
    def commitHash
    try {
        cleanWs()

        def version
        stage("checkout") {
            withCredentials([string(credentialsId: 'navikt-ci-oauthtoken', variable: 'GITHUB_OAUTH_TOKEN')]) {
                sh "git init"
                sh "git pull https://${GITHUB_OAUTH_TOKEN}:x-oauth-basic@github.com/navikt/tortuga-nais-support.git"
            }

            sh "make bump-version"

            version = sh(script: 'cat VERSION', returnStdout: true).trim()

            commitHash = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
            github.commitStatus("navikt-ci-oauthtoken", "navikt/tortuga-nais-support", 'continuous-integration/jenkins', commitHash, 'pending', "Build #${env.BUILD_NUMBER} has started")
        }

        stage("build") {
            sh "make"
        }

        stage("release") {
            sh "make release"

            withCredentials([string(credentialsId: 'navikt-ci-oauthtoken', variable: 'GITHUB_OAUTH_TOKEN')]) {
                sh "git push --tags https://${GITHUB_OAUTH_TOKEN}@github.com/navikt/tortuga-nais-support HEAD:master"
            }
        }

        github.commitStatus("navikt-ci-oauthtoken", "navikt/tortuga-nais-support", 'continuous-integration/jenkins', commitHash, 'success', "Build #${env.BUILD_NUMBER} has finished")
    } catch (err) {
        github.commitStatus("navikt-ci-oauthtoken", "navikt/tortuga-nais-support", 'continuous-integration/jenkins', commitHash, 'failure', "Build #${env.BUILD_NUMBER} has failed")

        throw err
    }
}
