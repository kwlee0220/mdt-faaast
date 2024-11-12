package mdt.endpoint;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import de.fraunhofer.iosb.ilt.faaast.service.endpoint.http.HttpEndpoint;
import de.fraunhofer.iosb.ilt.faaast.service.endpoint.http.HttpEndpointConfig;
import de.fraunhofer.iosb.ilt.faaast.service.exception.EndpointException;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class MDTHttpEndpoint extends HttpEndpoint {
	private static final int INTERVAL = 1;
	
	private ScheduledFuture<?> m_schedule;
	
	public static class MDTHttpEndpointConfig extends HttpEndpointConfig {
	    private String mdtEndpoint;
	    
	    public String getMdtEndpoint() {
	        return mdtEndpoint;
	    }

	    public void setMdtEndpoint(String endpoint) {
	        this.mdtEndpoint = endpoint;
	    }
	}

    @Override
    public MDTHttpEndpointConfig asConfig() {
        return (MDTHttpEndpointConfig)super.asConfig();
    }
	
    @Override
    public void start() throws EndpointException {
    	super.start();
    	
    	ScheduledExecutorService exector = Executors.newSingleThreadScheduledExecutor();
    	m_schedule = exector.scheduleAtFixedRate(this::checkHealth, INTERVAL, INTERVAL, TimeUnit.MINUTES);
    }
    
    private void checkHealth() {
    	
    }
}
