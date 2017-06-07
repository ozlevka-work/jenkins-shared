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
        this.steps.stage("Fetch changes") {
            if (this.config["svc"].containsKey('branch')) {
                this.steps.git([url: this.config['svc']['url'], credentialsId: this.config['credentilas']['git'], branch: this.config['svc']['branch'], changelog: true])
            } else {
                this.steps.git([url: this.config['svc']['url'], credentialsId: this.config['credentilas']['git'], changelog: true])
            }

            def changeLogSets = this.currentBuild.rawBuild.changeSets
            this.makeChangeset(changeLogSets)

            this.steps.sh this.changeset.toString()
        }
    }


    def makeChangeset(changeLogSets) {
        for (int i = 0; i < changeLogSets.size(); i++) {
            def entries = changeLogSets[i].items
            for (int j = 0; j < entries.length; j++) {
                def entry = entries[j]
                def files = new ArrayList(entry.affectedFiles)
                for (int k = 0; k < files.size(); k++) {
                    def file = files[k]
                    this.findComponent(file.path)
                }
            }
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
}
