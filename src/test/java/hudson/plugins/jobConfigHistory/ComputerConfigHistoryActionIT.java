package hudson.plugins.jobConfigHistory;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import org.junit.ClassRule;
import org.junit.Test;

import hudson.model.Slave;
import org.jvnet.hudson.test.JenkinsRule;

/**
 *
 * @author Greg Fogelberg
 */
public class ComputerConfigHistoryActionIT {

	@ClassRule
	public static JenkinsRule rule = new JenkinsRule();

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

	public class ComputerConfigHistoryActionImpl
			extends
				ComputerConfigHistoryAction {

		public ComputerConfigHistoryActionImpl() {
			super(agentMock);
		}

	}

}
