import static com.nextiva.SharedJobsStaticVars.*
import java.net.URLEncoder
import java.net.URLDecoder


def sendCounter(String metricName, def metricValue, Map metricLabels = [:], String metricHelpMessage = '') {
    sendMetric(PROMETHEUS_INSTANCE_NAME.toString(), env.JOB_NAME.toString(), metricName, metricValue,
            'counter', metricLabels + getBasicInfoMap(), metricHelpMessage)
}

def sendGauge(String metricName, def metricValue, Map metricLabels = [:], String metricHelpMessage = '') {
    sendMetric(PROMETHEUS_INSTANCE_NAME.toString(), env.JOB_NAME.toString(), metricName, metricValue,
            'gauge', metricLabels + getBasicInfoMap(), metricHelpMessage)
}

def sendHistogram(String metricName, def metricValue, Map metricLabels = [:], String metricHelpMessage = '') {
    sendMetric(PROMETHEUS_INSTANCE_NAME.toString(), env.JOB_NAME.toString(), metricName, metricValue,
            'histogram', metricLabels + getBasicInfoMap(), metricHelpMessage)
}

def sendSummary(String metricName, def metricValue, Map metricLabels = [:], String metricHelpMessage = '') {
    sendMetric(PROMETHEUS_INSTANCE_NAME.toString(), env.JOB_NAME.toString(), metricName, metricValue,
            'summary', metricLabels + getBasicInfoMap(), metricHelpMessage)
}

def sendMetric(String instance, String jobName, String metricName, def metricValue, String metricType,
               Map metricLabels = [:], String metricHelpMessage = '') {

    String labels = mapToLabelsStr(metricLabels)
    String encodedJobName = urlEncode(jobName)
    String encodedInstance = urlEncode(instance)
    String pushgatewayUrl = "${PROMETHEUS_PUSHGATEWAY_URL}/job/${encodedJobName}/instance/${encodedInstance}"

    String requestBody = """
        # HELP ${metricName} ${metricHelpMessage}
        # TYPE ${metricName} ${metricType}
        ${metricName}{${labels}} ${metricValue}
    """

    log.debug("Sending metrics to Prometheus - ${pushgatewayUrl}")
    log.debug(requestBody)

    timeout(time: 10, unit: 'SECONDS') {
        try {
            httpRequest httpMode: 'POST', quiet: !log.isDebug(), consoleLogResponseBody: log.isDebug(),
                    requestBody: requestBody, url: pushgatewayUrl, contentType: 'APPLICATION_FORM'
        } catch (e) {
            log.warning("Can't send metrics to Prometheus! ${e}")
        }
    }
}

def mapToLabelsStr(Map labelsMap) {
    log.warning('>>>>>>>')
    log.warning(labelsMap.toMapString())
    log.warning('<<<<<<<')
    String labels = ''
    labelsMap.each { k, v -> labels += "${k}=\"${v}\","}
    return labels
}

def urlEncode(String value) {
    String decodedValue = URLDecoder.decode(value, 'UTF-8').replaceAll('/| ', '_')
    return URLEncoder.encode(decodedValue, 'UTF-8').replaceAll('%', '_')
}

def getBuildInfoMap(def jobConfig) {
    return getBasicInfoMap() + [app_name: jobConfig.APP_NAME, ansible_env: jobConfig.ANSIBLE_ENV, deploy_environment: jobConfig.DEPLOY_ENVIRONMENT,
                                language: jobConfig.projectFlow['language'], language_version: jobConfig.projectFlow['languageVersion'],
                                path_to_src: jobConfig.projectFlow['pathToSrc'], job_timeout_minutes: jobConfig.jobTimeoutMinutes,
                                node_label: jobConfig.nodeLabel, version: jobConfig.version, build_version: jobConfig.BUILD_VERSION,
                                channel_to_notify: jobConfig.CHANNEL_TO_NOTIFY, application: jobConfig.APP_NAME,
                                blue_green_deploy: jobConfig.BLUE_GREEN_DEPLOY]

}

def getBasicInfoMap() {
    return [current_user: common.getCurrentUser().toString(), build_status: currentBuild.currentResult, node_name: env['NODE_NAME'],
            time_in_millis: currentBuild.timeInMillis, start_time_in_millis: currentBuild.startTimeInMillis,
            duration: currentBuild.duration, duration_string: currentBuild.durationString,
            pipeline_version: env['library.pipeline.version'], branch_name: env['BRANCH_NAME'],
            change_author: env['CHANGE_AUTHOR'], jenkins_job_name: env['JOB_NAME'], build_id: env['BUILD_ID'],
            build_url: env['BUILD_URL']]
}
