package utils

import org.codehaus.groovy.reflection.CachedMethod

/**
 * Common mocks
 */
trait Mocks implements BasePipelineAccessor {
    MocksAdjustment mocksAdjustment = new MocksAdjustment(basePipelineTest)

    /**
     * Mocks Jenkins common property
     */
    void mockCommon() {
        basePipelineTest.binding.setVariable 'params', [
                DEBUG: 'true'
        ]
    
        basePipelineTest.binding.setVariable 'env', [
                JOB_NAME   : 'Job name',
                BUILD_ID   : 'Build Id',
                BUILD_URL  : 'https://jenkins.nextiva.xyz/jenkins/',
                BRANCH_NAME: 'dev',
                NODE_NAME  : 'Debian Slave 3',
                DEBUG      : 'true'
        ]
    }

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
     * Mocks the required data for generateBuildProperties script
     */
    void mockGenerateBuildProperties() {
        basePipelineTest.helper.registerAllowedMethod 'readProperties', [Map], { c ->
            [
                    deploy_environment : 'deploy_environment_from_file',
                    some_property      : 'Kappa123',
                    some_other_property: 'Keepo'
            ]
        }
        basePipelineTest.helper.registerAllowedMethod "sh", [Map], { c -> "sh command output" }
        basePipelineTest.helper.registerAllowedMethod "writeFile", [Map], { c -> "Write file" }

    }

    /**
     * Mocks methods available through the docker variable
     */
    void mockDocker() {
        basePipelineTest.binding.setVariable('docker', mocksAdjustment.getDocker())
    }

    /**
     * Attach script to test context
     */
    void attachScript(String... scriptNames) {
        scriptNames.each { String scriptName ->
            def script = basePipelineTest.loadScript "vars/" + scriptName + ".groovy"
            basePipelineTest.binding.setVariable(scriptName, script)
            script.metaClass.methods.findAll { m -> m.getName() == 'call' }.each { m ->
                basePipelineTest.helper.registerAllowedMethod(
                        scriptName,
                        ((CachedMethod) m).cachedMethod.parameterTypes.findAll(),
                        { Object[] args -> script.call(*args) })
            }
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