package hudson.plugins.jobConfigHistory;

import hudson.model.AbstractItem;
import hudson.model.Item;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for  JobConfigHistoryJobListener.
 *
 * @author Mirko Friedenhagen
 */
class JobConfigHistoryJobListenerTest {

    final ItemListenerHistoryDao mockedConfigHistoryListenerHelper = mock(
            ItemListenerHistoryDao.class);

    final JobConfigHistoryJobListener sut = new JobConfigHistoryJobListenerWithMocks();

    @Test
    void testOnCreated() {
        Item item = createItem();
        sut.onCreated(item);
        verifyNoInteractions(mockedConfigHistoryListenerHelper);
    }

    @Test
    void testOnCreatedAbstractItem() {
        AbstractItem item = createAbstractItem();
        sut.onCreated(item);
        verify(mockedConfigHistoryListenerHelper).createNewItem(item);
    }

    @Test
    void testOnRenamed() {
        Item item = createItem();
        sut.onRenamed(item, "", "");
        verifyNoInteractions(mockedConfigHistoryListenerHelper);
    }

    @Test
    void testOnRenamedAbstractItemWithoutConfiguredHistoryRootDir() {
        AbstractItem item = createAbstractItem();
        sut.onRenamed(item, "oldName", "newName");
        verify(mockedConfigHistoryListenerHelper).renameItem(item, "oldName",
                "newName");
    }

    @Test
    void testOnDeleted() {
        Item item = createItem();
        sut.onDeleted(item);
        verifyNoInteractions(mockedConfigHistoryListenerHelper);
    }

    @Test
    void testOnDeletedAbstractItem() {
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
