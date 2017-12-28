package com.ericom.jenkins.test
@Grab(group='org.yaml', module='snakeyaml', version='1.14')
import org.yaml.snakeyaml.Yaml

class EricomYamlParser implements Serializable {

    EricomYamlParser(String originalFile) {
        println(originalFile)
    }
}
