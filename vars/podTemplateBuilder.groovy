def call(body) {
    def label = "slave-${UUID.randomUUID().toString()}"
    parentPodtemplate = libraryResource 'podtemplate/default.yaml'

    podTemplate(label: 'parent', yaml: parentPodtemplate) {}


    podTemplate(label: label, inheritFrom: 'parent', namespace: 'jenkins', workingDir: '/home/jenkins',
            containers: [containerTemplate(name: 'build', image: image, command: 'cat', ttyEnabled: true)],
            volumes: [hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock'),
                      hostPathVolume(hostPath: '/opt/m2cache', mountPath: '/opt/m2cache'),
                      hostPathVolume(hostPath: '/opt/npmcache', mountPath: '/opt/npmcache'),
                      hostPathVolume(hostPath: '/opt/yarncache', mountPath: '/opt/yarncache')]) {
        body.call(label)
    }
}


def build(String image){
    image = this.image
    return this
}