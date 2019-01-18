package com.nextiva

class SharedJobsStaticVars {
    static final BUILD_PROPERTIES_FILENAME = 'build.properties'
    static final LIST_OF_ENVS = ['dev', 'staging', 'rc', 'production', 'test', 'qa']
    static final LIST_OF_DEFAULT_BRANCH_PATTERNS = ['dev', 'develop', 'hotfix/.+', 'release/.+', 'master']
    static final NEXUS_STATIC_ASSETS_REPO_URL = 'http://repository.nextiva.xyz/repository/static-assets-'
    static final NEXUS_ANDROID_ASSETS_REPO_URL = 'http://repository.nextiva.xyz/repository/android-assets/'
    static final NEXUS_DEB_PKG_REPO_URL = 'http://repository.nextiva.xyz/repository/apt-'
    static final NEXUS_3_REST_API = 'http://repository.nextiva.xyz/service/rest/beta/search?repository='
    static final NEXUS_2_REST_API = 'http://repository.nextiva.xyz:8081/nexus/service/local/artifact/maven/resolve'
    static final ASSETS_PACKAGE_EXTENSION = 'bzip'
    static final VENV_DIR = '.venv'
    static final NEXTIVA_DOCKER_REGISTRY = 'repository.nextiva.xyz'
    static final TENABLE_DOCKER_REGISTRY = 'registry.cloud.tenable.com'
    static final NEXTIVA_DOCKER_REGISTRY_URL = 'https://repository.nextiva.xyz'
    static final TENABLE_DOCKER_REGISTRY_URL = 'https://registry.cloud.tenable.com'
    static final NEXTIVA_DOCKER_REGISTRY_CREDENTIALS_ID = 'nextivaRegistry'
    static final TENABLE_DOCKER_REGISTRY_CREDENTIALS_ID = 'tenableRegistry'
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
    static final JS_NODE_LABEL = 'js'
    static final ANSIBLE_NODE_LABEL = 'ansible'
    static final ANSIBLE_PASSWORD_PATH = '/etc/ansible_password'
    static final KUBERNETES_NODE_LABEL = 'kubernetes'
    static final KUBERNETES_REPO_URL = 'ssh://git@git.nextiva.xyz:7999/rel/k8s-platform.git'
    static final KUBERNETES_REPO_BRANCH = 'develop'
    static final KUBERNETES_KUBELOGIN_VERSION = '1.2.0'
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
    static final SLACK_NOTIFY_COLORS = ['SUCCESS': '#00FF00', 'FAILURE': '#FF0000', 'UNSTABLE': '#FF0000']
    static final SLACK_STATUS_REPORT_CHANNEL_RC = 'rc-platform-support'
    static final DEFAULT_JDK_VERSION = 'Java 8 Install automatically'
    static final DEFAULT_MAVEN_VERSION = 'Maven 3.3.3 Install automatically'
    static final BITBUCKET_URL = 'https://git.nextiva.xyz'
    static final BLUE_GREEN_DEPLOY_DEFAULT = false
    static final PROMETHEUS_PUSHGATEWAY_URL = 'http://10.103.50.110:9091/metrics'
    static final PROMETHEUS_INSTANCE_NAME = 'jenkins'
    static final PROMETHEUS_DEFAULT_METRIC = 1
    static final PROMETHEUS_BUILD_RUNNING_METRIC = 10
    static final PROMETHEUS_BUILD_FINISHED_METRIC = 0
    static final DEFAULT_VERACODE_APPLICATION_SCOPE = 'NextOS Platform (CRM)'
    static final JENKINS_BOT_URL = 'https://jenkins-bot.tooling.nextiva.io'
    static final S3_PRODUCTION_BUCKET_NAME = 'static-assets-prod.nextiva.io'
    static final S3_DEV_BUCKET_NAME = 'static-assets-dev.nextiva.io'
}
