package com.nextiva

/**
* args by default are set to '-u root'
* if you need to pass any additional parameters 
* simple add it into the string -> '-u root --network host'
**/


def build(String appName, String buildVersion, String extraPath='.') {
    def customImage = docker.build("${appName}:${buildVersion}", "-f ${extraPath}/Dockerfile --build-arg build_version=${buildVersion} ${extraPath}")
    return customImage
}

def execute(def customImage, def commandToRun, String args='-u root') {
    customImage.inside("${args}") {
        log.info("Running command ${commandToRun}")
        sh "${commandToRun}"
    }
}
