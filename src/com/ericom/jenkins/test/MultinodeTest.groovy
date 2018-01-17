package com.ericom.jenkins.test

class MultinodeTest {
    def steps
    def config
    def env

    MultinodeTest(steps, config, envinronment) {
        this.steps = steps
        this.config = config
        this.env = envinronment
    }
}
