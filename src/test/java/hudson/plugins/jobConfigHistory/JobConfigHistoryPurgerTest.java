package hudson.plugins.jobConfigHistory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author Mirko Friedenhagen
 */
public class JobConfigHistoryPurgerTest {

	final JobConfigHistory mockedPlugin = mock(JobConfigHistory.class);
	final Purgeable mockedDao = mock(Purgeable.class);
	final OverviewHistoryDao mockedOverviewDao = mock(OverviewHistoryDao.class);

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder(new File("target"));

	/**
	 * Test of getRecurrencePeriod method, of class JobConfigHistoryPurger.
	 */
	@Test
	public void testGetRecurrencePeriod() {
		JobConfigHistoryPurger sut = new JobConfigHistoryPurger(mockedPlugin,
				mockedDao, mockedOverviewDao);
		long expResult = 24 * 60 * 60 * 1000;
		long result = sut.getRecurrencePeriod();
		assertEquals(expResult, result);
	}

	/**
	 * Test of doRun method, of class JobConfigHistoryPurger.
	 */
	@Test
	public void testDoRun() throws Exception {
		when(mockedPlugin.getMaxDaysToKeepEntries()).thenReturn("1");
		JobConfigHistoryPurgerWithoutPurging sut = new JobConfigHistoryPurgerWithoutPurging(
				mockedPlugin, mockedDao, mockedOverviewDao);
		sut.doRun();
		assertTrue(sut.purgeCalled);
	}

	/**
	 * Test of doRun method, of class JobConfigHistoryPurger.
	 */
	@Test
	public void testDoRunNegative() throws Exception {
		when(mockedPlugin.getMaxDaysToKeepEntries()).thenReturn("-1");
		JobConfigHistoryPurgerWithoutPurging sut = new JobConfigHistoryPurgerWithoutPurging(
				mockedPlugin, mockedDao, mockedOverviewDao);
		sut.doRun();
		assertFalse(sut.purgeCalled);
	}

	/**
	 * Test of doRun method, of class JobConfigHistoryPurger.
	 */
	@Test
	public void testDoRunNoNumber() throws Exception {
		when(mockedPlugin.getMaxDaysToKeepEntries()).thenReturn("A");
		JobConfigHistoryPurgerWithoutPurging sut = new JobConfigHistoryPurgerWithoutPurging(
				mockedPlugin, mockedDao, mockedOverviewDao);
		sut.doRun();
		assertFalse(sut.purgeCalled);
	}

	/**
	 * Test of doRun method, of class JobConfigHistoryPurger.
	 */
	@Test
	public void testDoRunEmpty() throws Exception {
		when(mockedPlugin.getMaxDaysToKeepEntries()).thenReturn("");
		JobConfigHistoryPurgerWithoutPurging sut = new JobConfigHistoryPurgerWithoutPurging(
				mockedPlugin, mockedDao, mockedOverviewDao);
		sut.doRun();
		assertFalse(sut.purgeCalled);
	}

	/**
	 * Test of purgeSystemOrJobHistory method, of class JobConfigHistoryPurger.
	 */
	@Test
	public void testPurgeSystemOrJobHistory() throws IOException {
		JobConfigHistoryPurger sut = new JobConfigHistoryPurger(mockedPlugin,
				mockedDao, mockedOverviewDao);
		sut.setMaxAge(1);
		final File oldItemDir = tempFolder
				.newFolder(getFormattedDate(twoDaysAgo()));
		new File(oldItemDir, JobConfigHistoryConsts.HISTORY_FILE)
				.createNewFile();
		final File newItemDir = tempFolder.newFolder(getFormattedDate(now()));
		new File(newItemDir, JobConfigHistoryConsts.HISTORY_FILE)
				.createNewFile();
		File[] itemDirs = {tempFolder.getRoot()};
		sut.purgeSystemOrJobHistory(itemDirs);
		assertFalse(oldItemDir.exists());
		assertTrue(newItemDir.exists());
	}

	/**
	 * Test of purgeSystemOrJobHistory method, of class JobConfigHistoryPurger.
	 */
	@Test
	public void testPurgeSystemOrJobHistoryNoItems() throws IOException {
		JobConfigHistoryPurger sut = new JobConfigHistoryPurger(mockedPlugin,
				mockedDao, mockedOverviewDao);
		sut.setMaxAge(1);
		File[] itemDirs = {};
		sut.purgeSystemOrJobHistory(itemDirs);
		sut.purgeSystemOrJobHistory(null);
	}

