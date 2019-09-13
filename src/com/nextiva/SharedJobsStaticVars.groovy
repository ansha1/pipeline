package com.nextiva

class SharedJobsStaticVars {
    static final BUILD_PROPERTIES_FILENAME = 'build.properties'
    static final LIST_OF_ENVS = ['dev', 'staging', 'rc', 'production', 'test', 'qa', 'tooling', 'sales-demo']
    static final LIST_OF_DEFAULT_BRANCH_PATTERNS = ['dev', 'develop', 'hotfix/.+', 'release/.+', 'master']
    static final LIST_OF_BOOKED_NAMESPACES = ['bot', 'default', 'jenkins', 'kube-public', 'kube-system', 'monitoring', 'selenoid-moon', 'utils']
    static final NEXUS_STATIC_ASSETS_REPO_URL = 'http://repository.nextiva.xyz/repository/static-assets-'
    static final NEXUS_ANDROID_ASSETS_REPO_URL = 'http://repository.nextiva.xyz/repository/android-assets/'
    static final NEXUS_DEB_PKG_REPO_URL = 'http://repository.nextiva.xyz/repository/apt-'
    static final NEXUS_3_REST_API = 'https://nexus.nextiva.xyz/service/rest/v1/search?repository='
    static final NEXUS_3_LEGACY_REST_API = 'http://repository.nextiva.xyz/service/rest/beta/search?repository='
    static final NEXUS_2_REST_API = 'http://repository.nextiva.xyz:8081/nexus/service/local/artifact/maven/resolve'
    static final ASSETS_PACKAGE_EXTENSION = 'bzip'
    static final VENV_DIR = '.venv'
    static final NEXTIVA_DOCKER_REGISTRY = 'docker.nextiva.xyz'
    static final TENABLE_DOCKER_REGISTRY = 'registry.cloud.tenable.com'
    static final NEXTIVA_DOCKER_TEST_REGISTRY = 'nexus.nextiva.xyz'
    static final NEXTIVA_DOCKER_REGISTRY_URL = 'https://docker.nextiva.xyz'
    static final TENABLE_DOCKER_REGISTRY_URL = 'https://registry.cloud.tenable.com'
    static final NEXTIVA_DOCKER_TEST_REGISTRY_URL = 'https://nexus.nextiva.xyz/repository/docker-hosted-nextiva-test'
    static final NEXTIVA_DOCKER_REGISTRY_CREDENTIALS_ID = 'nextivaRegistry'
    static final TENABLE_DOCKER_REGISTRY_CREDENTIALS_ID = 'tenableRegistry'
    static final PIP_EXTRA_INDEX_URL = 'https://nexus.nextiva.xyz/repository/pypi-'
    static final PIP_EXTRA_INDEX_URL_SUFFIX = '/simple'
    static final PIP_EXTRA_INDEX_DEFAULT_REPO = 'dev'
    static final DEB_PKG_CONTENT_TYPE_PUBLISH = 'Content-Type:multipart/form-data'
    static final JENKINS_AUTH_CREDENTIALS = 'jenkinsbitbucket'
    static final SHARED_LIBRARY_REPO_DIR = '/opt/shared_repos'
    static final RELEASE_MANAGEMENT_REPO_URL = 'ssh://git@git.nextiva.xyz:7999/rel/release-management.git'
    static final RELEASE_MANAGEMENT_REPO_BRANCH = 'dev'
    static final GIT_CHECKOUT_CREDENTIALS = 'jenkins-in-bitbucket'
    static final BITBUCKET_JENKINS_AUTH = 'jenkins-user-in-bitbucket'
    static final DEFAULT_NODE_LABEL = 'debian'
    static final JS_NODE_LABEL = 'slave_node8'
    static final ANSIBLE_NODE_LABEL = 'ansible'
    static final ANSIBLE_PASSWORD_PATH = '/etc/ansible_password'
    static final KUBERNETES_REPO_BRANCH = 'master'
    static final KUBERNETES_NODE_LABEL = 'kubernetes'
    static final KUBERNETES_REPO_URL = 'ssh://git@git.nextiva.xyz:7999/cloud/cloud-apps.git'
    static final KUBERNETES_KUBELOGIN_DEFAULT_VERSION = '1.4.0'
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
    //Slack app bot token for getting slack userID over email
    static final SLACK_BOT_TOKEN = 'xoxb-17176588338-387685736801-YQyMyph5fBi64WFw9rRGTgIl'
    static final SLACK_NOTIFY_COLORS = ['SUCCESS': '#00FF00', 'FAILURE': '#FF0000', 'UNSTABLE': '#FF0000']
    static final SLACK_STATUS_REPORT_CHANNEL_QA = 'rc-platform-support'
    static final SLACK_STATUS_REPORT_CHANNEL_PROD = 'platform-production'
    static final DEFAULT_SLACK_CHANNEL = 'testchannel'
    static final SLACK_URL = 'https://nextivalab.slack.com'
    static final DEFAULT_JDK_VERSION = 'Java 8 Install automatically'
    static final DEFAULT_MAVEN_VERSION = 'Maven 3.3.3 Install automatically'
    static final BITBUCKET_URL = 'https://git.nextiva.xyz'
    static final PROMETHEUS_PUSHGATEWAY_URL = 'http://10.103.50.110:9091/metrics'
    static final PROMETHEUS_INSTANCE_NAME = 'jenkins'
    static final PROMETHEUS_DEFAULT_METRIC = 1
    static final PROMETHEUS_BUILD_RUNNING_METRIC = 10
    static final PROMETHEUS_BUILD_FINISHED_METRIC = 0
    static final DEFAULT_VERACODE_APPLICATION_SCOPE = 'Nextiva Services'
    static final S3_PUBLIC_BUCKET_NAME = 'public-static-assets.nextiva.io'
    static final S3_PRIVATE_BUCKET_NAME = 'private-static-assets.nextiva.io'
    static final AWS_REGION = 'us-west-2'
    static final AWS_CREDENTIALS = 'nextiva.io'
    static final AWS_S3_UPLOAD_CREDENTIALS = 'test-reports-s3-upload'
    static final S3_TEST_REPORTS_BUCKET = 'test-reports.nextiva.io'
    static final TEST_REPORTS_URL = 'https://test-reports.tooling.nextiva.io'
    static final BITBUCKET_SECTION_MARKER = '###### '
    static final DEFAULT_INVENTORY_DIRECTORY_SALES_DEMO = 'sales-demo'
    static final DEFAULT_KUBERNETES_CLUSTER_SALES_DEMO = 'sales-demo.nextiva.io'
    static final PUBLISH_STATIC_ASSETS_TO_S3_DEFAULT = true
    static final PUBLIC_STATIC_ASSETS_ADDRESS = 'public-static.nextos.com'
    static final VAULT_URL = "https://vault.tooling.nextiva.io"
    static final KUBEUP_VERSION = '1.0.0'
    static final KUBEDOG_VERSION = '0.3.3'
    static final VAULT_CLIENT_VERSION = '1.1.0'
    static final KUBEDOG_SHA256 = '203d520790c711f8da85a8edfd6e71b2db52928774fc36a642d081c5e467bc2b'
    static final VAULT_CLIENT_SHA256 = '65d665ee7ba08fb41a7113a2ae3c1d5fd7e0b530b59644ed7dc8a01870b2d73f'

