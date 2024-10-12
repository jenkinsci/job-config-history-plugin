package hudson.plugins.jobConfigHistory;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.XmlFile;
import hudson.model.Saveable;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for JobConfigHistorySaveableListener.
 *
 * @author Mirko Friedenhagen
 */
public class JobConfigHistorySaveableListenerTest {

    private final HistoryDao mockedConfigHistoryListenerHelper = mock(
            HistoryDao.class);

    private final JobConfigHistory mockedPlugin = mock(JobConfigHistory.class);

    @Test
    public void testOnChangeNotSaveable() {
        when(mockedPlugin.isSaveable(any(Saveable.class), any(XmlFile.class)))
                .thenReturn(false);
        JobConfigHistorySaveableListener sut = new JobConfigHistorySaveableListenerImpl();
        sut.onChange(null, null);
        verifyNoInteractions(mockedConfigHistoryListenerHelper);
    }

    @Test
    public void testOnChangeSaveable() {
        when(mockedPlugin.isSaveable(null, null))
                .thenReturn(true);
        JobConfigHistorySaveableListener sut = new JobConfigHistorySaveableListenerImpl();
        sut.onChange(null, null);
        verify(mockedConfigHistoryListenerHelper).saveItem(null);
    }

    private class JobConfigHistorySaveableListenerImpl
            extends
            JobConfigHistorySaveableListener {

        @Override
        @NonNull
        JobConfigHistory getPlugin() {
            return mockedPlugin;
        }

        @Override
        HistoryDao getHistoryDao(JobConfigHistory plugin) {
            return mockedConfigHistoryListenerHelper;
        }
    }
}
