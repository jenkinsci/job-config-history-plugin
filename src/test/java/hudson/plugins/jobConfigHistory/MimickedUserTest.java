package hudson.plugins.jobConfigHistory;

import hudson.model.User;
import hudson.security.ACL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@WithJenkins
@Execution(ExecutionMode.SAME_THREAD)
class MimickedUserTest {

    private final MimickedUser sutNameId = createSut("SYSTEM", "SYSTEM");
    private final MimickedUser sutUserNull = createSut(null);
    private final MimickedUser sutNameIdNullNull = createSut(null, null);

    private JenkinsRule jenkinsRule;
    private User user;
    private MimickedUser sutUser;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        jenkinsRule = rule;
        user = getSystemUser();
        sutUser = createSut(user);
    }

    @Test
    void testGetId() {
        assertEquals(user.getId(), sutUser.getId());
        assertEquals(user.getId(), sutNameId.getId());

        assertEquals(JobConfigHistoryConsts.UNKNOWN_USER_ID, sutUserNull.getId());
        assertNull(sutNameIdNullNull.getId());
    }

    @Test
    void testGetFullName() {
        assertEquals(user.getFullName(), sutUser.getFullName());
        assertEquals(user.getFullName(), sutNameId.getFullName());

        assertEquals(JobConfigHistoryConsts.UNKNOWN_USER_NAME, sutUserNull.getFullName());
        assertNull(sutNameIdNullNull.getFullName());
    }

    @Test
    void testGetUser() {
        assertEquals(user, sutUser.getUser(false));
        assertEquals(user, sutNameId.getUser(false));

        assertNull(sutUserNull.getUser(false));
    }

    private MimickedUser createSut(User user) {
        return new MimickedUser(user);
    }

    private MimickedUser createSut(String id, String fullName) {
        return new MimickedUser(id, fullName);
    }

    private User getSystemUser() {
        return jenkinsRule.getInstance().getUser(ACL.SYSTEM_USERNAME);
    }

}
