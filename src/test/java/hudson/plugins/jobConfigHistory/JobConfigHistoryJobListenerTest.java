package hudson.plugins.jobConfigHistory;

import hudson.model.AbstractItem;
import hudson.model.Item;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Mirko Friedenhagen
 */
public class JobConfigHistoryJobListenerTest {

    final ItemListenerHistoryDao mockedConfigHistoryListenerHelper = mock(
            ItemListenerHistoryDao.class);

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
        verify(mockedConfigHistoryListenerHelper).renameItem(item, "oldName",
                "newName");
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
     * Test of onDeleted method, of class JobConfigHistoryJobListener.
     */
    @Test
    public void testOnDeletedAbstractItem() {
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

    private class JobConfigHistoryJobListenerWithMocks
            extends
            JobConfigHistoryJobListener {

        @Override
        ItemListenerHistoryDao getHistoryDao() {
            return mockedConfigHistoryListenerHelper;
        }
    }

}
