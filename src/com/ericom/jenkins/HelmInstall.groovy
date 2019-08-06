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

    def updateValues(tag) {
        for (def a : this.buildParams["rootKey"].keySet()) {
            if (this.config["components"].containsKey(a)) {
                def cnf = this.config["components"][a]
                this.updateChartValueWithTag(cnf['kubeName'], cnf['container'], tag)
            }
        }
    }

    def updateChartValueWithTag(kubeName, containerName, tag) {
        def containerFullName = String.format("%s:%s", containerName, tag)
        if ( kubeName == "fluentBit" ) {
            this.chartValues["elk"]["fluent-bit-out-syslog"]["images"][kubeName] = containerFullName
        } else {

            this.chartValues["shield-mng"]["images"][kubeName] = containerFullName
            this.chartValues["shield-proxy"]["images"][kubeName] = containerFullName
            this.chartValues["farm-services"]["images"][kubeName] = containerFullName

            if ( kubeName == "esInitElasticsearch" ) {
                this.chartValues["elk"]["fluent-bit-out-syslog"]["images"][kubeName] = containerFullName
                this.chartValues["elk"]["management"]["images"][kubeName] = containerFullName
            }
        }
    }

    def clearRunningCluster() {
        def counter = 0
        def clusterCurrent = this.steps.sh script: '''
           #!/bin/bash
           helm ls | grep DEPLOYED | awk '{print $1}'
           
        ''', returnStdout: true
        def deployed = clusterCurrent.split('\n')
        if(deployed.size() > 0 && deployed[0] != '') {
            for(def ln : deployed) {
                this.steps.echo "Going delete ${counter}: ${ln}"
                this.steps.sh script: """
                    #!/bin/bash
                    helm delete --purge ${ln}
                """
                counter++
            }
        }
    }
}
