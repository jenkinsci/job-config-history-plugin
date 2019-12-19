package hudson.plugins.jobConfigHistory;

import static org.junit.Assert.assertEquals;

import hudson.model.User;
import hudson.security.ACL;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class UserFacadeTest {


    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    private User user;

    private UserFacade sutUser;
    private UserFacade sutNameId = createSut("SYSTEM", "SYSTEM");
    private UserFacade sutUserNull = createSut(null);
    private UserFacade sutNameIdNullNull = createSut(null,null);

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

    private UserFacade createSut(User user) { return new UserFacade(user); }

    private UserFacade createSut(String id, String fullName) { return new UserFacade(id, fullName); }

    private User getSystemUser() { return jenkinsRule.getInstance().getUser(ACL.SYSTEM_USERNAME); }

}
