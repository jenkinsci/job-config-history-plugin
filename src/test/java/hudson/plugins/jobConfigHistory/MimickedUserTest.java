package hudson.plugins.jobConfigHistory;

import static org.junit.Assert.assertEquals;

import hudson.model.User;
import hudson.security.ACL;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class MimickedUserTest {


    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    private User user;

    private MimickedUser sutUser;
    private MimickedUser sutNameId = createSut("SYSTEM", "SYSTEM");
    private MimickedUser sutUserNull = createSut(null);
    private MimickedUser sutNameIdNullNull = createSut(null,null);

    @Before
    public void setupUser() {
        user = getSystemUser();
        sutUser = createSut(user);
    }

    @Test
    public void testGetId() {
        assertEquals(user.getId(), sutUser.getId());
        assertEquals(user.getId(), sutNameId.getId());

        assertEquals(JobConfigHistoryConsts.UNKNOWN_USER_ID, sutUserNull.getId());
        assertEquals(null, sutNameIdNullNull.getId());
    }

    @Test
    public void testGetFullName() {
        assertEquals(user.getFullName(), sutUser.getFullName());
        assertEquals(user.getFullName(), sutNameId.getFullName());

        assertEquals(JobConfigHistoryConsts.UNKNOWN_USER_NAME, sutUserNull.getFullName());
        assertEquals(null, sutNameIdNullNull.getFullName());
    }

    @Test
    public void testGetUser() {
        assertEquals(user, sutUser.getUser(false));
        assertEquals(user, sutNameId.getUser(false));

        assertEquals(null, sutUserNull.getUser(false));
    }

    private MimickedUser createSut(User user) { return new MimickedUser(user); }

    private MimickedUser createSut(String id, String fullName) { return new MimickedUser(id, fullName); }

    private User getSystemUser() { return jenkinsRule.getInstance().getUser(ACL.SYSTEM_USERNAME); }

}
