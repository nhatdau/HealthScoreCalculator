package ai.quod.challenge.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.time.LocalDateTime;

/**
 * Heath Score Service
 * 
 * @author nhatdau
 *
 */
public interface HealthScoreService {

	/**
	 * Method to calculate health score of projects on GitHub in 1 period
	 * 
	 * @param startTime start time of calculation
	 * @param endTime   end time of calculation
	 * @throws MalformedURLException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void calculate(LocalDateTime startTime, LocalDateTime endTime)
			throws MalformedURLException, FileNotFoundException, IOException;
}
