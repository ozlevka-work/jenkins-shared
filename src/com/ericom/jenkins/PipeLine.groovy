package com.ericom.jenkins

import com.ericom.jenkins.test.TestFlow
import hudson.AbortException
@Grab(group = 'org.yaml', module='snakeyaml', version = "1.18")
import org.yaml.snakeyaml.Yaml


/**
 * Created by lev on 6/5/17.
 */
class PipeLine implements Serializable {

    def steps
    def currentBuild
    def config
    def current_tag
    def build_array
    def changeset = [:]
    def containers_names = []
    def env

    PipeLine(steps, current, environment) {
        this.steps = steps
        this.currentBuild = current
        this.env = environment
    }

    def loadConfig(String yml) {
        def yaml = new Yaml()
        this.config = yaml.load(yml)
    }

    def run() {
        try {
            if (fetchChangesCodeChanges()) {
                runUnitTestForChanges()
                makeDockerLogin()
                runRemoteBuild()
                //runBuildChanged()
                def test_flow = new TestFlow(this.steps, this.config, this.env)

                uploadTemporaryImages()
                //test_flow.run_npm_tests()
                test_flow.run_remote_system_test()
                uploadContainersWithTag(null)
                this.currentBuild.result = 'SUCCESS'
            } else {
                this.steps.echo "No changes in code found"
            }
        } catch (Exception e) {
            this.steps.echo e.toString()
            this.currentBuild.result = 'FAILED'
        } finally {
            sendNotification()
        }
    }

    def rebuild(key) {
        this.changeset[key] = true;
        this.steps.stage('Fetch code') {
            if (this.config["svc"].containsKey('branch')) {
                this.steps.git([url: this.config['svc']['url'], credentialsId: this.config['credentials']['git'], branch: this.config['svc']['branch'], changelog: true])
            } else {
                this.steps.git([url: this.config['svc']['url'], credentialsId: this.config['credentials']['git'], changelog: true])
            }
        }

        try {
            runBuildChanged()
            def test_flow = new TestFlow(this.steps, this.config)
            test_flow.run_npm_tests()
            makeDockerLogin()
            uploadContainersWithTag(null)
            this.currentBuild.result = 'SUCCESS'
        } catch (Exception e) {
            this.steps.echo e.toString()
            this.currentBuild.result = 'FAILED'
        } finally {
            sendNotification()
        }
    }


