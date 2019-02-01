def call(body) {
    def label = "${slaveName}-${UUID.randomUUID().toString()}"
    parentPodtemplate = libraryResource 'podtemplate/default.yaml'

//    List paramlist = [
//            string(name: 'submodule', defaultValue: ''),
//            string(name: 'submodule_branch', defaultValue: ''),
//            string(name: 'commit_sha', defaultValue: ''),
//    ]

    podTemplate(label: 'parent', yaml: parentPodtemplate) {
        podTemplate(label: label, workingDir: '/home/jenkins',
                containers: [containerTemplate(name: 'build', image: image, command: 'cat', ttyEnabled: true,
                        resourceRequestCpu: resourceRequestCpu,
                        resourceRequestMemory: resourceRequestMemory,
                        envVars: [
                                envVar(key: 'MYSQL_ALLOW_EMPTY_PASSWORD', value: 'true'),
                                envVar(key: 'BLABLA', value: 'true')
                        ],)],
                volumes: [hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock'),
                          hostPathVolume(hostPath: '/opt/m2cache', mountPath: '/opt/m2cache'),
                          hostPathVolume(hostPath: '/opt/npmcache', mountPath: '/opt/npmcache'),
                          hostPathVolume(hostPath: '/opt/yarncache', mountPath: '/opt/yarncache')]) {


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
    this.image = podTemplateConfiguration.get("image")
    this.resourceRequestCpu = podTemplateConfiguration.get("resourceRequestCpu", "250m")
    this.resourceRequestMemory = podTemplateConfiguration.get("resourceRequestMemory", "1Gi")
    this.buildDaysToKeepStr = podTemplateConfiguration.get("buildDaysToKeepStr", "3")
    this.buildNumToKeepStr = podTemplateConfiguration.get("buildNumToKeepStr", "5")
    this.jobTimeoutMinutes = podTemplateConfiguration.get("jobTimeoutMinutes", "60")
    this.paramlist = podTemplateConfiguration.get("paramlist", []) + [booleanParam(name: 'DEBUG', description: 'Enable DEBUG mode with extended output', defaultValue: false)]
    this.isDisableConcurrentBuildsEnabled = podTemplateConfiguration.get("disableConcurrentBuilds", true)
    if (this.isDisableConcurrentBuildsEnabled) {
        this.propertiesList += [disableConcurrentBuilds()]
    }
    this.authMap = podTemplateConfiguration.get("auth", [:])
    this.allowedUsers = this.authMap.get(env.BRANCH_NAME, [])
    this.securityPermissions = generateSecurityPermissions(this.allowedUsers)

    this.propertiesList = [parameters(paramlist), buildDiscarder(logRotator(daysToKeepStr: this.buildDaysToKeepStr, numToKeepStr: this.buildNumToKeepStr)), authorizationMatrix(inheritanceStrategy: nonInheriting(), permissions: this.securityPermissions)]

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