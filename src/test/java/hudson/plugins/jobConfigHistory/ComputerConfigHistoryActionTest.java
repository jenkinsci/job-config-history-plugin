package hudson.plugins.jobConfigHistory;

import hudson.model.Slave;
import static org.junit.Assert.*;
import org.junit.Test;
import static org.mockito.Mockito.mock;

/**
 *
 * @author Greg Fogelberg
 */
public class ComputerConfigHistoryActionTest {

    private final Slave agentMock = mock(Slave.class);

    /**
     * Test of getDisplayName method, of class ComputerConfigHistoryAction.
     */
    @Test
    public void testGetDisplayName() {
        ComputerConfigHistoryAction sut = new ComputerConfigHistoryActionImpl();
        String expResult = "Agent Config History";
        String result = sut.getDisplayName();
        assertEquals(expResult, result);
    }

    public class ComputerConfigHistoryActionImpl extends ComputerConfigHistoryAction {

        public ComputerConfigHistoryActionImpl() {
            super(agentMock);
        }

    }

}
