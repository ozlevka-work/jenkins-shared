package com.ericom.jenkins.test
@Grab(group='org.yaml', module='snakeyaml', version='1.14')
import org.yaml.snakeyaml.Yaml

class EricomYamlParser implements Serializable {
    def yaml
    def current
    def output

    final CONSUL_SERVICE = "consul"
    final CONSUL_SERVER_SERVICE = "consul-server"

    EricomYamlParser() {
        yaml = new Yaml()
    }

    def loadFile(String path) {
        def file = new File(path)
        def stream = new FileReader(file)
        current = yaml.load(stream)
        output = new LinkedHashMap()
    }

    def makeConsulTestYaml() {
        for(Map.Entry key: current.entrySet()) {
            if(key.getKey() != "services") {
                output.put(key.getKey(), key.getValue())
            }
        }

        return (new Yaml()).dump(output)
    }

}
