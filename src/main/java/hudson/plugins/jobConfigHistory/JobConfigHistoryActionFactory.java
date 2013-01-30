package hudson.plugins.jobConfigHistory;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.TransientProjectActionFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Extends project actions for all jobs.
 *
 * @author Stefan Brausch
 */
@Extension
public class JobConfigHistoryActionFactory extends TransientProjectActionFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<? extends Action> createFor(@SuppressWarnings("unchecked") AbstractProject target) {
        List<JobConfigHistoryProjectAction> historyJobActions;
        try {
            historyJobActions = target.getActions(JobConfigHistoryProjectAction.class);
        } catch (NullPointerException e) {
            historyJobActions = null;
        }

        if (historyJobActions != null && !historyJobActions.isEmpty()) {
            return historyJobActions;
        }
        final ArrayList<Action> actions = new ArrayList<Action>();
        final JobConfigHistoryProjectAction newAction = new JobConfigHistoryProjectAction(target);
        actions.add(newAction);
        return actions;
    }
}
