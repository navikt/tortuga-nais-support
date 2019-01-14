#!/usr/bin/env groovy
@Library('peon-pipeline') _

node {
    def appToken
    def commitHash
    try {
        cleanWs()

        def version
        stage("checkout") {

            appToken = github.generateAppToken()

            sh "git init"
            sh "git pull https://x-access-token:$appToken@github.com/navikt/tortuga-nais-support.git"

            sh "make bump-version"

            version = sh(script: 'cat VERSION', returnStdout: true).trim()

            commitHash = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
            github.commitStatus("pending", "navikt/tortuga-nais-support", appToken, commitHash)
        }

        stage("build") {
            sh "make"
        }

        stage("release") {
            sh "make release"
            sh "git push --tags https://x-access-token:$appToken@github.com/navikt/tortuga-nais-support HEAD:master"
        }

        github.commitStatus("pending", "navikt/tortuga-nais-support", appToken, commitHash)
    } catch (err) {
        github.commitStatus("pending", "navikt/tortuga-nais-support", appToken, commitHash)
        throw err
    }
}
