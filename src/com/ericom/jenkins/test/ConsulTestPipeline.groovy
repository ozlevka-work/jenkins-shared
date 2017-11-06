package com.ericom.jenkins.test
import com.ericom.jenkins.PipelineBase

@Grab(group = 'org.yaml', module='snakeyaml', version = "1.18")
import org.yaml.snakeyaml.Yaml

class ConsulTestPipeline extends PipelineBase{
    def consul_run_config

    ConsulTestPipeline(steps, current, environment) {
        super(steps, current, environment)
    }

    def downloadYamlFile() {
        this.steps.sh "wget ${this.config['files']['branchUrl']}${this.config['files']['yaml']}"
    }

    def runSystem() {
        this.steps.sh "docker swarm init --advertise-addr ${this.env.IP_ADDRESS}"
        this.steps.sh "docker stack deploy -c ./${this.config['files']['yaml']} shield"
    }

    def createNewTag() {
        def yaml = new Yaml()
        this.consul_run_config = yaml.load("./${this.config['files']['yaml']}")
    }

    def run() {
        this.steps.stage('Setup consul') {
            this.downloadYamlFile()
            this.runSystem()
            this.createNewTag()
            this.steps.echo this.consul_run_config["services"]["consul-server"]["image"]
        }
    }
}
