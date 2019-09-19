package ai.quod.challenge.model;

/**
 * Issue on GitHub model
 * 
 * @author nhatdau
 *
 */
public class Issue {
	private long id;
	private long openTime;
	private long closeTime = 0;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getOpenTime() {
		return openTime;
	}

	public void setOpenTime(long openTime) {
		this.openTime = openTime;
	}

	public long getCloseTime() {
		return closeTime;
	}

	public void setCloseTime(long closeTime) {
		this.closeTime = closeTime;
	}

}
