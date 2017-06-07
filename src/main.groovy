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

    return pl
}


def testConfig() {
    def pl = testLoadConfig()

    assert pl.config['credentilas']['git'] == 'ozlevka-github'
}

def testArray = [
        "Containers/Docker/shield-icap/web/AccessNow/resources/lang/dictionary.ko-kr.txt.gz",
        "Containers/Docker/shield-icap/web/AccessNow/src/accessnow.min.js.gz",
        "Containers/Docker/shield-icap/web/AccessNow/resources/lang/dictionary.ja-jp.txt.gz",
        "Containers/Docker/shield-icap/web/AccessNow/fonts/AvenirNextLTPro-Bold.ttf.gz",
        "Containers/Docker/shield-icap/web/AccessNow/resources/lang/dictionary.de.txt.gz",
        "Containers/Docker/shield-icap/web/AccessNow/resources/lang/dictionary.zh-cn.txt.gz",
        "Containers/Docker/shield-icap/web/AccessNow/resources/lang/dictionary.fr.txt.gz",
        "Containers/Docker/shield-icap/web/AccessNow/fonts/FontAwesome.otf.gz",
        "Containers/Docker/shield-icap/web/AccessNow/resources/lang/dictionary.ru.txt.gz",
        "Containers/Docker/shield-icap/web/AccessNow/resources/lang/dictionary.zh-tw.txt.gz",
        "Containers/Docker/shield-icap/web/AccessNow/fonts/AvenirNextLTPro-Bold.woff.gz",
        "Containers/Docker/shield-icap/web/AccessNow/resources/lang/dictionary.en-us.txt.gz",
        "Containers/Docker/shield-icap/web/AccessNow/fonts/fontawesome-webfont.eot.gz",
        "Containers/Docker/shield-icap/web/AccessNow/resources/lang/dictionary.it.txt.gz",
        "Containers/Docker/shield-icap/web/AccessNow/resources/lang/dictionary.list.txt.gz",
        "AccessNow/css/shield.css",
        "Containers/Docker/shield-icap/web/AccessNow/css/accessnow.min.css",
        "Containers/Docker/shield-icap/web/AccessNow/fonts/AvenirNextLTPro-Bold.svg.gz",
        "Containers/Docker/shield-icap/web/AccessNow/fonts/fontawesome-webfont.svg.gz",
        "Containers/Docker/shield-icap/web/AccessNow/fonts/AvenirNextLTPro-Bold.eot.gz",
        "Containers/Docker/shield-icap/web/AccessNow/resources/lang/dictionary.pt-br.txt.gz",
        "Containers/Docker/shield-icap/web/AccessNow/css/accessnow.min.css.gz",
        "Containers/Docker/shield-icap/web/AccessNow/fonts/fontawesome-webfont.woff2.gz",
        "Containers/Docker/shield-icap/web/AccessNow/fonts/fontawesome-webfont.ttf.gz",
        "Containers/Docker/shield-icap/web/AccessNow/resources/lang/dictionary.es-ar.txt.gz",
        "Containers/Docker/shield-icap/web/AccessNow/src/vendor.min.js.gz",
        "Containers/Docker/shield-icap/web/AccessNow/fonts/fontawesome-webfont.woff.gz",
        "Containers/Docker/shield-icap/web/AccessNow/resources/lang/12Dec2012ANChineseSimplified.lang.gz",
        "Containers/Docker/shield-icap/web/AccessNow/fonts/AvenirNextLTPro-Bold.min.svg.gz",
        "Containers/Docker/shield-configuration/Dockerfile"
]


def testContainsKey() {
    def config = testLoadConfig()

    assert !config.config['svc'].containsKey('branch')
}


def testChangesFinding() {

}



testConfig()

testContainsKey()


