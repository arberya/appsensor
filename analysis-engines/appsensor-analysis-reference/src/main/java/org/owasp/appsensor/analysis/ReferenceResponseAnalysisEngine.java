package org.owasp.appsensor.analysis;

import javax.inject.Inject;
import javax.inject.Named;

import org.owasp.appsensor.AppSensorClient;
import org.owasp.appsensor.Response;
import org.owasp.appsensor.logging.Loggable;
import org.owasp.appsensor.response.ResponseHandler;
import org.owasp.appsensor.storage.ResponseStore;
import org.slf4j.Logger;

/**
 * This is a reference {@link Response} analysis engine, 
 * and is an implementation of the Observer pattern. 
 * 
 * It is notified with implementations of the {@link Response} class.
 * 
 * The implementation is trivial and simply delegates the work to the configured 
 * {@link ResponseHandler} for processing.
 * 
 * @author John Melton (jtmelton@gmail.com) http://www.jtmelton.com/
 */
@Named
@Loggable
public class ReferenceResponseAnalysisEngine extends ResponseAnalysisEngine {

	private Logger logger;
	
	@Inject
	private AppSensorClient appSensorClient;
	
	/**
	 * This method simply catches responses and calls the 
	 * configured {@link ResponseHandler} to process them. 
	 * 
	 * @param response {@link Response} that has been added to the {@link ResponseStore}.
	 */
	@Override
	public void analyze(Response response) {
		if (response != null) {
			logger.info("Response executed for user <" + response.getUser().getUsername() + "> - executing response action " + response.getAction());
			
			appSensorClient.getResponseHandler().handle(response);
		}
	}
	
}
