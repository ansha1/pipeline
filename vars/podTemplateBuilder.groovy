def call(body) {
    def label = "slave-$appName-${UUID.randomUUID().toString()}"
    parentPodtemplate = libraryResource 'podtemplate/default.yaml'

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
            body.call(label)
        }
    }
}

def build(Map podTemplateConfiguration) {
    this.appName = podTemplateConfiguration.get("appName", "")
    this.image = podTemplateConfiguration.get("image")
    this.resourceRequestCpu = podTemplateConfiguration.get("resourceRequestCpu", "250m")
    this.resourceRequestMemory = podTemplateConfiguration.get("resourceRequestMemory", "1Gi")
    return this
}


