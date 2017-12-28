package com.ericom.jenkins.test
@Grab(group='org.yaml', module='snakeyaml', version='1.14')
import org.yaml.snakeyaml.Yaml

class EricomYamlParser implements Serializable {
    def yaml
    def current

    EricomYamlParser() {
        yaml = new Yaml()
    }

    def loadFile(String path) {
        def file = new File(path)
        def stream = new FileReader(file)
        current = yaml.load(stream)
    }


    def getFile() {
        return yaml.dump(current)
    }

}
