package com.ericom.jenkins.test

import com.ericom.jenkins.PipelineBase

class ConsulTestPipeline extends PipelineBase{

    ConsulTestPipeline(steps, current, environment) {
        super(steps, current, environment)
    }

    def downloadYamlFile() {
        this.steps.sh "wget ${this.config['files']['branchUrl']}${this.config['files']['yaml']}"
    }

    def runSystem() {
        this.steps.sh "docker swarm init --advertise-addr ${this.env.IP_ADDRES}"
        this.steps.sh "docker stack deploy -c ./${this.config['files']['yaml']} shield"
    }

    def run() {
        this.steps.stage('Setup consul') {
            this.downloadYamlFile()
            this.runSystem()
        }
    }
}
