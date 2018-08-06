import java.net.URLEncoder
import static com.nextiva.SharedJobsStaticVars.*


def sendCounter(def value, String helpMessage = '') {
    sendMetric('counter', value)
}

def sendGauge(def value, String helpMessage = '') {
    sendMetric('gauge', value)
}

def sendHistogram(def value, String helpMessage = '') {
    sendMetric('histogram', value)
}

def sendSummary(def value, String helpMessage = '') {
    sendMetric('summary', value)
}

def sendMetric(String metricType, String metricName, def metricValue, Map metricLabels = [:], String metricHelpMessage = '') {
    /*String requestBody = """
                    # HELP telemetry_requests_metrics_latency_microseconds A histogram of the response latency.
                    # TYPE telemetry_requests_metrics_latency_microseconds summary
                    telemetry_requests_metrics_latency_microseconds{quantile="0.01"} 3102
                    telemetry_requests_metrics_latency_microseconds{quantile="0.05"} 3272
                    telemetry_requests_metrics_latency_microseconds{quantile="0.5"} 4773
                    telemetry_requests_metrics_latency_microseconds{quantile="0.9"} 9001
                    telemetry_requests_metrics_latency_microseconds{quantile="0.99"} 76656
                    telemetry_requests_metrics_latency_microseconds_sum 1.7560473e+07
                    telemetry_requests_metrics_latency_microseconds_count 2693
                    """*/

    String labels = mapToLabelsStr(metricLabels)
    String requestBody = """
        # HELP ${metricName} ${metricHelpMessage}
        # TYPE ${metricName} ${metricType}
        ${metricName}{${labels}} ${metricValue}
    """

    log.debug(requestBody)
    httpRequest httpMode: 'POST', requestBody: requestBody,
        url: "${PROMETHEUS_PUSHGATEWAY_URL}/job/some_job/instance/some_instance", consoleLogResponseBody: log.isDebug(),
        contentType: 'APPLICATION_FORM'
}

def getBuildInfoMap(def jobConfig) {
    return [app_name: jobConfig.APP_NAME, ansible_env: jobConfig.ANSIBLE_ENV, deploy_environment: jobConfig.DEPLOY_ENVIRONMENT,
            language: jobConfig.projectFlow['language'], language_version: jobConfig.projectFlow['languageVersion'],
            path_to_src: jobConfig.projectFlow['pathToSrc'], job_timeout_minutes: jobConfig.jobTimeoutMinutes,
            node_label: jobConfig.nodeLabel, version: jobConfig.version, build_version: jobConfig.BUILD_VERSION,
            current_user: common.getCurrentUser()]
}

def mapToLabelsStr(Map labelsMap) {
    //Map labelsMap = getBuildInfoMap(jobConfig)
    String labels = ''
    labelsMap.each { k, v -> labels += "${k}=\"${v}\","}
    return labels
}
