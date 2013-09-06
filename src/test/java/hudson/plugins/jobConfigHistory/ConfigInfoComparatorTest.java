package hudson.plugins.jobConfigHistory;

import hudson.model.AbstractItem;
import hudson.model.ItemGroup;
import java.io.File;
import java.io.UnsupportedEncodingException;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author Mirko Friedenhagen
 */
public class ConfigInfoComparatorTest {

    /**
     * Test of compare method, of class ConfigInfoComparator.
     */
    @Test
    public void testCompare() throws UnsupportedEncodingException {
        ItemGroup itemGroupMock = mock(ItemGroup.class);
        when(itemGroupMock.getFullName()).thenReturn("does not matter");
        AbstractItem itemMock = mock(AbstractItem.class);
        when(itemMock.getParent()).thenReturn(itemGroupMock);
        when(itemMock.getFullName()).thenReturn("does not matter");
        HistoryDescr descrMock = mock(HistoryDescr.class);
        when(descrMock.getTimestamp()).thenReturn("2012-11-21_11-29-12");
        ConfigInfo ci1 = ConfigInfo.create(itemMock, new File(""), descrMock);
        ConfigInfo ci2 = ConfigInfo.create(itemMock, new File(""), descrMock);
        int expResult = 0;
        int result = ConfigInfoComparator.INSTANCE.compare(ci1, ci2);
        assertEquals(expResult, result);
    }

}
