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

    def sendNotification(body_data) {
        this.steps.stage('Send notifications') {
            def result = this.currentBuild.result
            if (result == null) {
                this.steps.echo "No changes found"
            } else {

                if(result == "SUCCESS") {
                    this.steps.emailext(
                            to: this.config['notification']['mails'].join(","),
                            subject: "SUCCESS: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
                            body: """<p>SUCCESS: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
                                ${body_data}
                                <p>Check console output at &QUOT;<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${
                                env.BUILD_NUMBER
                            }]</a>&QUOT;</p> """//,
                            //recipientProviders: [[$class: 'RequesterRecipientProvider']]
                    )
                } else {
                    this.steps.emailext(
                            to: this.config['notification']['mails'].join(","),
                            subject: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
                            body: """<p>FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
                                ${body_data}
                                <p>Errors: See attached log</p>
                                <p>Check console output at &QUOT;<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER
                            }]</a>&QUOT;</p>""",
                            attachLog: true,
                            compressLog: true
                    )
                }

            }
        }
    }
}