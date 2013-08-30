package hudson.plugins.jobConfigHistory;

import hudson.model.AbstractItem;
import hudson.model.ItemGroup;
import java.io.File;
import java.io.UnsupportedEncodingException;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.*;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author Mirko Friedenhagen
 */
public class ConfigInfoTest {

    private final ItemGroup itemGroupMock = mock(ItemGroup.class);

    private final AbstractItem itemMock = mock(AbstractItem.class);

    private final File file = new File("");
    
    private final HistoryDescr historyDescr = new HistoryDescr("Firstname Lastname", "userId", "operation", "123");

    public ConfigInfoTest() {
        when(itemGroupMock.getFullName()).thenReturn("does not matter parent");
        when(itemMock.getParent()).thenReturn(itemGroupMock);
        when(itemMock.getFullName()).thenReturn("does not matter item");
    }

    /**
     * Test of create method, of class ConfigInfo.
     */
    @Test
    public void testCreate_3args() throws UnsupportedEncodingException {
        ConfigInfo sut = ConfigInfo.create(itemMock, file, historyDescr);
        assertNotNull(sut);
        assertEquals("Firstname Lastname", sut.getUser());
        assertEquals("userId", sut.getUserID());
        assertEquals("operation", sut.getOperation());
        assertEquals("123", sut.getDate());
        assertEquals(true, sut.getIsJob());
    }

    /**
     * Test of create method, of class ConfigInfo.
     */
    @Test
    public void testCreate_4args() throws UnsupportedEncodingException {
        ConfigInfo sut = ConfigInfo.create("jobName", file, historyDescr, false);
        assertNotNull(sut);
        assertEquals(false, sut.getIsJob());
    }

    /**
     * Test of toString method, of class ConfigInfo.
     */
    @Test
    public void testToString() throws UnsupportedEncodingException {        
        ConfigInfo sut = ConfigInfo.create("jobName", file, historyDescr, false);
        String result = sut.toString();
        assertThat(result, startsWith("operation on "));
    }

}
