package hudson.plugins.jobConfigHistory;

import hudson.XmlFile;
import hudson.model.Saveable;
import org.junit.Test;
import static org.mockito.Mockito.*;

/**
 *
 * @author Mirko Friedenhagen
 */
public class JobConfigHistorySaveableListenerTest {

    private final HistoryDao mockedConfigHistoryListenerHelper = mock(HistoryDao.class);

    private final JobConfigHistory mockedPlugin = mock(JobConfigHistory.class);

    /**
     * Test of onChange method, of class JobConfigHistorySaveableListener.
     */
    @Test
    public void testOnChangeNotSaveable() {
        when(mockedPlugin.isSaveable(any(Saveable.class), any(XmlFile.class))).thenReturn(false);
        JobConfigHistorySaveableListener sut = new JobConfigHistorySaveableListenerImpl();
        sut.onChange(null, null);
        verifyZeroInteractions(mockedConfigHistoryListenerHelper);
    }

    /**
     * Test of onChange method, of class JobConfigHistorySaveableListener.
     */
    @Test
    public void testOnChangeSaveable() {
        when(mockedPlugin.isSaveable(any(Saveable.class), any(XmlFile.class))).thenReturn(true);
        JobConfigHistorySaveableListener sut = new JobConfigHistorySaveableListenerImpl();
        sut.onChange(null, null);
        verify(mockedConfigHistoryListenerHelper).saveItem(any(XmlFile.class));
    }

    private class JobConfigHistorySaveableListenerImpl extends JobConfigHistorySaveableListener {

        @Override
        JobConfigHistory getPlugin() {
            return mockedPlugin;
        }

        @Override
        HistoryDao getHistoryDao(JobConfigHistory plugin) {
            return mockedConfigHistoryListenerHelper;
        }
    }

}
