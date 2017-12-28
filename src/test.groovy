import com.ericom.jenkins.test.EricomYamlParser


def test1() {
    println("Hello test")
}

def yaml = new EricomYamlParser()

yaml.loadFile('/home/ozlevka/projects/Shield/Setup/docker-compose_dev.yml')

println(yaml.makeConsulTestYaml())
