package com.ericom.jenkins

class HelmInstall implements Serializable {
    def steps
    def currentBuild
    def builParams

    HelmInstall(steps, currentBuild, buidParams) {
        this.steps = steps
        this.currentBuild = currentBuild

        this.builParams = steps.readJSON text: buidParams
    }

    def checkParams() {
        this.steps.echo "${this.builParams.getClass().getName()}"
    }
}
