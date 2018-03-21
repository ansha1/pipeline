package com.nextiva;


class SharedJobsStaticVars {
    static final BUILD_PROPERTIES_FILENAME = 'build.properties'
    static final LIST_OF_ENVS = ['dev', 'staging', 'rc', 'production', 'test', 'qa']
    static final NEXUS_STATIC_ASSETS_REPO_URL = 'http://repository.nextiva.xyz/repository/static-assets-'
    static final NEXUS_DEB_PKG_REPO_URL = 'http://repository.nextiva.xyz/repository/apt-'
    static final ASSETS_PACKAGE_EXTENSION = 'bzip'
    static final VENV_DIR = '.env'
    static final DOCKER_REGISTRY = 'repository.nextiva.xyz'
    static final DOCKER_REGISTRY_URL = 'https://repository.nextiva.xyz'
    static final DOCKER_REGISTRY_CREDENTIALS_ID = 'nextivaRegistry'
    static final PIP_TRUSTED_HOST = 'repository.nextiva.xyz'
    static final PIP_EXTRA_INDEX_URL = 'http://repository.nextiva.xyz/repository/pypi-'
    static final PIP_EXTRA_INDEX_URL_SUFFIX = 'group/simple'
    static final DEB_PKG_CONTENT_TYPE_PUBLISH = 'Content-Type:multipart/form-data'
    static final JENKINS_AUTH_CREDENTIALS = 'jenkinsbitbucket'
}
