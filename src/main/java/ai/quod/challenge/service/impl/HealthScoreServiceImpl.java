package ai.quod.challenge.service.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.quod.challenge.model.Issue;
import ai.quod.challenge.model.RepoStatistic;
import ai.quod.challenge.service.HealthScoreService;

/**
 * {@link HealthScoreService} implementation class
 * 
 * @author nhatdau
 *
 */
@Service
public class HealthScoreServiceImpl implements HealthScoreService {
	private static final String ISSUES_EVENT = "IssuesEvent";
	private static final String CLOSED = "closed";
	private static final String CREATED_AT = "created_at";
	private static final String ISSUE = "issue";
	private static final String OPENED = "opened";
	private static final String ACTION = "action";
	private static final String PAYLOAD = "payload";
	private static final String ACTOR = "actor";
	private static final String NAME = "name";
	private static final String ID = "id";
	private static final String REPO = "repo";
	private static final String TYPE = "type";
	private static final String PUSH_EVENT = "PushEvent";
	private static final String API_HOST = "http://data.gharchive.org/";
	private static Logger LOG = LoggerFactory.getLogger(HealthScoreServiceImpl.class);

	@Override
	public void calculate(LocalDateTime startTime, LocalDateTime endTime) throws IOException {
		Assert.isTrue(endTime.isAfter(startTime), "End time must be greater than start time");
		String folderName = LocalDateTime.now().toString();
		File folder = new File(folderName);
		folder.mkdir();
		downloadDataFiles(startTime, endTime, folderName);
		Map<Long, RepoStatistic> map = processDataFiles(startTime, endTime, folderName);
		Collection<RepoStatistic> repoStatistics = calculateMetricsAndHealthScore(startTime, endTime, map);
		exportResultToCsv(repoStatistics, folderName);
	}

	private void exportResultToCsv(Collection<RepoStatistic> repoStatistics, String folderName) throws IOException {
		List<RepoStatistic> finalList = repoStatistics.stream()
				.sorted(Collections.reverseOrder(Comparator.comparing(RepoStatistic::getHealth_score)))
				.collect(Collectors.toList());

		FileWriter csvWriter = new FileWriter(folderName + File.separator + "results.csv");
		csvWriter.append(
				"org,repo_name,health_score,num_commits,num_contributors,average_num_commits,average_issue_open_time");
		csvWriter.append("\n");
		finalList.forEach(record -> {
			try {
				csvWriter.append(record.getOrg() + ",");
				csvWriter.append(record.getName() + ",");
				csvWriter.append(record.getHealth_score() + ",");
				csvWriter.append(record.getNum_commits() + ",");
				csvWriter.append(record.getContributors().size() + ",");
				csvWriter.append(record.getAverage_num_commits() + ",");
				csvWriter.append(record.getAverage_issue_open_time() + "\n");
			} catch (IOException e) {
				LOG.error("Error in writing CSV file", e);
			}
		});
		csvWriter.flush();
		csvWriter.close();
	}

	private Collection<RepoStatistic> calculateMetricsAndHealthScore(LocalDateTime startTime, LocalDateTime endTime,
			Map<Long, RepoStatistic> map) {
		long nDays = ChronoUnit.DAYS.between(startTime, endTime);
		long max_num_commits = 1;
		int max_num_contributors = 1;
		double max_average_num_commits = 0;
		double min_average_issue_open_time = Long.MAX_VALUE;
		Collection<RepoStatistic> repoStatistics = map.values();
		Iterator<RepoStatistic> iterator = repoStatistics.iterator();
		while (iterator.hasNext()) {
			RepoStatistic repoStatistic = iterator.next();
			if (repoStatistic.getNum_commits() > max_num_commits)
				max_num_commits = repoStatistic.getNum_commits();
			if (repoStatistic.getContributors().size() > max_num_contributors)
				max_num_contributors = repoStatistic.getContributors().size();
			double average_num_commits = (new BigDecimal(((double) repoStatistic.getNum_commits()) / nDays))
					.setScale(2, RoundingMode.HALF_UP).doubleValue();
			repoStatistic.setAverage_num_commits(average_num_commits);
			if (average_num_commits > max_average_num_commits)
				max_average_num_commits = average_num_commits;
			int numIssues = repoStatistic.getIssues().size();
			if (numIssues > 0) {
				double average_issue_open_time = (new BigDecimal(
						((double) repoStatistic.getIssues().values().stream().mapToLong(issue -> {
							if (issue.getCloseTime() > 0) {
								return issue.getCloseTime() - issue.getOpenTime();
							} else {
								return endTime.toEpochSecond(ZoneOffset.UTC) - issue.getOpenTime();
							}
						}).sum()) / numIssues)).setScale(2, RoundingMode.HALF_UP).doubleValue();
				repoStatistic.setAverage_issue_open_time(average_issue_open_time);
				if (average_issue_open_time < min_average_issue_open_time)
					min_average_issue_open_time = average_issue_open_time;
			}
		}
		// calculate health score
		long final_max_num_commits = max_num_commits;
		int final_max_num_contributors = max_num_contributors;
		double final_max_average_num_commits = max_average_num_commits;
		double final_min_average_issue_open_time = min_average_issue_open_time;
		repoStatistics.stream().forEach(repoStatistic -> {
			float health_score = (new BigDecimal((((double) repoStatistic.getNum_commits()) / final_max_num_commits)
					* (((double) repoStatistic.getContributors().size()) / final_max_num_contributors)
					* (repoStatistic.getAverage_num_commits() / final_max_average_num_commits)
					* (repoStatistic.getIssues().size() > 0
							? repoStatistic.getAverage_issue_open_time() / final_min_average_issue_open_time
							: 1))).setScale(2, RoundingMode.HALF_UP).floatValue();
			repoStatistic.setHealth_score(health_score);
		});
		return repoStatistics;
	}

