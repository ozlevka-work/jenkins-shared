package com.ericom.jenkins.test
import com.ericom.jenkins.PipelineBase
import com.ericom.jenkins.test.TestFlow


@Grab(group = 'org.yaml', module='snakeyaml', version = "1.18")
import org.yaml.snakeyaml.Yaml

class ConsulTestPipeline extends PipelineBase{
    def consul_run_config

    ConsulTestPipeline(steps, current, environment) {
        super(steps, current, environment)
    }

    def downloadYamlFile() {
        this.steps.sh "wget -O ${this.config['files']['yaml']} ${this.config['files']['branchUrl']}${this.config['files']['yaml']}"
    }

    def runSystem() {
        this.steps.sh "docker swarm init --advertise-addr ${this.env.IP_ADDRESS}"
        def machineName = this.steps.sh(script: "docker node ls | grep Leader | awk '{ print \$3 }'", returnStdout: true).trim()
        this.steps.sh "docker node update --label-add management=yes ${machineName}"
        this.steps.sh "docker stack deploy -c ./${this.config['files']['yaml']} shield"
    }

    def readSwarmYaml() {
        def yaml = new Yaml()
        this.consul_run_config = yaml.load(new FileReader("${this.env.PWD}/${this.config['files']['yaml']}"))
    }

    def prepareImageToTag() {
        def arr = this.consul_run_config["services"]["consul-server"]["image"].split(':')
        return "${arr[0]}:${arr[1]} ${arr[0]}:jenkins-test"
    }

    def run() {
        def tst = new TestFlow(this.steps, this.config, this.env)
        this.readSwarmYaml()
        this.steps.stage('Clean environment') {
            tst.tryToClearEnvironment()
        }

        this.steps.stage('Setup consul') {
            this.downloadYamlFile()
            this.runSystem()
        }

        this.steps.stage("Make test requirements") {
            this.steps.sh "docker tag ${this.prepareImageToTag()}"
        }
    }
}
