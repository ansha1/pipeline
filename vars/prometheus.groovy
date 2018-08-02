@Grab('io.prometheus:simpleclient:0.4.0')
@Grab('io.prometheus:simpleclient_pushgateway:0.4.0')
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter
import io.prometheus.client.Gauge
import io.prometheus.client.Histogram
import io.prometheus.client.Summary
import io.prometheus.client.exporter.PushGateway


def event(){
    def registry = new CollectorRegistry();
    def duration = Gauge.build()
        .name("my_batch_job_duration_seconds").help("Duration of my batch job in seconds.").register(registry);
    def durationTimer = duration.startTimer();

    /*Gauge activeTransactions = Gauge.build()
        .name("my_library_transactions_active")
        .help("Active transactions.")
        .register(registry);*/

    try {
        //activeTransactions.inc()
        // Your code here.
    
        // This is only added to the registry after success,
        // so that a previous success in the Pushgateway isn't overwritten on failure.
        def lastSuccess = Gauge.build()
             .name("my_batch_job_last_success").help("Last time my batch job succeeded, in unixtime.").register(registry);
        lastSuccess.setToCurrentTime();

        //sleep(20)
        echo('222')

        //activeTransactions.dec()
    } finally {
        durationTimer.setDuration();
        def pg = new PushGateway("10.103.50.110:9091");
        pg.pushAdd(registry, "my_batch_job");
    }
}
