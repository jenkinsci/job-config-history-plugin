package hudson.plugins.jobConfigHistory;

import hudson.model.AbstractItem;
import hudson.model.ItemGroup;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
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

    private static final String DATE = "2012-11-21_11-29-12";

    private static final String DATE_NEWER = "2012-11-21_11-29-14";

    private final HistoryDescr historyDescr = new HistoryDescr(
            "Firstname Lastname", "userId", "operation", DATE);

    private final HistoryDescr historyDescrNewer = new HistoryDescr(
            "Firstname Lastname", "userId", "operation", DATE_NEWER);

    public ConfigInfoTest() {
        when(itemGroupMock.getFullName()).thenReturn("does not matter parent");
        when(itemMock.getParent()).thenReturn(itemGroupMock);
        when(itemMock.getFullName()).thenReturn("does not matter item");
    }

    /**
     * Test of create method, of class ConfigInfo.
     */
    @Test
    public void testCreate_3args() {
        ConfigInfo sut = ConfigInfo.create(itemMock, file, historyDescr);
        assertNotNull(sut);
        assertEquals("Firstname Lastname", sut.getUser());
        assertEquals("userId", sut.getUserID());
        assertEquals("operation", sut.getOperation());
        assertEquals(DATE, sut.getDate());
        assertEquals(true, sut.getIsJob());
    }

    /**
     * Test of create method, of class ConfigInfo.
     */
    @Test
    public void testCreate_4args() {
        ConfigInfo sut = ConfigInfo.create("jobName", file, historyDescr, false);
        assertNotNull(sut);
        assertEquals(false, sut.getIsJob());
    }

    /**
     * Test of toString method, of class ConfigInfo.
     */
    @Test
    public void testToString() {
        ConfigInfo sut = ConfigInfo.create("jobName", file, historyDescr, false);
        String result = sut.toString();
        assertThat(result, startsWith("operation on "));
    }

    /**
     * Test of parsedDate method, of class ConfigInfo.
     */
    @Test
    public void testParsedDate() {
        //"2012-11-21_11-29-12"
        ConfigInfo sut = ConfigInfo.create("jobName", file, historyDescr, false);
        Date expResult = new Date(112, 10, 21, 11, 29, 12);
        Date result = sut.parsedDate();
        assertEquals(expResult, result);
    }

    /**
     * Test of compareTo method, of class ConfigInfo.
     */
    @Test
    public void testCompareTo() {
        ConfigInfo o = ConfigInfo.create("jobName", file, historyDescrNewer, false);
        ConfigInfo sut = ConfigInfo.create("jobName", file, historyDescr, false);
        final List<ConfigInfo> list = Arrays.asList(sut, o);
        Collections.sort(list);
        assertEquals(sut, list.get(1));
        assertEquals(o, list.get(0));
        assertEquals(o, Collections.min(list));
    }

}