    def sendNotification() {
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
                                <p>List of Containers built and pushed: ${this.containers_names}</p>
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
                                <p style='color:red; font-weight:bold;'> Components should have been in build: ${this.build_array} </p>
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

    def makeDockerLogin() {
        this.steps.withCredentials([[$class          : 'UsernamePasswordMultiBinding', credentialsId: this.config['credentials']['docker'],
                                     usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
            this.steps.stage("Login to docker") {
                this.steps.sh 'docker logout && docker login -u $USERNAME -p $PASSWORD'
            }
        }
    }


    def runTestOnly() {
        def test_flow = new TestFlow(this.steps, this.config, this.env)
        test_flow.run()
    }

    def runNpmTests() {
        def test_flow = new TestFlow(this.steps, this.config, this.env)
        test_flow.run_npm_tests()
    }

    def fetchChangesCodeChanges() {
        this.steps.stage("Fetch changes") {
            if (this.config["svc"].containsKey('branch')) {
                this.steps.git([url: this.config['svc']['url'], credentialsId: this.config['credentials']['git'], branch: this.config['svc']['branch'], changelog: true])
            } else {
                this.steps.git([url: this.config['svc']['url'], credentialsId: this.config['credentials']['git'], changelog: true])
            }

            def changeLogSets = this.currentBuild.rawBuild.changeSets
            for (int i = 0; i < changeLogSets.size(); i++) {
                def entries = changeLogSets[i].items
                for (int j = 0; j < entries.length; j++) {
                    def entry = entries[j]
                    //echo "${entry.commitId} by ${entry.author} on ${new Date(entry.timestamp)}: ${entry.msg}"
                    def files = new ArrayList(entry.affectedFiles)
                    for (int k = 0; k < files.size(); k++) {
                        def file = files[k]
                        //echo "  ${file.editType.name} ${file.path}"
                        this.findComponent(file.path)
                    }
                }
            }
        }

        return this.changeset.size() > 0
    }


    def uploadContainersWithTag(candidate) {
        def tag = candidate
        if(tag == null) {
            tag = (new Date()).format('yyMMdd-HH.mm')
        }
        this.steps.stage('Push Images') {
            for(int i = 0; i < this.build_array.size(); i++) {
                def buildPath = this.config['components'][this.build_array[i]]['path']
                this.steps.sh "cd ${buildPath} && ./_upload.sh ${tag}-${env.BUILD_NUMBER}"
                this.steps.echo "Param ${this.config['components'][this.build_array[i]]} upload success"
                this.containers_names << "${buildPath} by tag ${tag}-${env.BUILD_NUMBER}"
            }

            this.steps.echo "List of build containers: ${this.containers_names}"
        }
    }

    def findComponent(String path) {
        def lst = this.config['components'].keySet() as List
        for(def key in lst) {
            if(path.startsWith(this.config['components'][key]['path'].toString())) {
                this.changeset[key] = true
            }
        }
    }

    def makeDependencies() {
        def needed_to_build = []
        def keys = this.changeset.keySet()

        for(int i = 0; i < keys.size(); i++) {
            def key = keys[i]
            needed_to_build.add(key)
            if(this.config['components'][key].containsKey('dependency')) {
                for(int j = 0; j < this.config['components'][key]['dependency'].size(); j++) {
                    testAlreadyExists(needed_to_build, this.config['components'][key]['dependency'][j])
                    needed_to_build.add(this.config['components'][key]['dependency'][j])
                }
            }
        }

        return needed_to_build
    }


    def testAlreadyExists(collection, item) {
        def indexes = []
        for (int i = 0; i < collection.size(); i++) {
            if (collection[i] == item) {
                indexes.add(i)
            }
        }

        for(int i = 0; i < indexes.size(); i++) {
            collection.remove(indexes[i])
        }
    }

    def runUnitTestForChanges() {
        this.steps.stage("Unit Tests") {
            for(String key: this.changeset.keySet()) {
                this.steps.echo "Run tests for ${key}"
                def buildPath = this.config['components'][key]['path']
                def file_name = "${this.env.PWD}/${buildPath}/_test.sh"
                this.steps.echo "File Path: ${file_name}"
                def file = new File(file_name)
                if(file.exists()) {
                    this.steps.sh "cd ${buildPath} && ./_test.sh"
                } else {
                    this.steps.echo "Skip tests for ${key}"
                }
            }
        }
    }

    def uploadTemporaryImages() {
        this.steps.stage('Upload jenkins images') {
            for(int i = 0; i < this.build_array.size(); i++) {
                def buildPath = this.config['components'][this.build_array[i]]['path']
                this.steps.sh "cd ${buildPath} && ./_upload.sh jenkins"
            }
        }
    }

    def runBuildChanged() {
        this.steps.stage('Build Images') {
            this.build_array = makeDependencies()
            for(int i = 0; i < this.build_array.size(); i++) {
                def buildPath = this.config['components'][this.build_array[i]]['path']
                this.steps.sh "cd ${buildPath} && ./_build.sh"
                this.steps.echo "Component ${this.build_array[i]} succesfully build"
            }
        }
    }

    def runRemoteBuild() {
        this.steps.stage('Remote Build Process') {
            this.build_array = makeDependencies()
            def build_path_array = []
            for(int i = 0; i < this.build_array.size(); i++) {
                build_path_array.add(this.config['components'][this.build_array[i]]['path'])
            }

            this.steps.echo "Going run ${this.config['ansible']['build_playbook']} for paths ${build_path_array}"

            this.steps.withCredentials([[$class          : 'UsernamePasswordMultiBinding', credentialsId: this.config['credentials']['docker'],
                                         usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                this.steps.ansiblePlaybook(
                        playbook: this.config['ansible']['build_playbook'],
                        dynamicInventory: true,
                        extraVars: [
                                build_path     : build_path_array,
                                docker_user    : this.env.USERNAME,
                                docker_password: this.env.PASSWORD
                        ]
                )
            }
        }
    }
}
