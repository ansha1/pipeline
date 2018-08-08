import static com.nextiva.SharedJobsStaticVars.*


def sendCounter(String metricName, def metricValue, Map metricLabels = [:], String metricHelpMessage = '') {
    sendMetric(PROMETHEUS_JOB_NAME.toString(), PROMETHEUS_INSTANCE_NAME.toString(), metricName, metricValue,
            'counter', metricLabels, metricHelpMessage)
}

def sendGauge(String job_name, String metricName, def metricValue, Map metricLabels = [:], String metricHelpMessage = '') {
    sendMetric(PROMETHEUS_INSTANCE_NAME.toString(), job_name, metricName, metricValue,
            'gauge', metricLabels, metricHelpMessage)
}

def sendHistogram(String metricName, def metricValue, Map metricLabels = [:], String metricHelpMessage = '') {
    sendMetric(PROMETHEUS_JOB_NAME.toString(), PROMETHEUS_INSTANCE_NAME.toString(), metricName, metricValue,
            'histogram', metricLabels, metricHelpMessage)
}

def sendSummary(String metricName, def metricValue, Map metricLabels = [:], String metricHelpMessage = '') {
    sendMetric(PROMETHEUS_JOB_NAME.toString(), PROMETHEUS_INSTANCE_NAME.toString(), metricName, metricValue,
            'summary', metricLabels, metricHelpMessage)
}

def sendMetric(String instance, String jobName, String metricName, def metricValue, String metricType,
               Map metricLabels = [:], String metricHelpMessage = '') {
    String labels = mapToLabelsStr(metricLabels)
    String shortJobName = jobName.replace('/', '_')
    String requestBody = """
        # HELP ${metricName} ${metricHelpMessage}
        # TYPE ${metricName} ${metricType}
        ${metricName}{${labels}} ${metricValue}
    """

    log.debug(requestBody)

    timeout(time: 10, unit: 'SECONDS') {
        try {
            httpRequest httpMode: 'POST', requestBody: requestBody,
                    url: "${PROMETHEUS_PUSHGATEWAY_URL}/job/${shortJobName}/instance/${instance}", consoleLogResponseBody: log.isDebug(),
                    contentType: 'APPLICATION_FORM'
        } catch (e) {
            log.warning("Can't send metrics to Prometheus! ${e}")
        }
    }
}

def mapToLabelsStr(Map labelsMap) {
    String labels = ''
    labelsMap.each { k, v -> labels += "${k}=\"${v}\","}
    return labels
}

def getBuildInfoMap(def jobConfig) {
    return getJenkinsInfoMap() + [app_name: jobConfig.APP_NAME, ansible_env: jobConfig.ANSIBLE_ENV, deploy_environment: jobConfig.DEPLOY_ENVIRONMENT,
            language: jobConfig.projectFlow['language'], language_version: jobConfig.projectFlow['languageVersion'],
            path_to_src: jobConfig.projectFlow['pathToSrc'], job_timeout_minutes: jobConfig.jobTimeoutMinutes,
            node_label: jobConfig.nodeLabel, version: jobConfig.version, build_version: jobConfig.BUILD_VERSION,
            channel_to_notify: jobConfig.CHANNEL_TO_NOTIFY]

}

def getJenkinsInfoMap() {
    return [current_user: common.getCurrentUser(), build_status: currentBuild.result, node_name: env.NODE_NAME,
            time_in_millis: currentBuild.timeInMillis, start_time_in_millis: currentBuild.startTimeInMillis,
            duration: currentBuild.duration, duration_string: currentBuild.durationString,
            pipeline_version: env['library.pipelines.version'], branch_name: env['BRANCH_NAME'],
            change_author: env['CHANGE_AUTHOR']]
}
