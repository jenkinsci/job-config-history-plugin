package hudson.plugins.jobConfigHistory;

import hudson.model.AbstractItem;
import hudson.model.Item;
import java.io.IOException;
import org.junit.Test;
import static org.mockito.Mockito.*;

/**
 *
 * @author Mirko Friedenhagen
 */
public class JobConfigHistoryJobListenerTest {

    final ItemListenerHistoryDao mockedConfigHistoryListenerHelper = mock(ItemListenerHistoryDao.class);

    final JobConfigHistoryJobListener sut = new JobConfigHistoryJobListenerWithMocks();

    /**
     * Test of onCreated method, of class JobConfigHistoryJobListener.
     */
    @Test
    public void testOnCreated() {
        Item item = createItem();
        sut.onCreated(item);
        verifyZeroInteractions(mockedConfigHistoryListenerHelper);
    }

    /**
     * Test of onCreated method, of class JobConfigHistoryJobListener.
     */
    @Test
    public void testOnCreatedAbstractItem() {
        AbstractItem item = createAbstractItem();
        sut.onCreated(item);
        verify(mockedConfigHistoryListenerHelper).createNewItem(item);
    }

    /**
     * Test of onRenamed method, of class JobConfigHistoryJobListener.
     */
    @Test
    public void testOnRenamed() {
        Item item = createItem();
        sut.onRenamed(item, "", "");
        verifyZeroInteractions(mockedConfigHistoryListenerHelper);
    }

    /**
     * Test of onRenamed method, of class JobConfigHistoryJobListener.
     */
    @Test
    public void testOnRenamedAbstractItemWithoutConfiguredHistoryRootDir() {
        AbstractItem item = createAbstractItem();
        sut.onRenamed(item, "oldName", "newName");
        verify(mockedConfigHistoryListenerHelper).renameItem(item, "oldName", "newName");
    }

    /**
     * Test of onDeleted method, of class JobConfigHistoryJobListener.
     */
    @Test
    public void testOnDeleted() {
        Item item = createItem();
        sut.onDeleted(item);
        verifyZeroInteractions(mockedConfigHistoryListenerHelper);
    }
    
    /**
     * Test of onUpdated method, of class JobConfigHistoryJobListener.
     */
    @Test
    public void testOnUpdated() {
        Item item = createItem();
        sut.onUpdated(item);
        verifyZeroInteractions(mockedConfigHistoryListenerHelper);
    }
    
    /**
     * Test of onUpdated method, of class JobConfigHistoryJobListener.
     */
    @Test
    public void testOnUpdatedAbstractItem() {
        Item item = createAbstractItem();
        sut.onUpdated(item);
        verify(mockedConfigHistoryListenerHelper).saveItem(item);
    }

    /**
     * Test of onDeleted method, of class JobConfigHistoryJobListener.
     */
    @Test
    public void testOnDeletedAbstractItem() throws IOException {
        AbstractItem item = createAbstractItem();
        sut.onDeleted(item);
        verify(mockedConfigHistoryListenerHelper).deleteItem(item);
    }

    private AbstractItem createAbstractItem() {
        AbstractItem item = mock(AbstractItem.class);
        when(item.toString()).thenReturn("abstractItem");
        when(item.getConfigFile()).thenReturn(null);
        return item;
    }

    private Item createItem() {
        Item item = mock(Item.class);
        when(item.toString()).thenReturn("item");
        return item;
    }

    private class JobConfigHistoryJobListenerWithMocks extends JobConfigHistoryJobListener {

        @Override
        ItemListenerHistoryDao getHistoryDao() {
            return mockedConfigHistoryListenerHelper;
        }
    }

}
