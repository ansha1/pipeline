def call(Map slaveConfig, body) {

/*jobTriggers could be
cron('H/15 * * * *')
upstream(threshold: hudson.model.Result.SUCCESS, upstreamProjects: "surveys-server/dev")
*/
    slaveName = slaveConfig.get("slaveName", "slave")
    buildNamespace = slaveConfig.get("buildNamespace", "jenkins")
    image = slaveConfig.get("image")
    if (image == null) {
        error "Slave image is not defined, please define it in the your slaveConfig"
    }
    resourceRequestCpu = slaveConfig.get("resourceRequestCpu", "250m")
    resourceRequestMemory = slaveConfig.get("resourceRequestMemory", "1Gi")
    buildDaysToKeepStr = slaveConfig.get("buildDaysToKeepStr", "3")
    buildNumToKeepStr = slaveConfig.get("buildNumToKeepStr", "5")
    jobTimeoutMinutes = slaveConfig.get("jobTimeoutMinutes", "60")
    paramlist = slaveConfig.get("paramlist", []) + [booleanParam(name: 'DEBUG', description: 'Enable DEBUG mode with extended output', defaultValue: false)]
    jobTriggers = slaveConfig.get("jobTriggers", [])
    authMap = slaveConfig.get("auth", [:])
    allowedUsers = authMap.get(env.BRANCH_NAME, ["authenticated"])
    securityPermissions = generateSecurityPermissions(allowedUsers)
    propertiesList = [parameters(paramlist),
                      buildDiscarder(logRotator(daysToKeepStr: buildDaysToKeepStr, numToKeepStr: buildNumToKeepStr)),
                      authorizationMatrix(inheritanceStrategy: nonInheriting(), permissions: securityPermissions),
                      pipelineTriggers(jobTriggers)]
    isDisableConcurrentBuildsEnabled = slaveConfig.get("disableConcurrentBuilds", true)
    if (isDisableConcurrentBuildsEnabled) {
        propertiesList += [disableConcurrentBuilds()]
    }


    def label = "${slaveName}-${UUID.randomUUID().toString()}"
    parentPodtemplate = libraryResource 'podtemplate/default.yaml'

    podTemplate(label: 'parent', yaml: parentPodtemplate) {
        podTemplate(label: label, workingDir: '/home/jenkins', namespace: buildNamespace,
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


List<String> generateSecurityPermissions(List<String> allowedUsers) {
    List<String> basicList = ['hudson.model.Item.Read:authenticated']
    allowedUsers.each {
        basicList.add("hudson.model.Item.Build:${it}")
        basicList.add("hudson.model.Item.Cancel:${it}")
        basicList.add("hudson.model.Item.Workspace:${it}")
    }
    return basicList
}

//
///*jobTriggers could be
//cron('H/15 * * * *')
//upstream(threshold: hudson.model.Result.SUCCESS, upstreamProjects: "surveys-server/dev")
// */
//def build(Map slaveConfig) {
//
//    this.slaveName = slaveConfig.get("slaveName", "slave")
//    this.buildNamespace = slaveConfig.get("buildNamespace", "jenkins")
//    this.image = slaveConfig.get("image")
//    this.resourceRequestCpu = slaveConfig.get("resourceRequestCpu", "250m")
//    this.resourceRequestMemory = slaveConfig.get("resourceRequestMemory", "1Gi")
//    this.buildDaysToKeepStr = slaveConfig.get("buildDaysToKeepStr", "3")
//    this.buildNumToKeepStr = slaveConfig.get("buildNumToKeepStr", "5")
//    this.jobTimeoutMinutes = slaveConfig.get("jobTimeoutMinutes", "60")
//    this.paramlist = slaveConfig.get("paramlist", []) + [booleanParam(name: 'DEBUG', description: 'Enable DEBUG mode with extended output', defaultValue: false)]
//    this.jobTriggers = slaveConfig.get("jobTriggers", [])
//    this.authMap = slaveConfig.get("auth", [:])
//    this.allowedUsers = this.authMap.get(env.BRANCH_NAME, ["authenticated"])
//    this.securityPermissions = generateSecurityPermissions(this.allowedUsers)
//    this.propertiesList = [parameters(paramlist),
//                           buildDiscarder(logRotator(daysToKeepStr: this.buildDaysToKeepStr, numToKeepStr: this.buildNumToKeepStr)),
//                           authorizationMatrix(inheritanceStrategy: nonInheriting(), permissions: this.securityPermissions),
//                           pipelineTriggers(jobTriggers)]
//    this.isDisableConcurrentBuildsEnabled = slaveConfig.get("disableConcurrentBuilds", true)
//    if (this.isDisableConcurrentBuildsEnabled) {
//        this.propertiesList += [disableConcurrentBuilds()]
//    }
//    return this
//}
