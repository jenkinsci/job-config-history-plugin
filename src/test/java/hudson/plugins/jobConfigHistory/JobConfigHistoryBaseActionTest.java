package hudson.plugins.jobConfigHistory;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.kohsuke.stapler.StaplerRequest;

import hudson.security.AccessControlled;
import jenkins.model.Jenkins;

/**
 *
 * @author Mirko Friedenhagen
 */
public class JobConfigHistoryBaseActionTest {

	private final Jenkins jenkinsMock = mock(Jenkins.class);
	private final StaplerRequest staplerRequestMock = mock(
			StaplerRequest.class);

	/**
	 * Test of getDisplayName method, of class JobConfigHistoryBaseAction.
	 */
	@Test
	public void testGetDisplayName() {
		JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
		String expResult = "Job Config History";
		String result = sut.getDisplayName();
		assertEquals(expResult, result);
	}

	/**
	 * Test of getUrlName method, of class JobConfigHistoryBaseAction.
	 */
	@Test
	public void testGetUrlName() {
		JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
		String expResult = "jobConfigHistory";
		String result = sut.getUrlName();
		assertEquals(expResult, result);
	}

	/**
	 * Test of getOutputType method, of class JobConfigHistoryBaseAction.
	 */
	@Test
	public void testGetOutputTypeXml() {
		JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
		when(staplerRequestMock.getParameter("type")).thenReturn("xml");
		String expResult = "xml";
		String result = sut.getOutputType();
		assertEquals(expResult, result);
	}

	/**
	 * Test of getOutputType method, of class JobConfigHistoryBaseAction.
	 */
	@Test
	public void testGetOutputTypeOther() {
		JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
		when(staplerRequestMock.getParameter("type"))
				.thenReturn("does not matter");
		String expResult = "plain";
		String result = sut.getOutputType();
		assertEquals(expResult, result);
	}
	/**
	 * Test of checkTimestamp method, of class JobConfigHistoryBaseAction.
	 */
	@Test
	public void testCheckTimestamp() {
		JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
		assertFalse(sut.checkTimestamp("null"));
		assertFalse(sut.checkTimestamp(null));
		assertTrue(sut.checkTimestamp("2013-08-31_23-59-59"));
	}

	/**
	 * Test of getRequestParameter method, of class JobConfigHistoryBaseAction.
	 */
	@Test
	public void testGetRequestParameter() {
		JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
		final String parameterName = "type";
		when(staplerRequestMock.getParameter(parameterName)).thenReturn("xml");
		String expResult = "xml";
		String result = sut.getRequestParameter(parameterName);
		assertEquals(expResult, result);
	}

	/**
	 * Test of checkConfigurePermission method, of class
	 * JobConfigHistoryBaseAction.
	 */
	@Test
	public void testCheckConfigurePermission() {
		JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
		sut.checkConfigurePermission();
	}

	/**
	 * Test of hasConfigurePermission method, of class
	 * JobConfigHistoryBaseAction.
	 */
	@Test
	public void testHasConfigurePermission() {
		JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
		boolean expResult = false;
		boolean result = sut.hasConfigurePermission();
		assertEquals(expResult, result);
	}

	/**
	 * Test of getJenkins method, of class JobConfigHistoryBaseAction.
	 */
	@Test
	public void testGetJenkins() {
		JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
		Jenkins expResult = jenkinsMock;
		Jenkins result = sut.getJenkins();
		assertEquals(expResult, result);
	}

	/**
	 * Test of getAccessControlledObject method, of class
	 * JobConfigHistoryBaseAction.
	 */
	@Test
	public void testGetAccessControlledObject() {
		JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
		AccessControlled expResult = null;
		AccessControlled result = sut.getAccessControlledObject();
		assertEquals(expResult, result);
	}

	/**
	 * Test of getDiffLines method, of class JobConfigHistoryBaseAction.
	 */
	@Test
	public void testGetDiffLines() throws Exception {
		final String resourceName = "diff.txt";
		final List<String> lines = TUtils.readResourceLines(resourceName);
		JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
		List<SideBySideView.Line> result = sut.getDiffLines(lines);
		assertEquals(24, result.size());
	}

	/**
	 * Test of getDiffAsString method, of class JobConfigHistoryBaseAction.
	 */
	@Test
	public void testGetDiffAsString() throws IOException {
		String result = testGetDiffAsString("file1.txt", "file2.txt");
		assertThat(result, endsWith("@@ -1,1 +1,1 @@\n-a\n+b\n"));
		assertThat(result, containsString("--- "));
		assertThat(result, containsString("+++ "));
	}

	/**
	 * Test of getDiffAsString method, of class JobConfigHistoryBaseAction.
	 */
	@Test
	public void testGetDiffAsStringOfEqualFiles() throws IOException {
		String result = testGetDiffAsString("file1.txt", "file1.txt");
		assertEquals("\n", result);
	}

	private String testGetDiffAsString(final String file1txt,
			final String file2txt) throws IOException {
		File file1 = new File(JobConfigHistoryBaseActionTest.class
				.getResource(file1txt).getPath());
		File file2 = new File(JobConfigHistoryBaseActionTest.class
				.getResource(file2txt).getPath());
		String[] file1Lines = TUtils.readResourceLines(file1txt)
				.toArray(new String[]{});
		String[] file2Lines = TUtils.readResourceLines(file2txt)
				.toArray(new String[]{});
		JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
		return sut.getDiffAsString(file1, file2, file1Lines, file2Lines);
	}

	public class JobConfigHistoryBaseActionImpl
			extends
				JobConfigHistoryBaseAction {

		public JobConfigHistoryBaseActionImpl() {
			super(jenkinsMock);
		}

		public void checkConfigurePermission() {
		}

		public boolean hasConfigurePermission() {
			return false;
		}

		public AccessControlled getAccessControlledObject() {
			return null;
		}

		public String getIconFileName() {
			throw new UnsupportedOperationException("Not supported yet.");
		}

		public  List<SideBySideView.Line> getLines(boolean useRegex) { throw new UnsupportedOperationException("Not supported yet."); }

		@Override
		protected StaplerRequest getCurrentRequest() {
			return staplerRequestMock;
		}
	}

}
