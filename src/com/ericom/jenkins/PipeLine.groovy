package com.ericom.jenkins
@Grab(group = 'org.yaml', module='snakeyaml', version = "1.18")
import org.yaml.snakeyaml.Yaml


/**
 * Created by lev on 6/5/17.
 */
class PipeLine implements Serializable {

    def steps
    def currentBuild
    def config

    def changeset = [:]

    PipeLine(steps, current) {
        this.steps = steps
        this.currentBuild = current
    }

    def loadConfig(String yml) {
        def yaml = new Yaml()
        this.config = yaml.load(yml)
    }

    def run() {
        if (fetchChangesCodeChanges()) {
            runBuildChanged()
        } else {
            this.steps.echo "No changes in code found"
        }
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
                    needed_to_build.add(this.config['components'][key]['dependency'][j])
                }
            }
        }

        return needed_to_build
    }


    def runBuildChanged() {
        this.steps.stage('Build Images') {
            def build_array = makeDependencies()
            for(i = 0; i < build_array.size(); i++) {
                def buildPath = this.config['components'][build_array[i]]['path']
                this.steps.sh "cd ${buildPath} && sudo ./_build.sh"
                this.steps.echo "Component ${build_array[i]} succesfully build"
            }
        }
    }
}
