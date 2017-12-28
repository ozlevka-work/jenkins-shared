package com.ericom.jenkins.test
import com.ericom.jenkins.PipelineBase
import com.ericom.jenkins.test.TestFlow
import com.ericom.jenkins.test.EricomYamlParser

//JenkinsPipelineUnit


class ConsulTestPipeline extends PipelineBase{
    final CONSUL_YAML_NAME = "consul-compose.yaml"
    def consul_run_config
    def machine_name
    def tst

    ConsulTestPipeline(steps, current, environment) {
        super(steps, current, environment)
        this.tst = new TestFlow(this.steps, this.config, this.env)
    }

    def downloadYamlFile() {
        this.steps.sh "wget -O ${this.config['files']['yaml']} ${this.config['files']['branchUrl']}${this.config['files']['yaml']}"
    }

    def runSystem() {
        this.steps.sh "docker swarm init --advertise-addr ${this.env.IP_ADDRESS}"
        this.machine_name = this.steps.sh(script: "docker node ls | grep Leader | awk '{ print \$3 }'", returnStdout: true).trim()
        this.steps.sh "docker node update --label-add management=yes ${this.machine_name}"
        this.steps.sh "docker stack deploy -c ./${CONSUL_YAML_NAME} shield"
    }

    def readSwarmYaml() {
        this.downloadYamlFile()
        def parser = new EricomYamlParser()
        parser.loadFile("${this.env.PWD}/${this.config['files']['yaml']}")
        def writer = new FileWriter("${this.env.PWD}/${CONSUL_YAML_NAME}", false)
        writer.write(parser.makeConsulTestYaml())
        writer.flush()
        writer.close()
    }

    def prepareImageToTag() {
        return "\"${this.config['test']['consul-image']}:latest ${this.config['test']['consul-image']}:jenkins-test"
    }

    def makeReportsDirPath() {
        return new File("${this.env.TEST_HOME}").getParent()
    }

    def makeTestContainerRunScript(reports_dir, command = '') {
        return  "docker run --rm -t " +
                " -e CONSUL_ADDRESS=${this.machine_name} " +
                "  --network host -v /var/run/docker.sock:/var/run/docker.sock " +
                " -v ${reports_dir}:/reports consul-test:latest " + command
    }

    def make_consul_cluster() {
        this.steps.stage('Clean environment') {
            this.tst.tryToClearEnvironment()
        }

        this.steps.stage('Setup consul') {
            this.readSwarmYaml()
            this.runSystem()
        }

    }

    def run() {

        def reports_dir

        this.steps.stage('Clean environment') {
            this.tst.tryToClearEnvironment()
        }

        try {
            this.steps.stage('Setup consul') {
                this.readSwarmYaml()
                this.runSystem()
            }

            this.fetchCodeChanges()

            this.steps.stage("Make containers ready") {
                this.steps.sh "cd Containers/Docker/shield-virtual-client && docker build -t consul-test:latest -f Docker-consul ."
                this.steps.sh "docker tag ${this.prepareImageToTag()}"
            }

            try {
                this.steps.stage("Run test") {
                    reports_dir = "${this.makeReportsDirPath()}/consul_admin_test_ha"
                    this.steps.echo "Reports dir: ${reports_dir}"
                    this.steps.sh this.makeTestContainerRunScript(reports_dir)
                }
            } catch (Exception ex) {
                throw ex
            } finally {
                this.steps.stage("Publish report") {
                    this.steps.publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: true, reportDir: 'report', reportFiles: 'mochawesome.html', reportName: "Consul update Test Running  Report for Build ${env.BUILD_NUMBER}", reportTitles: ''])
                }
            }

            this.steps.stage('Clean environment') {
                this.tst.tryToClearEnvironment()
            }

            this.steps.stage('Setup consul') {
                this.readSwarmYaml()
                this.runSystem()
            }

            try {
                this.steps.stage('Admin backup test') {
                    this.steps.sh "if [ ! -d ${reports_dir}/admin ]; then mkdir -p ${reports_dir}/admin; fi"
                    this.steps.sh this.makeTestContainerRunScript(reports_dir + "/admin", "npm run consul-die-test")
                }
            } catch (Exception ex) {
                throw ex
            } finally {
                this.steps.stage("Publish report") {
                    this.steps.publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: true, reportDir: 'report', reportFiles: 'mochawesome.html', reportName: "Admin backup Test Running  Report for Build ${env.BUILD_NUMBER}", reportTitles: ''])
                }    
            }

            

            this.currentBuild.result = 'SUCCESS'
        } catch (Exception e) {
            this.steps.echo e.toString()
            this.currentBuild.result = 'FAILED'
        } finally {
            this.steps.stage('Clean environment') {
                this.tst.tryToClearEnvironment()
            }

            this.sendNotification("")
        }
    }
}
