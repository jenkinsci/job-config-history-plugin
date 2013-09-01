package hudson.plugins.jobConfigHistory;

import hudson.XmlFile;
import hudson.model.AbstractItem;
import hudson.model.Item;
import java.io.File;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.mockito.Mockito.*;

/**
 *
 * @author Mirko Friedenhagen
 */
public class JobConfigHistoryJobListenerTest {

    final JobConfigHistory mockedPlugin = mock(JobConfigHistory.class);

    final ConfigHistoryListenerHelper mockedConfigHistoryListenerHelper = mock(ConfigHistoryListenerHelper.class);

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder(new File("target"));

    /**
     * Test of onCreated method, of class JobConfigHistoryJobListener.
     */
    @Test
    public void testOnCreated() {
        Item item = mock(Item.class);
        when(item.toString()).thenReturn("item");
        JobConfigHistoryJobListener sut = new JobConfigHistoryJobListenerWithMocks();
        sut.onCreated(item);
        verifyZeroInteractions(mockedConfigHistoryListenerHelper);
    }

    /**
     * Test of onCreated method, of class JobConfigHistoryJobListener.
     */
    @Test
    public void testOnCreatedAbstractItem() {
        AbstractItem item = mock(AbstractItem.class);
        when(item.toString()).thenReturn("item");
        when(item.getConfigFile()).thenReturn(null);
        JobConfigHistoryJobListener sut = new JobConfigHistoryJobListenerWithMocks();
        sut.onCreated(item);
        verify(mockedConfigHistoryListenerHelper).createNewHistoryEntry(any(XmlFile.class));
    }

    /**
     * Test of onRenamed method, of class JobConfigHistoryJobListener.
     */
    @Test
    public void testOnRenamed() {
        Item item = mock(Item.class);
        JobConfigHistoryJobListener sut = new JobConfigHistoryJobListenerWithMocks();
        sut.onRenamed(item, "", "");
        verifyZeroInteractions(mockedConfigHistoryListenerHelper);
    }

    /**
     * Test of onRenamed method, of class JobConfigHistoryJobListener.
     */
    @Test
    public void testOnRenamedAbstractItemWithoutConfiguredHistoryRootDir() {
        AbstractItem item = mock(AbstractItem.class);
        when(item.getConfigFile()).thenReturn(null);
        JobConfigHistoryJobListener sut = new JobConfigHistoryJobListenerWithMocks();
        sut.onRenamed(item, "", "");
        verify(mockedConfigHistoryListenerHelper).createNewHistoryEntry(any(XmlFile.class));
    }

    /**
     * Test of onRenamed method, of class JobConfigHistoryJobListener.
     */
    @Test
    public void testOnRenamedAbstractItemWithConfiguredHistoryRootDir() throws IOException {
        AbstractItem item = mock(AbstractItem.class);
        when(item.getConfigFile()).thenReturn(null);
        when(mockedPlugin.getConfiguredHistoryRootDir()).thenReturn(tempFolder.getRoot());
        when(mockedPlugin.getHistoryDir(any(XmlFile.class))).thenReturn(tempFolder.newFolder("oldName"));
        JobConfigHistoryJobListener sut = new JobConfigHistoryJobListenerWithMocks();
        sut.onRenamed(item, "oldName", "newName");
        verify(mockedConfigHistoryListenerHelper).createNewHistoryEntry(any(XmlFile.class));
    }

    /**
     * Test of onDeleted method, of class JobConfigHistoryJobListener.
     */
    @Test
    public void testOnDeleted() {
        Item item = mock(Item.class);
        JobConfigHistoryJobListener sut = new JobConfigHistoryJobListenerWithMocks();
        sut.onDeleted(item);
        verifyZeroInteractions(mockedConfigHistoryListenerHelper);
    }

    /**
     * Test of onDeleted method, of class JobConfigHistoryJobListener.
     */
    @Test
    public void testOnDeletedAbstractItem() throws IOException {
        AbstractItem item = mock(AbstractItem.class);
        when(item.getConfigFile()).thenReturn(null);
        when(mockedPlugin.getHistoryDir(any(XmlFile.class))).thenReturn(tempFolder.newFile());
        JobConfigHistoryJobListener sut = new JobConfigHistoryJobListenerWithMocks();
        sut.onDeleted(item);
        verify(mockedConfigHistoryListenerHelper).createNewHistoryEntry(any(XmlFile.class));
    }

    private class JobConfigHistoryJobListenerWithMocks extends JobConfigHistoryJobListener {

        @Override
        JobConfigHistory getPlugin() {
            return mockedPlugin;
        }

        @Override
        ConfigHistoryListenerHelper getConfigHistoryListenerHelper(String operationName) {
            return mockedConfigHistoryListenerHelper;
        }
    }

}
