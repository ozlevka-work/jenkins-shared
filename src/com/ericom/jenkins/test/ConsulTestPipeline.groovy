package com.ericom.jenkins.test
import com.ericom.jenkins.PipelineBase
import com.ericom.jenkins.test.TestFlow




class ConsulTestPipeline extends PipelineBase{
    def consul_run_config
    def machine_name

    ConsulTestPipeline(steps, current, environment) {
        super(steps, current, environment)
    }

    def downloadYamlFile() {
        this.steps.sh "wget -O ${this.config['files']['yaml']} ${this.config['files']['branchUrl']}${this.config['files']['yaml']}"
    }

    def runSystem() {
        this.steps.sh "docker swarm init --advertise-addr ${this.env.IP_ADDRESS}"
        this.machine_name = this.steps.sh(script: "docker node ls | grep Leader | awk '{ print \$3 }'", returnStdout: true).trim()
        this.steps.sh "docker node update --label-add management=yes ${this.machine_name}"
        this.steps.sh "docker stack deploy -c ./${this.config['files']['yaml']} shield"
    }

    def readSwarmYaml() {
        this.downloadYamlFile()
        this.consul_run_config = this.steps.readYaml file:"${this.env.PWD}/${this.config['files']['yaml']}"
    }

    def prepareImageToTag() {
        def arr = this.consul_run_config["services"]["consul-server"]["image"].split(':')
        return "${arr[0]}:${arr[1]} ${arr[0]}:jenkins-test"
    }

    def run() {
        def tst = new TestFlow(this.steps, this.config, this.env)
        this.steps.stage('Clean environment') {
            tst.tryToClearEnvironment()
        }

        try {
            this.steps.stage('Setup consul') {
                this.readSwarmYaml()
                this.runSystem()
            }

            this.steps.stage('Get latest version of test') {
                this.fetchCodeChanges()
            }

            this.steps.stage("Make containers ready") {
                this.steps.sh "cd Containers/Docker/shield-virtual-client && docker build -t consul-test:latest -f Docker-consul ."
                this.steps.sh "docker tag ${this.prepareImageToTag()}"
            }

            this.steps.stage("Run test") {
                def reports_dir = "${env.PWD}"
                this.steps.echo "Reports dir: ${reports_dir}"
                this.steps.sh "docker run --rm -t -e CONSUL_ADDRESS=${this.machine_name} --network host -v /var/run/docker.sock:/var/run/docker.sock -v ${reports_dir}:/reports consul-test:latest"
            }
        } finally {
            this.steps.stage('Clean environment') {
                tst.tryToClearEnvironment()
            }
        }
    }
}
