package com.ericom.jenkins

/**
 * Created by lev on 5/20/17.
 */
class BuildHelper {
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
        echo this.steps.getClass().getName()
    }
}
