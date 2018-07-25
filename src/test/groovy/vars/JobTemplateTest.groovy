package vars

import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import utils.Mocks
import utils.Validator

class JobTemplateTest extends BasePipelineTest implements Mocks, Validator {
    static final def JENKINS_FILE_JAVA = {
        APP_NAME = 'java-app'
        DEPLOY_ON_K8S = 'false'
        BASIC_INVENTORY_PATH = 'ansible/role-based_playbooks/inventory/java-app'
        PLAYBOOK_PATH = 'ansible/role-based_playbooks/java-app.yml'
        CHANNEL_TO_NOTIFY = 'java-app'
        ansibleEnvMap = [dev       : "dev",
                         qa        : "rc",
                         production: "production"]
        projectFlow = ['language': 'java']
        healthCheckMap = [dev       : ["http://0.0.0.0:8080/health"],
                          qa        : ["http://0.0.0.0:8080/health"],
                          production: ["http://0.0.0.0:8080/health"]]
        branchPermissionsMap = [dev       : ["authenticated"],
                                qa        : ["first_user", "second_user"],
                                production: ["first_user", "second_user"]]
    }

    static final def JENKINS_FILE_JS = {
        APP_NAME = 'js-app'
        BASIC_INVENTORY_PATH = 'ansible/role-based_playbooks/inventory/static-deploy/'
        PLAYBOOK_PATH = 'ansible/role-based_playbooks/static-deploy.yml'
        CHANNEL_TO_NOTIFY = 'js-app'
        ansibleEnvMap = [dev       : "dev",
                         qa        : "rc",
                         production: "production"]
        projectFlow = ['language'    : 'js',
                       'testCommands': '''
                                   npm install
                                   npm run lint
                                   npm run test:coverage
                                   ''']
        healthCheckMap = [dev       : ["http://dev.dev.nextiva.xyz/apps/js-app/build.properties"],
                          qa        : ["http://rc.rc.nextiva.xyz/apps/js-app/build.properties"],
                          production: ["http://nextiva.nextos.com/apps/js-app/build.properties"]]
        branchPermissionsMap = [dev       : ["authenticated"],
                                qa        : ["first_user", "second_user"],
                                production: ["first_user", "second_user"]]
    }

    static final def JENKINS_FILE_PYTHON = {
        APP_NAME = 'nextiva-python-app'
        DEPLOY_ON_K8S = 'false'
        BASIC_INVENTORY_PATH = 'ansible/role-based_playbooks/inventory/nextiva-python-app/'
        PLAYBOOK_PATH = 'ansible/role-based_playbooks/nextiva-python-app-deploy.yml'
        CHANNEL_TO_NOTIFY = 'nextiva-python-app'
        ansibleEnvMap = [dev       : "dev",
                         qa        : "rc",
                         production: "production"]
        projectFlow = ['language'       : 'python',
                       'languageVersion': 'python3.6']
        healthCheckMap = [dev       : ["http://0.0.0.0:8080/health"],
                          qa        : ["http://0.0.0.0:8080/health"],
                          production: ["http://0.0.0.0:8080/health"]]
        branchPermissionsMap = [dev       : ["authenticated"],
                                qa        : ["first_user", "second_user"],
                                production: ["first_user", "second_user"]]
    }

    @Override
    @Before
    void setUp() throws Exception {
        scriptRoots += '/'
        super.setUp()
        binding.setVariable 'NODE_NAME', 'Debian Slave 3'
        binding.setVariable 'BRANCH_NAME', 'dev'
        binding.setVariable 'GIT_URL', 'ssh://git@git.nextiva.xyz:7999/~oleksandr.kramarenko/qa_integration.git'
        binding.setVariable 'params', [
                deploy_version: '1.0',
                stack: 'a'
        ]
        attachScript 'jobConfig', 'kubernetes', 'prepareRepoDir', 'runAnsiblePlaybook', 'prepareRepoDir',
                'runAnsiblePlaybook', 'isRCLocked', 'healthCheck', 'slack', 'log'

        helper.registerAllowedMethod 'getUtils', [String, String], { loadScript('src/com/nextiva/JavaUtils.groovy') }
        helper.registerAllowedMethod 'waitForQualityGate', [], { [status: 'OK'] }
        helper.registerAllowedMethod 'sh', [Map], { c -> 'some output' }
        helper.registerAllowedMethod "choice", [LinkedHashMap], { c -> 'a' }

        mockEnv()
        mockDocker()

        mockClosure 'pipeline', 'agent', 'tools', 'options', 'parameters', 'stages', 'steps', 'script',
                'when', 'expression', 'parallel', 'post', 'always'
        mockString 'label', 'jdk', 'maven', 'sh', 'tool', 'ansiColor'
        mockStringString 'buildPublishDockerImage'
        mockNoArgs 'timestamps', 'nonInheriting'
        mockMap 'authorizationMatrix', 'timeout', 'checkstyle', 'git', 'build', 'slackSend', 'junit'
        mockStringClosure 'dir', 'withSonarQubeEnv', 'lock'
        mockStringStringClosure 'withRegistry'
    }

    @Override
    BasePipelineTest getBasePipelineTest() {
        return this
    }

    private void executeJobTemplate(def jenkinsFile) {
        def script = loadScript "vars/jobTemplate.groovy"
        script.call jenkinsFile
        printCallStack()
    }

    @Test
    void execute_pipeline_java() {
        executeJobTemplate JENKINS_FILE_JAVA
    }

    @Test
    void execute_pipeline_js() {
        executeJobTemplate JENKINS_FILE_JS
    }

    @Test
    void execute_pipeline_python() {
        executeJobTemplate JENKINS_FILE_PYTHON
    }

    @Test
    void execute_with_qg_ok() {
        helper.registerAllowedMethod 'waitForQualityGate', [], { [status: 'OK'] }
        executeJobTemplate JENKINS_FILE_JAVA
        checkThatMethodWasExecutedWithValue('print', 'Sonar Quality Gate failed', 0)
    }

    //Ignored due to the commented code
    @Ignore
    @Test
    void execute_with_qg_failed() {
        helper.registerAllowedMethod 'waitForQualityGate', [], { [status: 'FAILED'] }
        executeJobTemplate JENKINS_FILE_JAVA
        checkThatMethodWasExecutedWithValue('print', 'Sonar Quality Gate failed', 1)
    }


}
