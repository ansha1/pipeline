package utils

import com.lesfurets.jenkins.unit.BasePipelineTest
import com.lesfurets.jenkins.unit.PipelineTestHelper

class MocksAdjustment {
    private final BasePipelineTest basePipelineTest

    MocksAdjustment(basePipelineTest) {
        this.basePipelineTest = basePipelineTest
    }

    def getDocker() {
        def docker = new Script() {
            @Override
            Object run() {
                return null
            }

            def withRegistry(String s1, String s2, Closure closure) {
                basePipelineTest.helper.callClosure(closure, s1, s2)
                return 'Kappa123'
            }

            def build(String imageName, String directoty = '.') {
                def customImage = new Script() {
                    def id = 'Image_Id'

                    @Override
                    Object run() {
                        return null
                    }

                    def push() {
                        return 'push'
                    }

                    def push(String label) {
                        return 'push with label ' + label
                    }

                }
                //noinspection UnnecessaryQualifiedReference
                MocksAdjustment.updateInterceptors(customImage, basePipelineTest.helper)
                return customImage
            }

            def inside(String s, Closure c) {
                basePipelineTest.helper.callClosure(c, s)
            }
        }
        //noinspection UnnecessaryQualifiedReference
        MocksAdjustment.updateInterceptors(docker, basePipelineTest.helper)
        return docker
    }

    static def updateInterceptors(script, PipelineTestHelper helper) {
        script.metaClass.invokeMethod = helper.getMethodInterceptor()
        script.metaClass.static.invokeMethod = helper.getMethodInterceptor()
        script.metaClass.methodMissing = helper.getMethodMissingInterceptor()
    }
}
