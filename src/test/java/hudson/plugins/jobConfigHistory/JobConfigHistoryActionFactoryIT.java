package hudson.plugins.jobConfigHistory;

import hudson.model.AbstractProject;
import hudson.model.Action;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration Tests for JobConfigHistoryActionFactory.
 *
 * @author Mirko Friedenhagen
 */
public class JobConfigHistoryActionFactoryIT {

    @ClassRule
    public static JenkinsRule rule = new JenkinsRule();

    private final AbstractProject<?, ?> mockedTarget = mock(
            AbstractProject.class);

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
        final List<JobConfigHistoryProjectAction> actionList = Collections.singletonList(mock(JobConfigHistoryProjectAction.class));
        when(mockedTarget.getActions(JobConfigHistoryProjectAction.class))
                .thenReturn(actionList);
        JobConfigHistoryActionFactory sut = new JobConfigHistoryActionFactory();
        Collection<? extends Action> result = sut.createFor(mockedTarget);
        assertEquals(1, result.size());
        assertNotEquals(actionList, result);
    }
}
