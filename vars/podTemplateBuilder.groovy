def call(body) {
    def label = "${slaveName}-${UUID.randomUUID().toString()}"
    parentPodtemplate = libraryResource 'podtemplate/default.yaml'

    podTemplate(label: 'parent', yaml: parentPodtemplate) {
        podTemplate(label: label, workingDir: '/home/jenkins', namespace: buildNamespace,
                slaveConnectTimeout: 1200,
                activeDeadlineSeconds: 1200,
                idleMinutes: 240,
                containers: [containerTemplate(name: 'build', image: image, command: 'cat', ttyEnabled: true,
                        resourceRequestCpu: resourceRequestCpu,
                        resourceRequestMemory: resourceRequestMemory,
                        envVars: [
                                envVar(key: 'CYPRESS_CACHE_FOLDER', value: '/opt/cypress_cache'),
                                envVar(key: 'YARN_CACHE_FOLDER', value: '/opt/yarn_cache'),
                                envVar(key: 'CYPRESS_CACHE_FOLDER', value: '/opt/cypress_cache'),
                                envVar(key: 'npm_config_cache', value: '/opt/npmcache'),
                                envVar(key: 'M2_LOCAL_REPO', value: '/home/jenkins/.m2repo')
                        ],)],
                volumes: [hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock'),
                          hostPathVolume(hostPath: '/opt/m2cache', mountPath: '/home/jenkins/.m2repo'),
                          hostPathVolume(hostPath: '/opt/npmcache', mountPath: '/opt/npmcache'),
                          hostPathVolume(hostPath: '/opt/cypress_cache', mountPath: '/opt/cypress_cache'),
                          hostPathVolume(hostPath: '/opt/yarncache', mountPath: '/opt/yarncache'),
//                          secretVolume(mountPath: '/root/.m2', secretName: 'maven-secret')]) {
                          ]) {

            timestamps {
                ansiColor('xterm') {
                    timeout(time: jobTimeoutMinutes, unit: 'MINUTES') {

                        node(label) {
                            properties(propertiesList)
                            body.call()
                        }
                    }
                }
            }
        }
    }
}

def build(Map podTemplateConfiguration) {

    this.slaveName = podTemplateConfiguration.get("slaveName", "slave")
    this.buildNamespace = podTemplateConfiguration.get("buildNamespace", "jenkins")
    this.image = podTemplateConfiguration.get("image")
    this.resourceRequestCpu = podTemplateConfiguration.get("resourceRequestCpu", "250m")
    this.resourceRequestMemory = podTemplateConfiguration.get("resourceRequestMemory", "1Gi")
    this.buildDaysToKeepStr = podTemplateConfiguration.get("buildDaysToKeepStr", "3")
    this.buildNumToKeepStr = podTemplateConfiguration.get("buildNumToKeepStr", "5")
    this.jobTimeoutMinutes = podTemplateConfiguration.get("jobTimeoutMinutes", "60")
    this.paramlist = podTemplateConfiguration.get("paramlist", []) + [booleanParam(name: 'DEBUG', description: 'Enable DEBUG mode with extended output', defaultValue: false)]
    this.authMap = podTemplateConfiguration.get("auth", [:])
    this.allowedUsers = this.authMap.get(env.BRANCH_NAME, ["authenticated"])
    this.securityPermissions = generateSecurityPermissions(this.allowedUsers)
    this.propertiesList = [parameters(paramlist), buildDiscarder(logRotator(daysToKeepStr: this.buildDaysToKeepStr, numToKeepStr: this.buildNumToKeepStr)), authorizationMatrix(inheritanceStrategy: nonInheriting(), permissions: this.securityPermissions)]
    this.isDisableConcurrentBuildsEnabled = podTemplateConfiguration.get("disableConcurrentBuilds", true)
    if (this.isDisableConcurrentBuildsEnabled) {
        this.propertiesList += [disableConcurrentBuilds()]
    }
    return this
}


List<String> generateSecurityPermissions(List<String> allowedUsers) {
    List<String> basicList = ['hudson.model.Item.Read:authenticated']
    allowedUsers.each {
        basicList.add("hudson.model.Item.Build:${it}")
        basicList.add("hudson.model.Item.Cancel:${it}")
        basicList.add("hudson.model.Item.Workspace:${it}")
    }
    return basicList
}