package com.ericom.jenkins

class HelmInstall implements Serializable {
    def steps
    def currentBuild
    def buildParams
    def config
    def chartValues

    HelmInstall(steps, currentBuild, buildParams) {
        this.steps = steps
        this.currentBuild = currentBuild
        this.buildParams = buildParams
    }

    def loadConfig(yaml) {
        this.config = this.steps.readYaml(text: yaml)
    }

    def readChartValues(valuesPath) {
        this.chartValues = this.steps.readYaml(file: valuesPath)
    }
}
