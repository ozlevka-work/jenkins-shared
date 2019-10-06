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
        if (containerName == null) {
            return
        }

        def containerFullName = String.format("%s:%s", containerName, tag)
        if ( kubeName == "esfluentBit" ) {
            this.chartValues["common"]["fluent-bit-out-syslog"]["images"][kubeName] = containerFullName
        } else {

            this.chartValues["shield-mng"]["images"][kubeName] = containerFullName
            this.chartValues["shield-proxy"]["images"][kubeName] = containerFullName
            this.chartValues["farm-services"]["images"][kubeName] = containerFullName

            if ( kubeName == "esInitElasticsearch" ) {
                this.chartValues['common']['fluent-bit-out-syslog']['images'][kubeName] = containerFullName
                this.chartValues["elk"]["management"]["images"][kubeName] = containerFullName
            }

            if (kubeName == "esCollector") {
                this.chartValues['common']['fluent-bit-out-syslog']['images'][kubeName] = containerFullName
            }
        }
    }

    def clearRunningCluster() {

        def helm_get_cmd = '''
           #!/bin/bash
           helm ls | grep DEPLOYED | awk '{print $1}'
           
        '''
        this.runCommandSplitOutputAndRun(helm_get_cmd,"""
                    #!/bin/bash
                    helm delete --purge %s
        """)

        def purge_browsers_cmd = '''
            #!/bin/bash
            kubectl -n farm-services get job | grep -v NAME | awk '{print $1}'
        '''
        this.runCommandSplitOutputAndRun(purge_browsers_cmd, """
             #!/bin/bash
             kubectl -n farm-services delete job %s
        """)
    }

    def runCommandSplitOutputAndRun(command, template) {
        def commandOutput = this.steps.sh script: command, returnStdout: true
        def command_out_array = commandOutput.split('\n')
        if (command_out_array.size() > 0 && command_out_array[0] != '') {
            for(def el : command_out_array) {
                def cmd = String.format(template, el)
                this.steps.sh script: cmd
            }
        }
    }
}
