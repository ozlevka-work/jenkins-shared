@Grab(group = 'org.yaml', module='snakeyaml', version = "1.18")
import com.ericom.jenkins.PipeLine
import org.yaml.snakeyaml.Yaml


/**
 * Created by lev on 5/19/17.
 */




def y = new Yaml();

def obj = y.load(new FileReader("/home/lev/projects/Shield/Dev-Feb16/deploy-shield-new.yml"))


