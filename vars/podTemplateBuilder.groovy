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
                                properties([
                                        buildDiscarder(logRotator(daysToKeepStr: buildDaysToKeepStr, numToKeepStr: buildNumToKeepStr)),
                                        disableConcurrentBuilds(),
                                        parameters(paramlist),
//                        parameters([
////                                string(name: 'submodule', defaultValue: ''),
////                                string(name: 'submodule_branch', defaultValue: ''),
////                                string(name: 'commit_sha', defaultValue: ''),
//                        ])
                                ])

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
    this.paramlist = podTemplateConfiguration.get("paramlist", [booleanParam(name: 'DEBUG', description: 'Enable DEBUG mode with extended output', defaultValue: false),])
    return this
}


//disableConcurrentBuilds()
//authorizationMatrix(inheritanceStrategy: nonInheriting(), permissions: securityPermissions)