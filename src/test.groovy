import com.ericom.jenkins.test.TestHelmValues


def test1() {
    println("Hello test")
}

def testHelm = new TestHelmValues()
testHelm.runTest()
