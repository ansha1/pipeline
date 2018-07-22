package com.nextiva

class SharedJobsStaticVars {
    static final BUILD_PROPERTIES_FILENAME = 'build.properties'
    static final LIST_OF_ENVS = ['dev', 'staging', 'rc', 'production', 'test', 'qa']
    static final LIST_OF_DEFAULT_BRANCH_PATTERNS = ['dev', 'develop', 'hotfix/.+', 'release/.+', 'master']
    static final NEXUS_STATIC_ASSETS_REPO_URL = 'http://repository.nextiva.xyz/repository/static-assets-'
    static final NEXUS_DEB_PKG_REPO_URL = 'http://repository.nextiva.xyz/repository/apt-'
    static final ASSETS_PACKAGE_EXTENSION = 'bzip'
    static final VENV_DIR = '.env'
    static final DOCKER_REGISTRY = 'repository.nextiva.xyz'
    static final DOCKER_REGISTRY_URL = 'https://repository.nextiva.xyz'
    static final DOCKER_REGISTRY_CREDENTIALS_ID = 'nextivaRegistry'
    static final PIP_TRUSTED_HOST = 'repository.nextiva.xyz'
    static final PIP_EXTRA_INDEX_URL = 'http://repository.nextiva.xyz/repository/pypi-'
    static final PIP_EXTRA_INDEX_URL_SUFFIX = '-group/simple'
    static final PIP_EXTRA_INDEX_DEFAULT_REPO = 'dev'
    static final DEB_PKG_CONTENT_TYPE_PUBLISH = 'Content-Type:multipart/form-data'
    static final JENKINS_AUTH_CREDENTIALS = 'jenkinsbitbucket'
    static final SHARED_LIBRARY_REPO_DIR = '/opt/shared_repos'
    static final RELEASE_MANAGEMENT_REPO_URL = 'ssh://git@git.nextiva.xyz:7999/rel/release-management.git'
    static final RELEASE_MANAGEMENT_REPO_BRANCH = 'dev'
    static final GIT_CHECKOUT_CREDENTIALS = 'jenkins-in-bitbucket'
    static final BITBUCKET_JENKINS_AUTH = 'jenkins-user-in-bitbucket'
    static final DEFAULT_NODE_LABEL = 'debian'
    static final ANSIBLE_NODE_LABEL = 'ansible'
    static final JS_NODE_LABEL = 'js'
    static final KUBERNETES_NODE_LABEL = 'kubernetes'
    static final ANSIBLE_PASSWORD_PATH = '/etc/ansible_password'
    static final KUBERNETES_REPO_URL = 'ssh://git@git.nextiva.xyz:7999/rel/k8s-platform.git'
    static final KUBERNETES_REPO_BRANCH = 'develop'
    static final SONAR_QUBE_SCANNER = 'SonarQube Scanner'
    static final SONAR_QUBE_ENV = 'SonarQube'
    static final JOB_TIMEOUT_MINUTES_DEFAULT = 30
    static final BUILD_NUM_TO_KEEP_STR = '7'
    static final ARTIFACT_NUM_TO_KEEP_STR = '7'
    static final RC_JOB_LOCK_MESSAGE = """The Deployment to RC environment is locked.\n
            Please contact the primary approver in Kyiv: Sergey Podgorov <sergey.podgorov@nextiva.com>, second-level approvers: Kirill Osadchyy <kirill.osadchyy@nextiva.com> & Ivan Maksymiv <ivan.maksymiv@nextiva.com>\n
            OR the primary approver in Arizona: Keerthi Suraparaju <keerthi.suraparaju@nextiva.com>, second-level approver Ebrahim Feyzi <ebrahim.feyzi@nextiva.com>  with unlocking request for RC environment.\n
            If you can't reach out anyone from the defined list, you can join slack channel #rcjobslock and make a request there
            """
    static final NEWRELIC_API_KEY_MAP = ['production': '83904bdc782deef7783afde4a50348538b758577a208889']
    //Slack app bot token for getting slack userID over email
    static final SLACK_BOT_TOKEN = 'xoxb-17176588338-387685736801-YQyMyph5fBi64WFw9rRGTgIl'
    static final DEFAULT_JDK_VERSION = 'Java 8 Install automatically'
    static final DEFAULT_MAVEN_VERSION = 'Maven 3.3.3 Install automatically'
    static final BITBUCKET_ADDRESS = 'http://git.nextiva.xyz'
}
