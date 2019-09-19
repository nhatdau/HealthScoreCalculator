package ai.quod.challenge;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import ai.quod.challenge.service.HealthScoreService;

/**
 * Health Score Calculator for GitHub application
 * 
 * @author nhatdau
 *
 */
@SpringBootApplication
public class HealthScoreCalculatorApplication implements CommandLineRunner {
	private static Logger LOG = LoggerFactory.getLogger(HealthScoreCalculatorApplication.class);
	@Autowired
	private HealthScoreService healthScoreService;

	public static void main(String[] args) {
		SpringApplication.run(HealthScoreCalculatorApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		if (args.length != 2) {
			LOG.error("Input parameters need to include start time, end time for calculating");
		} else {
			LOG.info("Start calculating health score of projects");
			healthScoreService.calculate(LocalDateTime.parse(args[0], DateTimeFormatter.ISO_DATE_TIME),
					LocalDateTime.parse(args[1], DateTimeFormatter.ISO_DATE_TIME));
			LOG.info("Finish calculating health score of projects");
		}
	}

}