	private Map<Long, RepoStatistic> processDataFiles(LocalDateTime startTime, LocalDateTime endTime, String folderName)
			throws FileNotFoundException, IOException {
		LocalDate currentDate = startTime.toLocalDate();
		LocalDate endDate = endTime.toLocalDate();
		Map<Long, RepoStatistic> map = new HashMap<Long, RepoStatistic>();
		while (currentDate.isBefore(endDate)) {
			String sDate = currentDate.toString();
			for (int i = 0; i <= 23; i++) {
				String gzfileName = folderName + File.separator + sDate + "-" + i + ".json.gz";
				String outputFileName = folderName + File.separator + sDate + "-" + i + ".json";
				byte[] buffer = new byte[1024];
				GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(gzfileName));
				FileOutputStream out = new FileOutputStream(outputFileName);
				int len;
				while ((len = gzis.read(buffer)) > 0) {
					out.write(buffer, 0, len);
				}
				gzis.close();
				out.close();
				// parse json file
				FileReader fileReader = new FileReader(outputFileName);
				BufferedReader bufferedReader = new BufferedReader(fileReader);
				String line = null;
				ObjectMapper objectMapper = new ObjectMapper();
				while ((line = bufferedReader.readLine()) != null) {
					JsonNode jsonNode = objectMapper.readTree(line);
					String eventType = jsonNode.get(TYPE).asText();
					switch (eventType) {
					case PUSH_EVENT:
						long repoId = jsonNode.get(REPO).get(ID).asLong();
						if (!map.containsKey(repoId)) {
							RepoStatistic repoStatistic = new RepoStatistic();
							repoStatistic.setId(repoId);
							String[] org_name = jsonNode.get(REPO).get(NAME).asText().split("/");
							repoStatistic.setName(org_name[1]);
							repoStatistic.setOrg(org_name[0]);
							repoStatistic.setNum_commits(1);
							repoStatistic.getContributors().add(jsonNode.get(ACTOR).get(ID).asLong());
							map.put(repoId, repoStatistic);
						} else {
							RepoStatistic repoStatistic = map.get(repoId);
							repoStatistic.setNum_commits(repoStatistic.getNum_commits() + 1);
							repoStatistic.getContributors().add(jsonNode.get(ACTOR).get(ID).asLong());
						}
						break;
					case ISSUES_EVENT:
						long repoId2 = jsonNode.get(REPO).get(ID).asLong();
						String action = jsonNode.get(PAYLOAD).get(ACTION).asText();
						if (!map.containsKey(repoId2)) {
							if (action.contains(OPENED)) {
								RepoStatistic repoStatistic = new RepoStatistic();
								repoStatistic.setId(repoId2);
								String[] org_name = jsonNode.get(REPO).get(NAME).asText().split("/");
								repoStatistic.setName(org_name[1]);
								repoStatistic.setOrg(org_name[0]);
								Issue issue = new Issue();
								long issueId = jsonNode.get(PAYLOAD).get(ISSUE).get(ID).asLong();
								issue.setId(issueId);
								issue.setOpenTime(LocalDateTime
										.parse(jsonNode.get(CREATED_AT).asText(), DateTimeFormatter.ISO_DATE_TIME)
										.toEpochSecond(ZoneOffset.UTC));
								repoStatistic.getIssues().put(issueId, issue);
								map.put(repoId2, repoStatistic);
							}
						} else {
							RepoStatistic repoStatistic = map.get(repoId2);
							long issueId = jsonNode.get(PAYLOAD).get(ISSUE).get(ID).asLong();
							if (action.contains(OPENED)) {
								Issue issue = new Issue();
								issue.setId(issueId);
								issue.setOpenTime(LocalDateTime
										.parse(jsonNode.get(CREATED_AT).asText(), DateTimeFormatter.ISO_DATE_TIME)
										.toEpochSecond(ZoneOffset.UTC));
								repoStatistic.getIssues().put(issueId, issue);
							} else if (action.equals(CLOSED)) {
								if (repoStatistic.getIssues().containsKey(issueId)) {
									repoStatistic.getIssues().get(issueId)
											.setCloseTime(LocalDateTime
													.parse(jsonNode.get(CREATED_AT).asText(),
															DateTimeFormatter.ISO_DATE_TIME)
													.toEpochSecond(ZoneOffset.UTC));
								}
							}
						}
						break;
					default:
						break;
					}
				}
				bufferedReader.close();
			}
			currentDate = currentDate.plusDays(1);
		}
		return map;
	}

	private void downloadDataFiles(LocalDateTime startTime, LocalDateTime endTime, String folderName)
			throws MalformedURLException, IOException, FileNotFoundException {
		LocalDate currentDate = startTime.toLocalDate();
		LocalDate endDate = endTime.toLocalDate();
		while (currentDate.isBefore(endDate)) {
			String sDate = currentDate.toString();
			for (int i = 0; i <= 23; i++) {
				String fileName = sDate + "-" + i + ".json.gz";
				URL url = new URL(API_HOST + fileName);
				URLConnection urlConnection = url.openConnection();
				urlConnection.setRequestProperty("User-Agent",
						"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.75 Safari/537.36");
				ReadableByteChannel readableByteChannel = Channels.newChannel(urlConnection.getInputStream());
				FileOutputStream fileOutputStream = new FileOutputStream(folderName + File.separator + fileName);
				FileChannel fileChannel = fileOutputStream.getChannel();
				fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
				fileOutputStream.close();
			}
			currentDate = currentDate.plusDays(1);
		}
	}

}
