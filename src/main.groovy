@Grab(group = 'org.yaml', module='snakeyaml', version = "1.18")
import com.ericom.jenkins.PipeLine
import org.yaml.snakeyaml.Yaml


/**
 * Created by lev on 5/19/17.
 */


def testLoadConfig() {
    def path = 'resources/com/ericom/defenition/component-defenition.yml'
    def cofStr = (new File(path)).text
    def pl = new PipeLine(null, null)

    pl.loadConfig(cofStr)
    println pl.config
}



testLoadConfig()


