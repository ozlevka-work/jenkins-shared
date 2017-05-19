package org.main.test

/**
 * Created by lev on 5/19/17.
 */
class Test {



    def removeNewLine(String text) {
        if(text == null || text.isEmpty()) {
            return text
        } else {
            return text.replaceAll(/\n/, "<br/>")
        }
    }


    def tryGroovyFeature(String text) {
        def strArr = []
        try{
            for(i in (1..20)) {
                strArr << i.toString() + " Hello \n"
            }
        } catch(Exception e) {
            print e
        }

        return strArr
    }
}
