import java.net.URLEncoder
import static com.nextiva.SharedJobsStaticVars.*


/*def sendGauge() {
    def pullRequestResponce = httpRequest authentication: BITBUCKET_JENKINS_AUTH, contentType: 'APPLICATION_JSON', httpMode: 'POST', requestBody: requestBody, url: createPrUrl,
            consoleLogResponseBody: log.isDebug()
}*/


def sendMetric() {
    String requestBody = """
                    # HELP telemetry_requests_metrics_latency_microseconds A histogram of the response latency.
                    # TYPE telemetry_requests_metrics_latency_microseconds summary
                    telemetry_requests_metrics_latency_microseconds{quantile="0.01"} 3102
                    telemetry_requests_metrics_latency_microseconds{quantile="0.05"} 3272
                    telemetry_requests_metrics_latency_microseconds{quantile="0.5"} 4773
                    telemetry_requests_metrics_latency_microseconds{quantile="0.9"} 9001
                    telemetry_requests_metrics_latency_microseconds{quantile="0.99"} 76656
                    telemetry_requests_metrics_latency_microseconds_sum 1.7560473e+07
                    telemetry_requests_metrics_latency_microseconds_count 2693"""

    def pullRequestResponce = httpRequest httpMode: 'POST', requestBody: requestBody,
            url: "${PROMETHEUS_PUSHGATEWAY_URL}/job/some_job/instance/some_instance", consoleLogResponseBody: log.isDebug(),
            contentType: 'APPLICATION_FORM'
}