	/**
	 * Test of purgeSystemOrJobHistory method, of class JobConfigHistoryPurger.
	 */
	@Test
	public void testPurgeSystemOrJobHistoryItemIsAFile() throws IOException {
		JobConfigHistoryPurger sut = new JobConfigHistoryPurger(mockedPlugin,
				mockedDao, mockedOverviewDao);
		sut.setMaxAge(1);
		final File newFile = tempFolder.newFile(getFormattedDate(now()));
		File[] itemDirs = {newFile};
		sut.purgeSystemOrJobHistory(itemDirs);
		assertTrue(newFile.exists());
	}

	/**
	 * Test of purgeSystemOrJobHistory method, of class JobConfigHistoryPurger.
	 */
	@Test
	public void testPurgeSystemOrJobHistoryItemHasNoHistory()
			throws IOException {
		JobConfigHistoryPurger sut = new JobConfigHistoryPurger(mockedPlugin,
				mockedDao, mockedOverviewDao);
		sut.setMaxAge(1);
		final File newFolder = tempFolder
				.newFolder(getFormattedDate(twoDaysAgo()));
		File[] itemDirs = {newFolder};
		sut.purgeSystemOrJobHistory(itemDirs);
		assertTrue(newFolder.exists());
	}

	/**
	 * Test of isTooOld method, of class JobConfigHistoryPurger.
	 */
	@Test
	public void testIsNotTooOld() {
		assertFalse(testIsOlderThanOneDay(now()));
	}

	/**
	 * Test of isTooOld method, of class JobConfigHistoryPurger.
	 */
	@Test
	public void testIsTooOld() {
		assertTrue(testIsOlderThanOneDay(twoDaysAgo()));
	}

	/**
	 * Test of isTooOld method, of class JobConfigHistoryPurger.
	 */
	@Test
	public void testIsTooOldInvalidFormat() {
		JobConfigHistoryPurger sut = new JobConfigHistoryPurger(mockedPlugin,
				mockedDao, mockedOverviewDao);
		sut.setMaxAge(1);
		assertFalse(sut.isTooOld(new File("invalid format")));
	}

	/**
	 * Test of deleteDirectory method, of class JobConfigHistoryPurger.
	 */
	@Test
	public void testDeleteDirectoryWithWarnings() {
		File dirMock = mock(File.class);
		when(dirMock.listFiles())
				.thenReturn(new File[]{new File("ABC"), new File("DEF")});
		JobConfigHistoryPurger sut = new JobConfigHistoryPurger(mockedPlugin,
				mockedDao, mockedOverviewDao);
		sut.deleteDirectory(dirMock);
	}

	/**
	 * Test of deleteDirectory method, of class JobConfigHistoryPurger.
	 */
	@Test
	public void testDeleteDirectory() throws IOException {
		final File newFile = tempFolder.newFile();
		assertTrue(newFile.exists());
		File dir = tempFolder.getRoot();
		JobConfigHistoryPurger sut = new JobConfigHistoryPurger(mockedPlugin,
				mockedDao, mockedOverviewDao);
		sut.deleteDirectory(dir);
		assertFalse(newFile.exists());
		assertFalse(dir.exists());
	}

	private boolean testIsOlderThanOneDay(final Date date) {
		JobConfigHistoryPurger sut = new JobConfigHistoryPurger(mockedPlugin,
				mockedDao, mockedOverviewDao);
		sut.setMaxAge(1);
		File historyDir = new File(getFormattedDate(date));
		return sut.isTooOld(historyDir);
	}

	private Date twoDaysAgo() {
		return new Date(System.currentTimeMillis()
				- TimeUnit.MILLISECONDS.convert(2, TimeUnit.DAYS));
	}

	private String getFormattedDate(Date date) {
		final SimpleDateFormat dateParser = new SimpleDateFormat(
				JobConfigHistoryConsts.ID_FORMATTER);
		return dateParser.format(date);
	}

	private Date now() {
		return new Date();
	}

	private static class JobConfigHistoryPurgerWithoutPurging
			extends
				JobConfigHistoryPurger {

		boolean purgeCalled = false;

		public JobConfigHistoryPurgerWithoutPurging(JobConfigHistory plugin,
				Purgeable purgeable, OverviewHistoryDao overviewHistoryDao) {
			super(plugin, purgeable, overviewHistoryDao);
		}

		@Override
		void purgeHistoryByAge() {
			purgeCalled = true;
		}
	}

}