    /*
     Enable sending interactive Slack message through Jenkins Bot.
     If Jenkins Bot doesn't work then we can disable it and switch back to the non-interactive notification.
     */
    static final JENKINS_BOT_ENABLE = false
    static final JENKINS_BOT_URL = 'https://jenkins-bot.tooling.nextiva.io'
    static final JENKINS_KUBERNETES_CLUSTER_DOMAIN = "tooling.nextiva.io"

    static final Map DEFAULT_TOOL_CONFIGURATION = ["jnlp"   : ["image"                : "jenkins/jnlp-slave:3.29-1-alpine",
                                                               "command"              : "/usr/local/bin/jenkins-slave",
                                                               "resourceRequestCpu"   : "100m",
                                                               "resourceLimitCpu"     : "300m",
                                                               "resourceRequestMemory": "256Mi",
                                                               "resourceLimitMemory"  : "512Mi"],
                                                   "kubeup" : ["image"                  : "repository.nextiva.xyz/kubeup:latest",
                                                               "resourceRequestMemory"  : "64Mi",
                                                               "resourceLimitMemory"    : "256Mi",
                                                               "resourceRequestCpu"     : "100m",
                                                               "resourceLimitCpu"       : "100m",
                                                               "repository"             : "ssh://git@git.nextiva.xyz:7999/cloud/kubeup.git",
                                                               "branch"                 : "develop",
                                                               "cloudAppsRepository"    : "ssh://git@git.nextiva.xyz:7999/cloud/cloud-apps.git",
                                                               "cloudAppsBranch"        : "master",
                                                               "cloudPlatformRepository": "ssh://git@git.nextiva.xyz:7999/cloud/cloud-platform.git",
                                                               "cloudPlatformBranch"    : "dev"],
                                                   "ansible": ["image"                : "ansible/ansible-runner:1.3.4",
                                                               "resourceRequestMemory": "128Mi",
                                                               "resourceLimitMemory"  : "512Mi",
                                                               "repository"           : "ssh://git@git.nextiva.xyz:7999/rel/release-management.git",
                                                               "branch"               : "dev",],
                                                   "docker" : ["image"                : "docker:stable-git",
                                                               "resourceRequestMemory": "128Mi",
                                                               "resourceLimitMemory"  : "256Mi"],
                                                   "maven"  : ["image"                : "maven:3.6.1-jdk-8",
                                                               "resourceRequestCpu"   : "500m",
                                                               "resourceRequestMemory": "1Gi",
                                                               "resourceLimitMemory"  : "2Gi"],
                                                   "pip"    : ["image"                : "python:3.7-alpine",
                                                               "resourceRequestCpu"   : "500m",
                                                               "resourceRequestMemory": "1Gi",
                                                               "resourceLimitMemory"  : "2Gi"],
                                                   "npm"    : ["image"                : "node:lts-alpine",
                                                               "resourceRequestCpu"   : "500m",
                                                               "resourceRequestMemory": "1Gi",
                                                               "resourceLimitMemory"  : "2Gi"],
    ]
}
