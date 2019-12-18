package hudson.plugins.jobConfigHistory;

import hudson.model.User;
import jenkins.model.Jenkins;

/**
 * Avoids the need for having a real user initialized when all we need is the name and the id.
 */
public class UserFacade {

	private String id;
	private String fullName;

	public UserFacade(User user) {
		this(
			(user != null) ? user.getId() : JobConfigHistoryConsts.UNKNOWN_USER_NAME,
			(user != null) ? user.getFullName() : JobConfigHistoryConsts.UNKNOWN_USER_ID);
	}

	public UserFacade(String id, String fullName) {
		this.id = id;
		this.fullName = fullName;
	}

	public String getId() { return id; }

	public String getFullName() { return fullName; }

	public User getUser(boolean create) {
		return User.getById(getId(), create);
	}
}


