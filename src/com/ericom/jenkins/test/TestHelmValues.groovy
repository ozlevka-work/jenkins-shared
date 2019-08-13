package com.ericom.jenkins.test

import com.ericom.jenkins.HelmInstall
import net.sf.json.JSONObject
import org.apache.tools.ant.util.ReaderInputStream
import org.yaml.snakeyaml.Yaml

class TestHelmValues {
    def helm

    TestHelmValues() {
        def str = """
            { "rootKey": {"es-ldap-api": true, "es-fluent-bit": true}} 
        """
        def params = JSONObject.fromObject(str)
        this.helm = new HelmInstall(null, null, params)
    }

    def loadConfig() {
        def yaml = new Yaml()
        this.helm.config = yaml.load(new FileInputStream(new File("/home/ozlevka/projects/jenkins-shared/resources/com/ericom/defenition/component-defenition.yml")))
    }

    def loadChartValues() {
        def yaml = new Yaml()
        this.helm.chartValues = yaml.load(new FileInputStream(new File("/home/ozlevka/projects/Kube/shield/values.yaml")))
    }

    def runTest() {
        this.loadConfig()
        this.loadChartValues()
        this.helm.updateValues("test1")
        def yaml = new Yaml()
        FileWriter writer = new FileWriter(new File("/home/ozlevka/projects/Kube/shield/values.yaml"))
        yaml.dump(this.helm.chartValues, writer)
    }
}
