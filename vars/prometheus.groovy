@Grab('io.prometheus:simpleclient:0.4.0')
@Grab('io.prometheus:simpleclient_pushgateway:0.4.0')
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter
import io.prometheus.client.Gauge
import io.prometheus.client.Histogram
import io.prometheus.client.Summary
import io.prometheus.client.exporter.PushGateway


@NonCPS
def getDurationTimer(){
  CollectorRegistry registry = new CollectorRegistry();
  def duration = Gauge.build()
     .name("my_batch_job_duration_seconds").help("Duration of my batch job in seconds.").register(registry);
  def durationTimer = duration.startTimer();
  return durationTimer
}

@NonCPS
def getPushGateway(){
    def pg = new PushGateway("10.103.50.110:9091");
    return pg
}
