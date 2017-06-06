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

    PipeLine(steps, current) {
        this.steps = steps
        this.currentBuild = current
    }

    def loadConfig(String yml) {
        def yaml = new Yaml()
        this.config = yaml.load(yml)
    }

    def run() {

    }
}
