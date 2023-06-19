import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import org.apache.commons.lang3.time.StopWatch;

public class HapiInterceptor implements IClientInterceptor {

    private StopWatch stopwatch;

    @Override
    public void interceptRequest(IHttpRequest theRequest) {
        stopwatch = StopWatch.createStarted();
    }

    @Override
    public void interceptResponse(IHttpResponse theResponse) {
        stopwatch.stop();
        long responseTime = stopwatch.getTime();
        System.out.println("Response time: " + responseTime + " ms");
    }

}
