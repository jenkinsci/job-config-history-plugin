package hudson.plugins.jobConfigHistory;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.TransientProjectActionFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * Extends project actions for all jobs.
 *
 * @author Stefan Brausch
 */
@Extension
public class JobConfigHistoryActionFactory extends
        TransientProjectActionFactory {

    /** Our logger. */
    private static final Logger LOG = Logger
            .getLogger(JobConfigHistoryActionFactory.class.getName());

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<? extends Action> createFor(
            @SuppressWarnings("unchecked") AbstractProject target) {
        LOG.fine(this + " adds JobConfigHistoryProjectAction for " + target);
        final ArrayList<Action> actions = new ArrayList<Action>();
        final JobConfigHistoryProjectAction historyJobAction = target
                .getAction(JobConfigHistoryProjectAction.class);
        if (historyJobAction == null) {
            actions.add(new JobConfigHistoryProjectAction(target));
        } else {
            LOG.fine(target + " already has " + historyJobAction);
        }
        return actions;
    }

}
