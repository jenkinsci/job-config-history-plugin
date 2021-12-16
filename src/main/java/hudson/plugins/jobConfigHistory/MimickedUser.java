package hudson.plugins.jobConfigHistory;

import hudson.model.User;

/**
 * Avoids the need for having a real user initialized when all we need is the name and the id.
 * User existence is not guaranteed nor checked.
 */
public class MimickedUser {

    private final String id;
    private final String fullName;

    public MimickedUser(User user) {
        this(
                (user != null) ? user.getId() : JobConfigHistoryConsts.UNKNOWN_USER_NAME,
                (user != null) ? user.getFullName() : JobConfigHistoryConsts.UNKNOWN_USER_ID);
    }

    public MimickedUser(String id, String fullName) {
        this.id = id;
        this.fullName = fullName;
    }

    /**
     * @return {@link User#getId()}
     */
    public String getId() {
        return id;
    }

    /**
     * @return a string matching {@link User#getFullName()}
     */
    public String getFullName() {
        return fullName;
    }

    public User getUser(boolean create) {
        return User.getById(getId(), create);
    }
}


