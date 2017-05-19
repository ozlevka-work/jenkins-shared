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
        (1..20).each {
            strArr << it.toString() + "Hello \n"
        }

        return strArr
    }
}
