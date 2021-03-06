package org.owasp.appsensor.storage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;

import org.joda.time.DateTime;
import org.owasp.appsensor.AppSensorServer;
import org.owasp.appsensor.Response;
import org.owasp.appsensor.User;
import org.owasp.appsensor.criteria.SearchCriteria;
import org.owasp.appsensor.listener.ResponseListener;
import org.owasp.appsensor.logging.Loggable;
import org.owasp.appsensor.util.DateUtils;
import org.owasp.appsensor.util.FileUtils;
import org.slf4j.Logger;

import com.google.gson.Gson;

/**
 * This is a reference implementation of the {@link ResponseStore}.
 * 
 * Implementations of the {@link ResponseListener} interface can register with 
 * this class and be notified when new {@link Response}s are added to the data store 
 * 
 * This implementation is file-based and has the feature that it will load previous 
 * {@link Response}s if configured to do so.
 * 
 * @author John Melton (jtmelton@gmail.com) http://www.jtmelton.com/
 * @author Raphaël Taban
 */
@Named
@Loggable
public class FileBasedResponseStore extends ResponseStore {

	private Logger logger;
	
	@SuppressWarnings("unused")
	@Inject
	private AppSensorServer appSensorServer;
	
	public static final String DEFAULT_FILE_PATH = File.separator + "tmp";
	
	public static final String DEFAULT_FILE_NAME = "responses.txt";
	
	public static final String FILE_PATH_CONFIGURATION_KEY = "filePath";
	
	public static final String FILE_NAME_CONFIGURATION_KEY = "fileName";
	
	private Gson gson = new Gson();
	
	private Path path = null;
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addResponse(Response response) {
		logger.warn("Security response " + response + " triggered for user: " + response.getUser().getUsername());

		writeResponse(response);
		
		super.notifyListeners(response);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<Response> findResponses(SearchCriteria criteria) {
		if (criteria == null) {
			throw new IllegalArgumentException("criteria must be non-null");
		}
		
		Collection<Response> matches = new ArrayList<Response>();
		
		User user = criteria.getUser();
		Collection<String> detectionSystemIds = criteria.getDetectionSystemIds(); 
		DateTime earliest = DateUtils.fromString(criteria.getEarliest());
		
		Collection<Response> responses = loadResponses();
		
		for (Response response : responses) {
			//check user match if user specified
			boolean userMatch = (user != null) ? user.equals(response.getUser()) : true;
			
			//check detection system match if detection systems specified
			boolean detectionSystemMatch = (detectionSystemIds != null && detectionSystemIds.size() > 0) ? 
					detectionSystemIds.contains(response.getDetectionSystemId()) : true;
			
			boolean earliestMatch = (earliest != null) ? earliest.isBefore(DateUtils.fromString(response.getTimestamp())) : true;
					
			if (userMatch && detectionSystemMatch && earliestMatch) {
				matches.add(response);
			}
		}
		
		return matches;
	}
	
	protected void writeResponse(Response response) {
		String json = gson.toJson(response);
		
		try {
			Files.write(getPath(), Arrays.asList(json), StandardCharsets.UTF_8, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
		} catch (IOException e) {
			logger.error("Error occurred loading writing event file to path: " + getPath(), e);
		}
	}
	
	protected Collection<Response> loadResponses() {
		Collection<Response> responses = new ArrayList<>();
		
		try {
			Collection<String> lines = Files.readAllLines(getPath());
			
			for (String line : lines) {
				Response response = gson.fromJson(line, Response.class);
				
				responses.add(response);
			}
		} catch (IOException e) {
			logger.error("Error occurred loading configured event file from path: " + getPath(), e);
		}
		
		return responses;
	}
	
	protected Path getPath() {
		if (path != null && Files.exists(path)) {
			return path;
		}
		
		path = FileUtils.getOrCreateFile(lookupFilePath(), lookupFileName());
		
		return path;
	}
	
	protected String lookupFilePath() {
		return DEFAULT_FILE_PATH;
	}
	
	protected String lookupFileName() {
		return DEFAULT_FILE_NAME;
	}
	
}
