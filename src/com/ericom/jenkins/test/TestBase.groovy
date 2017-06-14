package com.ericom.jenkins.test

/**
 * Created by lev on 6/14/17.
 */
class TestBase implements Serializable, Runnable {
    def config

    TestBase(conf) {
        this.config = conf
    }

    private streamToSring(InputStream input) {
        def s = new Scanner(input);
        def builder = new StringBuilder();
        while (s.hasNextLine()) {
            builder.append(s.nextLine() +"\n");
        }
        return builder.toString();
    }

    def runTest() {
        int counter = 1
        int max_retries = this.config['test']['wait']['retries']

        //TO DO test via script
        while (counter <= max_retries) {
            println "Going test system Retry: ${counter}"
            try {
                def proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(this.config['test']['proxy']['address'],this.config['test']['proxy']['port'].toInteger()))
                for (int i = 0; i < this.config['test']['urls'].size(); i++) {
                    def url = new URL(this.config['test']['urls'][i]).openConnection(proxy)
                    def result = streamToSring(url.getInputStream())

                    if(!result.contains('Protected by Ericom Shield')) {
                        throw new Exception(result + ' \n Page is not AccessNow')
                    }
                }
                break
            } catch (Exception e) {
                println e.toString()
            }
            sleep(this.config['test']['wait']['sleep'].toInteger() * 1000)
            counter++
        }

        if(counter > max_retries) {
            this.steps.echo 'Maximum retries exceeded'
            throw new Exception('Maximum retries exceeded')
        }
    }

        void run() {
        try {
            runTest()
        } catch (Exception e) {
            println e
        }
    }
}
