package com.ericom.jenkins.test

import hudson.AbortException

/**
 * Created by lev on 6/12/17.
 */
class TestFlow {
    def steps
    def config
    def runSystemScript

    TestFlow(steps, config) {
        this.steps = steps
        this.config = config
    }


    def downloadTestFiles() {
        for(int i = 0; i < this.config['test']['swarm']['files'].size(); i++) {
            def file = this.config['test']['swarm']['files'][i]
            def url = this.config['test']['swarm']['repo'] + '/' + file
            this.steps.sh "sudo wget -O ${file} ${url}"
            if(file.endsWith('.sh')) {
                this.runSystemScript = file
                this.steps.sh "sudo chmod +x ${file}"
            }
        }

        this.steps.sh 'ls -al'
    }

    def tryToClearEnvironment() {
        def result
        def stop = false
        while(!stop) {
            try {
                result = this.steps.sh script:'(sudo docker swarm leave -f) 2>&1', returnStdout:true
            } catch (AbortException e) {
                this.steps.echo result
                return
            }

            stop = true
        }

    }

    def testSystemIsUp() {

    }

    def run() {
        this.steps.stage("Clean Environment") {
            this.tryToClearEnvironment()
        }

        this.steps.stage("Prepare Test") {
            this.downloadTestFiles()
        }

        this.steps.stage("Setup system") {
            this.steps.sh script:"sudo ./${this.runSystemScript}"
        }
    }

}
