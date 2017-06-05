package com.ericom.jenkins

/**
 * Created by lev on 6/5/17.
 */
class PipeLine implements Serializable {

    def steps
    def currentBuild

    PipeLine(steps, current) {
        this.steps = steps
        this.currentBuild = current
    }



    def run() {
        println "Hello start new library"
    }
}
