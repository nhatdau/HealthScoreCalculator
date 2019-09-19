package ai.quod.challenge.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Repository on GitHub model
 * 
 * @author nhatdau
 *
 */
public class RepoStatistic implements Serializable {
	private long id;
	private String org;
	private String name;
	private long num_commits = 0;
	private double average_num_commits = 0;
	private Set<Long> contributors = new HashSet<Long>();
	private Map<Long, Issue> issues = new HashMap<Long, Issue>();
	private double average_issue_open_time = 0;
	private float health_score;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getOrg() {
		return org;
	}

	public void setOrg(String org) {
		this.org = org;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getNum_commits() {
		return num_commits;
	}

	public void setNum_commits(long num_commits) {
		this.num_commits = num_commits;
	}

	public double getAverage_num_commits() {
		return average_num_commits;
	}

	public void setAverage_num_commits(double average_num_commits) {
		this.average_num_commits = average_num_commits;
	}

	public float getHealth_score() {
		return health_score;
	}

	public Set<Long> getContributors() {
		return contributors;
	}

	public void setContributors(Set<Long> contributors) {
		this.contributors = contributors;
	}

	public Map<Long, Issue> getIssues() {
		return issues;
	}

	public void setIssues(Map<Long, Issue> issues) {
		this.issues = issues;
	}

	public double getAverage_issue_open_time() {
		return average_issue_open_time;
	}

	public void setAverage_issue_open_time(double average_issue_open_time) {
		this.average_issue_open_time = average_issue_open_time;
	}

	public void setHealth_score(float health_score) {
		this.health_score = health_score;
	}
}
