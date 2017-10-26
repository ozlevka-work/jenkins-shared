package com.ericom.jenkins

@Grab(group = 'org.yaml', module='snakeyaml', version = "1.18")
import org.yaml.snakeyaml.Yaml

class PipelineBase implements Serializable {
    def steps
    def config
    def currentBuild
    def env

    PipelineBase(steps, current, environment) {
        this.steps = steps
        this.currentBuild = current
        this.env = environment
    }

    def loadConfig(String yml) {
        def yaml = new Yaml()
        this.config = yaml.load(yml)
    }


    def fetchCodeChanges() {
        this.steps.stage("Get latest version") {
            if (this.config["svc"].containsKey('branch')) {
                this.steps.git([url: this.config['svc']['url'], credentialsId: this.config['credentials']['git'], branch: this.config['svc']['branch'], changelog: true])
            } else {
                this.steps.git([url: this.config['svc']['url'], credentialsId: this.config['credentials']['git'], changelog: true])
            }
        }
    }
}