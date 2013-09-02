package hudson.plugins.jobConfigHistory;

import hudson.model.AbstractProject;
import hudson.model.Action;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author Mirko Friedenhagen
 */
public class JobConfigHistoryActionFactoryTest {

    private final AbstractProject mockedTarget = mock(AbstractProject.class);

    /**
     * Test of createFor method, of class JobConfigHistoryActionFactory.
     */
    @Test
    public void testCreateFor() {
        JobConfigHistoryActionFactory sut = new JobConfigHistoryActionFactory();
        Collection<? extends Action> result = sut.createFor(mockedTarget);
        assertEquals(1, result.size());
    }

    /**
     * Test of createFor method, of class JobConfigHistoryActionFactory.
     */
    @Test
    public void testCreateForWithAction() {
        final List<JobConfigHistoryProjectAction> actionList = Arrays.asList(mock(JobConfigHistoryProjectAction.class));
        when(mockedTarget.getActions(JobConfigHistoryProjectAction.class))
                .thenReturn(actionList);
        JobConfigHistoryActionFactory sut = new JobConfigHistoryActionFactory();
        Collection<? extends Action> result = sut.createFor(mockedTarget);
        assertEquals(1, result.size());
        assertNotEquals(actionList, result);
    }
}
