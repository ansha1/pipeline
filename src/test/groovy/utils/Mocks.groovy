package utils

/**
 * Common mocks
 */
trait Mocks implements BasePipelineAccessor {
    /**
     * Mocks the required data for sendSlack function
     */
    void mockSendSlack() {
        basePipelineTest.helper.registerAllowedMethod "slackSend", [Map.class], { println 'Slack message mock' }
        basePipelineTest.binding.setVariable 'env', [
                JOB_NAME : 'Job name',
                BUILD_ID : 'Build Id',
                BUILD_URL: 'https://jenkins.nextiva.xyz/jenkins/'
        ]
    }

    /**
     * Attach script to test context
     */
    void attachScript(String... scriptNames) {
        scriptNames.each { String scriptName ->
            def script = basePipelineTest.loadScript "vars/" + scriptName + ".groovy"
            basePipelineTest.binding.setVariable(scriptName, script)
        }
    }

    void mockClosure(String... methodNames) {
        methodNames.each { String methodName ->
            basePipelineTest.helper.registerAllowedMethod(methodName, [Closure], { Closure c ->
                basePipelineTest.helper.callClosure(c)
            })
        }
    }

    void mockStringClosure(String... methodNames) {
        methodNames.each { String methodName ->
            basePipelineTest.helper.registerAllowedMethod(methodName, [String, Closure], { String s, Closure c ->
                basePipelineTest.helper.callClosure(c)
            })
        }
    }

    void mockStringStringClosure(String... methodNames) {
        methodNames.each { String methodName ->
            basePipelineTest.helper.registerAllowedMethod(methodName, [String, String, Closure], { String s1, String s2, Closure c ->
                basePipelineTest.helper.callClosure(c)
            })
        }
    }

    void mockString(String... methodNames) {
        methodNames.each { String methodName ->
            basePipelineTest.helper.registerAllowedMethod(methodName, [String], null)
        }
    }

    void mockStringString(String... methodNames) {
        methodNames.each { String methodName ->
            basePipelineTest.helper.registerAllowedMethod(methodName, [String, String], null)
        }
    }

    void mockNoArgs(String... methodNames) {
        methodNames.each { String methodName ->
            basePipelineTest.helper.registerAllowedMethod(methodName, [], null)
        }
    }

    void mockMap(String... methodNames) {
        methodNames.each { String methodName ->
            basePipelineTest.helper.registerAllowedMethod(methodName, [Map], null)
        }
    }

    void mockList(String... methodNames) {
        methodNames.each { String methodName ->
            basePipelineTest.helper.registerAllowedMethod(methodName, [List], null)
        }
    }

}