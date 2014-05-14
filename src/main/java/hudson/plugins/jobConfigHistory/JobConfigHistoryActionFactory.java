package hudson.plugins.jobConfigHistory;

import hudson.Extension;
import hudson.model.AbstractItem;
import hudson.model.Action;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.TransientActionFactory;

/**
 * Extends project actions for all jobs.
 *
 * @author Stefan Brausch
 */
@Extension
public class JobConfigHistoryActionFactory extends TransientActionFactory<AbstractItem> {


    /** Our logger. */
    private static final Logger LOG = Logger.getLogger(JobConfigHistoryActionFactory.class.getName());
    
    @Override public Class<AbstractItem> type() {
        return AbstractItem.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<? extends Action> createFor(AbstractItem target) {
        final JobConfigHistoryProjectAction newAction = new JobConfigHistoryProjectAction(target);
        LOG.log(Level.FINE, "{0} adds {1} for {2}", new Object[] {this, newAction, target});
        return Collections.singleton(newAction);
    }
}