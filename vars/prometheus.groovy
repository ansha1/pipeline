@Grab('io.prometheus:simpleclient:0.4.0')
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter
import io.prometheus.client.Gauge
import io.prometheus.client.Histogram
import io.prometheus.client.Summary


def getRequestsCounter(){
    static final Counter requests = Counter.build()
        .name("requests_total").help("Total requests.").register();
    return requests
}
