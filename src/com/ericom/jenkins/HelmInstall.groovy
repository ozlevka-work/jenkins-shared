package com.ericom.jenkins

class HelmInstall implements Serializable {
    def steps
    def currentBuild
    def buildParams

    HelmInstall(steps, currentBuild, buildParams) {
        this.steps = steps
        this.currentBuild = currentBuild
        this.buildParams = buildParams
    }

    def checkParams() {
        this.steps.echo "${this.buildParams.rootKey}"
    }
}
