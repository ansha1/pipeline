package utils

import com.lesfurets.jenkins.unit.BasePipelineTest
import com.nextiva.utils.LogLevel
import org.apache.commons.io.FileUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder

import static com.lesfurets.jenkins.unit.global.lib.LibraryConfiguration.library
import static com.lesfurets.jenkins.unit.global.lib.LocalSource.localSource

trait JenkinsScriptsHelper implements BasePipelineAccessor, Mocks {

    @Rule
    public TemporaryFolder scriptsFolder = new TemporaryFolder()

    void prepareSharedLib() {
        FileUtils.copyDirectory(new File('.'), scriptsFolder.newFolder("pipeline@master"))
        basePipelineTest.helper.registerSharedLibrary(library()
                .name('pipeline')
                .retriever(localSource(scriptsFolder.root.absolutePath))
                .targetPath(scriptsFolder.root.absolutePath)
                .defaultVersion("master")
                .allowOverride(true)
                .implicit(false)
                .build())
    }

    Script loadScriptHelper(String scriptPath) {
        Script script = basePipelineTest.loadScript(scriptPath)
        script.scm = [branches: '', doGenerateSubmoduleConfigurations: '', extensions: '', userRemoteConfigs: '']

        attachScript 'jobWithProperties', 'kubernetes', 'log', 'nexus'

        basePipelineTest.helper.registerAllowedMethod 'waitForQualityGate', [], { [status: 'OK'] }
        basePipelineTest.helper.registerAllowedMethod 'readMavenPom', [Map], { ['version': '1.0.1'] }
        basePipelineTest.helper.registerAllowedMethod "choice", [LinkedHashMap], { c -> 'a' }
        basePipelineTest.helper.registerAllowedMethod "ansiColor", [String, Closure.class], { s, c ->
            Map env = basePipelineTest.binding.getVariable('env')
            env.put('TERM', s)
            basePipelineTest.binding.setVariable('env', env)
            basePipelineTest.helper.callClosure(c, s)
        }

        basePipelineTest.helper.registerAllowedMethod "echo", [String], { println it }
        basePipelineTest.helper.registerAllowedMethod 'readProperties', [Map], { return ["version": "1.0.1"] }

        mockEnv()
        mockDocker()
        mockSlack()

        mockClosure 'pipeline', 'agent', 'tools', 'options', 'stages', 'steps', 'script',
                'when', 'expression', 'parallel', 'post', 'always', 'timestamps'
        mockString 'label', 'jdk', 'maven', 'sh', 'tool', 'ansiColor'
        mockStringStringString 'buildPublishDockerImage', 'buildPublishPypiPackage'
        mockNoArgs 'timestamps', 'nonInheriting'
        mockMap 'authorizationMatrix', 'timeout', 'checkstyle', 'git', 'build', 'slackSend', 'junit',
                'httpRequest', 'booleanParam', 'usernamePassword', 'writeFile'
        mockStringClosure 'dir', 'withSonarQubeEnv', 'lock', 'ansicolor', 'container'
        mockStringStringClosure 'withRegistry'
        mockList 'parameters'
        mockListClosure 'withEnv'
        mockMapClosure 'sshagent', 'withAWS'

        basePipelineTest.helper.registerAllowedMethod "kubernetesSlave", [Map, Object], { m, c ->
            println "Executing kubernetesSlave closure with config: $m"
            basePipelineTest.helper.callClosure(c, m)
        }

        script.env.JOB_LOG_LEVEL = LogLevel.TRACE
//        script.env.JOB_LOG_LEVEL = LogLevel.NONE
//        script.env.JOB_LOG_LEVEL = LogLevel.INFO
//        script.env.JOB_LOG_LEVEL = LogLevel.ERROR

//        basePipelineTest.helper.registerAllowedMethod 'withNamespace', [String, Closure], { s,c -> return c.call()}
//        basePipelineTest.helper.registerAllowedMethod 'withNamespace', [String, Object], { s,c -> return c.call()}
//        basePipelineTest.helper.registerAllowedMethod 'createNamespace', [Object.class], { return it }
//        basePipelineTest.helper.registerAllowedMethod 'deleteNamespace', [Object.class], { return 'true'}
        return script
    }

    @Override
    BasePipelineTest getBasePipelineTest() {
        return this
    }
}
