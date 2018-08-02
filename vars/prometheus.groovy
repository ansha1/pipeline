@Grab('io.prometheus:simpleclient:0.4.0')
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter
import io.prometheus.client.Gauge
import io.prometheus.client.Histogram
import io.prometheus.client.Summary


def getRequestsCounter(){
    def requests = Counter.build()
        .name("requests_total3").help("Total requests3.").register();
    return requests
}
