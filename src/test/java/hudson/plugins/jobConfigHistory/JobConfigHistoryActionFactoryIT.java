package hudson.plugins.jobConfigHistory;

import hudson.model.AbstractProject;
import hudson.model.Action;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration Tests for JobConfigHistoryActionFactory.
 *
 * @author Mirko Friedenhagen
 */
@WithJenkins
class JobConfigHistoryActionFactoryIT {

    private final AbstractProject<?, ?> mockedTarget = mock(
            AbstractProject.class);

    private JenkinsRule rule;

    @BeforeEach
    void setUp(JenkinsRule j) {
        rule = j;
    }

    /**
     * Test of createFor method, of class JobConfigHistoryActionFactory.
     */
    @Test
    void testCreateFor() {
        JobConfigHistoryActionFactory sut = new JobConfigHistoryActionFactory();
        Collection<? extends Action> result = sut.createFor(mockedTarget);
        assertEquals(1, result.size());
    }

    /**
     * Test of createFor method, of class JobConfigHistoryActionFactory.
     */
    @Test
    void testCreateForWithAction() {
        final List<JobConfigHistoryProjectAction> actionList = Collections.singletonList(mock(JobConfigHistoryProjectAction.class));
        when(mockedTarget.getActions(JobConfigHistoryProjectAction.class))
                .thenReturn(actionList);
        JobConfigHistoryActionFactory sut = new JobConfigHistoryActionFactory();
        Collection<? extends Action> result = sut.createFor(mockedTarget);
        assertEquals(1, result.size());
        assertNotEquals(actionList, result);
    }
}
