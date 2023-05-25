package hudson.plugins.jobConfigHistory;

import hudson.model.AbstractItem;
import hudson.model.ItemGroup;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Mirko Friedenhagen
 */
public class ConfigInfoTest {

    private static final String DATE = "2012-11-21_11-29-12";

    private final HistoryDescr historyDescr = new HistoryDescr(
            "Firstname Lastname", "userId", "operation", DATE, null, null);

    public ConfigInfoTest() {
        ItemGroup<?> itemGroupMock = mock(ItemGroup.class);
        when(itemGroupMock.getFullName()).thenReturn("does not matter parent");
        AbstractItem itemMock = mock(AbstractItem.class);
        when(itemMock.getParent()).thenReturn(itemGroupMock);
        when(itemMock.getFullName()).thenReturn("Job1");
    }

    /**
     * Test of create method, of class ConfigInfo.
     */
    @Test
    public void testCreate_4args() {
        ConfigInfo sut = ConfigInfo.create("jobName", true, historyDescr,
                false);
        assertNotNull(sut);
        assertFalse(sut.getIsJob());
    }

    /**
     * Test of toString method, of class ConfigInfo.
     */
    @Test
    public void testToString() {
        ConfigInfo sut = ConfigInfo.create("jobName", true, historyDescr,
                false);
        String result = sut.toString();
        assertThat(result, startsWith("operation on "));
    }

    /**
     * Test of parsedDate method, of class ConfigInfo.
     */
    @Test
    public void testParsedDate() {
        // "2012-11-21_11-29-12"
        ConfigInfo sut = ConfigInfo.create("jobName", true, historyDescr,
                false);
        Date expResult = new Date(112, Calendar.NOVEMBER, 21, 11, 29, 12);
        Date result = sut.parsedDate();
        assertEquals(expResult, result);
    }
}
