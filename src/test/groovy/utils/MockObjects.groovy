package utils

import com.lesfurets.jenkins.unit.BasePipelineTest
import com.lesfurets.jenkins.unit.PipelineTestHelper
import hudson.model.Cause

class MockObjects {
    private final BasePipelineTest basePipelineTest

    MockObjects(basePipelineTest) {
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

                    def tag() {
                        return 'tag'
                    }

                    def push(String label) {
                        return 'push with label ' + label
                    }

                }
                //noinspection UnnecessaryQualifiedReference
                MockObjects.updateInterceptors(customImage, basePipelineTest.helper)
                return customImage
            }

            def inside(String s, Closure c) {
                basePipelineTest.helper.callClosure(c, s)
            }
        }
        //noinspection UnnecessaryQualifiedReference
        MockObjects.updateInterceptors(docker, basePipelineTest.helper)
        return docker
    }

    def getJob() {
        def job = new Script() {
            @Override
            Object run() {
                return 'Kappa123'
            }

            def getCause(type) {
                if (type == Cause.UserIdCause.class) {
                    def userCause = new Script() {
                        @Override
                        Object run() {
                            return null
                        }

                        def getUserId() {
                            return 'user_id'
                        }
                    }
                    //noinspection UnnecessaryQualifiedReference
                    MockObjects.updateInterceptors(userCause, basePipelineTest.helper)
                    return userCause
                }
                return 'cause'
            }
        }
        //noinspection UnnecessaryQualifiedReference
        MockObjects.updateInterceptors(job, basePipelineTest.helper)
        return job
    }

    def getUser() {
        def user = new Script() {
            @Override
            Object run() {
                return null
            }
            def get(id) {
                return this
            }
            def getProperty(type) {
                def property = new Script() {
                    @Override
                    Object run() {
                        return null
                    }
                    def getAddress() {
                        return 'Address'
                    }
                }
                //noinspection UnnecessaryQualifiedReference
                MockObjects.updateInterceptors(property, basePipelineTest.helper)
                return property
            }
        }
        //noinspection UnnecessaryQualifiedReference
        MockObjects.updateInterceptors(user, basePipelineTest.helper)
        return user
    }

    static def updateInterceptors(script, PipelineTestHelper helper) {
        script.metaClass.invokeMethod = helper.getMethodInterceptor()
        script.metaClass.static.invokeMethod = helper.getMethodInterceptor()
        script.metaClass.methodMissing = helper.getMethodMissingInterceptor()
    }
}
