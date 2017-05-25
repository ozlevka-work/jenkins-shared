package com.ericom.jenkins

/**
 * Created by lev on 5/20/17.
 */
class BuildHelper implements Serializable {
    def steps

    BuildHelper(steps) {
        this.steps = steps
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
    }
}
