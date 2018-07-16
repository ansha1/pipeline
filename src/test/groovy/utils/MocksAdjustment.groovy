package utils

import com.lesfurets.jenkins.unit.BasePipelineTest

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
                customImage.metaClass.invokeMethod = basePipelineTest.helper.getMethodInterceptor()
                customImage.metaClass.static.invokeMethod = basePipelineTest.helper.getMethodInterceptor()
                customImage.metaClass.methodMissing = basePipelineTest.helper.getMethodMissingInterceptor()
                return customImage
            }
        }
        docker.metaClass.invokeMethod = basePipelineTest.helper.getMethodInterceptor()
        docker.metaClass.static.invokeMethod = basePipelineTest.helper.getMethodInterceptor()
        docker.metaClass.methodMissing = basePipelineTest.helper.getMethodMissingInterceptor()
        return docker
    }

}
