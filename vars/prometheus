@Grab('io.prometheus:simpleclient:0.4.0')
import io.prometheus.client.CollectorRegistry


def getRequestsCounter(){
    static final Counter requests = Counter.build()
        .name("requests_total").help("Total requests.").register();
    return requests
}
