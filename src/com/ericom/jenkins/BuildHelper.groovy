package com.ericom.jenkins

/**
 * Created by lev on 5/20/17.
 */
class BuildHelper implements Serializable {
    def steps
    def current
    BuildHelper(steps, current) {
        this.steps = steps
        this.current = current
    }

    def readExecutedShell() {
        def proc = "ls -al".execute()
        def out = new StringBuilder()
        def err = new StringBuilder()

        proc.consumeProcessOutput(out, err)

        return [out.toString(), err.toString()]
    }


    def testMyTest() {
        steps.stage('Hello world') {
            steps.sh 'echo "Hello wordissiomo"'
        }

        steps.stage('Hello end of world') {
            steps.sh 'echo $JAVA_HOME'
        }

        steps.stage('Test open') {
            steps.echo steps.current.toString()
        }
    }
}
