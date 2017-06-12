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
                runBuildChanged()
                def test_flow = new TestFlow(this.steps, this.config)
                test_flow.run()
                makeDockerLogin()
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


    def  sendNotification() {
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
        this.steps.withCredentials([[$class          : 'UsernamePasswordMultiBinding', credentialsId: this.config['credentilas']['docker'],
                                     usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
            this.steps.stage("Login to docker") {
                this.steps.sh 'docker logout && docker login -u $USERNAME -p $PASSWORD'
            }
        }
    }


    def runTestOnly() {
        def test_flow = new TestFlow(this.steps, this.config)
        test_flow.run()
    }

    def fetchChangesCodeChanges() {
        this.steps.stage("Fetch changes") {
            if (this.config["svc"].containsKey('branch')) {
                this.steps.git([url: this.config['svc']['url'], credentialsId: this.config['credentilas']['git'], branch: this.config['svc']['branch'], changelog: true])
            } else {
                this.steps.git([url: this.config['svc']['url'], credentialsId: this.config['credentilas']['git'], changelog: true])
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
            tag = startDate.format('yyMMdd-HH.mm')
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


//    def makeChangeset(changeLogSets) {
//        for (int i = 0; i < changeLogSets.size(); i++) {
//            def entries = changeLogSets[i].items
//            for (int j = 0; j < entries.length; j++) {
//                def entry = entries[j]
//                def files = new ArrayList(entry.affectedFiles)
//                for (int k = 0; k < files.size(); k++) {
//                    def file = files[k]
//                    this.findComponent(file.path)
//                }
//            }
//        }
//    }

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
}
