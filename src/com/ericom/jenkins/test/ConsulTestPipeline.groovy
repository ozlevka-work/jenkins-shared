package com.ericom.jenkins.test

import com.ericom.jenkins.PipelineBase

class ConsulTestPipeline extends PipelineBase{

    ConsulTestPipeline(steps, current, environment) {
        super(steps, current, environment)
    }


    def helloWorld() {
        this.steps.echo "Hello World"
    }
}
