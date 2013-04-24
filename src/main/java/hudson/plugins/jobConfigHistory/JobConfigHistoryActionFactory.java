package hudson.plugins.jobConfigHistory;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.TransientProjectActionFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * Extends project actions for all jobs.
 *
 * @author Stefan Brausch
 */
@Extension
public class JobConfigHistoryActionFactory extends TransientProjectActionFactory {


    /** Our logger. */
    private static final Logger LOG = Logger.getLogger(JobConfigHistoryActionFactory.class.getName());
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<? extends Action> createFor(@SuppressWarnings("unchecked") AbstractProject target) {
        final ArrayList<Action> actions = new ArrayList<Action>();
        final List<JobConfigHistoryProjectAction> historyJobActions = target.getActions(JobConfigHistoryProjectAction.class);
        LOG.fine(target + " already had " + historyJobActions);
        final JobConfigHistoryProjectAction newAction = new JobConfigHistoryProjectAction(target);
        actions.add(newAction);
        LOG.fine(this + " adds " + newAction + " for " + target);
        return actions;
    }
}