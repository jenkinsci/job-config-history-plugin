package hudson.plugins.jobConfigHistory;

import hudson.model.Slave;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Integration tests of ComputerConfigHistoryAction.
 *
 * @author Greg Fogelberg
 */
@WithJenkins
@Execution(ExecutionMode.SAME_THREAD)
class ComputerConfigHistoryActionIT {

    private final Slave agentMock = mock(Slave.class);

    private JenkinsRule rule;

    @BeforeEach
    void setUp(JenkinsRule j) {
        rule = j;
    }

    @Test
    void testGetDisplayName() {
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
