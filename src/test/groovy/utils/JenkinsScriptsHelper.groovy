package utils

import com.lesfurets.jenkins.unit.BasePipelineTest
import org.springframework.security.access.method.P

import static com.nextiva.SharedJobsStaticVars.BUILD_PROPERTIES_FILENAME
import com.nextiva.utils.LogLevel

import static com.lesfurets.jenkins.unit.global.lib.LibraryConfiguration.library
import static com.lesfurets.jenkins.unit.global.lib.LocalSource.localSource

trait JenkinsScriptsHelper implements BasePipelineAccessor, Mocks {

    void prepareSharedLib() {
        def pathPrefix = System.getenv("WORKSPACE")
        def scriptsFolder = (pathPrefix) ? "$pathPrefix/build/classes/groovy/test" : 'build/classes/groovy/test'
        basePipelineTest.helper.registerSharedLibrary(library()
                .name('pipeline')
                .retriever(localSource(scriptsFolder))
                .targetPath(scriptsFolder)
                .defaultVersion("master")
                .allowOverride(true)
                .implicit(false)
                .build())
    }

    Script loadScriptHelper(String scriptPath) {
        Script script = basePipelineTest.loadScript(scriptPath)
        script.scm = [branches: '', doGenerateSubmoduleConfigurations: '', extensions: '', userRemoteConfigs: '']

        basePipelineTest.helper.registerAllowedMethod "choice", [LinkedHashMap], { c -> 'a' }
        basePipelineTest.helper.registerAllowedMethod "ansiColor", [String, Closure.class], { s, c ->
            script.env.TERM = s
            basePipelineTest.helper.callClosure(c, s)
        }
        basePipelineTest.helper.registerAllowedMethod "echo", [String], { println it }

        mockEnv()
        mockDocker()
        mockSlack()

        mockClosure 'pipeline', 'agent', 'stages', 'steps', 'script',
                'parallel', 'post', 'always', 'timestamps'
        mockString 'label', 'jdk', 'maven', 'sh', 'tool', 'ansiColor'
        mockStringStringString 'buildPublishDockerImage', 'buildPublishPypiPackage'
        mockNoArgs 'nonInheriting'
        mockMap 'authorizationMatrix', 'timeout', 'checkstyle', 'git', 'build', 'slackSend', 'junit',
                'httpRequest', 'booleanParam', 'usernamePassword', 'writeFile', 'jiraSendBuildInfo',
                'jiraSendDeploymentInfo'
        mockStringClosure 'dir', 'withSonarQubeEnv', 'lock', 'ansicolor', 'container'
        mockStringStringClosure 'withRegistry'
        mockList 'parameters'
        mockListClosure 'withEnv'
        mockMapClosure 'sshagent', 'withAWS'

        basePipelineTest.helper.registerAllowedMethod "kubernetesSlave", [Map, Object], { m, c ->
            println "Executing kubernetesSlave closure with config: $m"
            basePipelineTest.helper.callClosure(c, m)
        }
        basePipelineTest.helper.registerAllowedMethod 'fileExists', [String], { s ->
            return s == BUILD_PROPERTIES_FILENAME
        }
        basePipelineTest.helper.registerAllowedMethod 'readProperties', [Map], { Map m ->
            if (m.containsKey("file") && m.get("file") == BUILD_PROPERTIES_FILENAME)
                return ["version": "1.0.1"]
            else
                return null
        }


        script.env.JOB_LOG_LEVEL = LogLevel.TRACE
//        basePipelineTest.helper.registerAllowedMethod 'withNamespace', [String, Closure], { s,c -> return c.call()}
//        basePipelineTest.helper.registerAllowedMethod 'withNamespace', [String, Object], { s,c -> return c.call()}
//        basePipelineTest.helper.registerAllowedMethod 'createNamespace', [Object.class], { return it }
//        basePipelineTest.helper.registerAllowedMethod 'deleteNamespace', [Object.class], { return 'true'}
        LinkedHashMap.getMetaClass().getEnvironment { script.env }
        return script
    }

    @Override
    BasePipelineTest getBasePipelineTest() {
        return this
    }
}

