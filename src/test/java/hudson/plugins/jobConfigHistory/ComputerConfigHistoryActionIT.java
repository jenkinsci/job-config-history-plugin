package hudson.plugins.jobConfigHistory;

import hudson.model.Slave;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Integration tests of ComputerConfigHistoryAction.
 *
 * @author Greg Fogelberg
 */
public class ComputerConfigHistoryActionIT {

    @ClassRule
    public static JenkinsRule rule = new JenkinsRule();

    private final Slave agentMock = mock(Slave.class);

    @Test
    public void testGetDisplayName() {
        ComputerConfigHistoryAction sut = new ComputerConfigHistoryActionImpl();
        String expResult = "Agent Config History";
        String result = sut.getDisplayName();
        assertEquals(expResult, result);
    }

    public class ComputerConfigHistoryActionImpl
            extends
            ComputerConfigHistoryAction {

        public ComputerConfigHistoryActionImpl() {
            super(agentMock);
        }

    }

}
