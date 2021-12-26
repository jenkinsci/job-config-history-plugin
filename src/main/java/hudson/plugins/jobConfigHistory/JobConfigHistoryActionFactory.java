/*
 * The MIT License
 *
 * Copyright 2013 Stefan Brausch.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.jobConfigHistory;

import hudson.Extension;
import hudson.model.AbstractItem;
import hudson.model.Action;
import jenkins.model.TransientActionFactory;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Extends project actions for all jobs.
 *
 * @author Stefan Brausch
 */
@Extension
public class JobConfigHistoryActionFactory
        extends
        TransientActionFactory<AbstractItem> {

    /**
     * Our logger.
     */
    private static final Logger LOG = Logger
            .getLogger(JobConfigHistoryActionFactory.class.getName());

    @Override
    public Class<AbstractItem> type() {
        return AbstractItem.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<? extends Action> createFor(@Nonnull AbstractItem target) {
        final JobConfigHistoryProjectAction newAction = new JobConfigHistoryProjectAction(
                target);
        LOG.log(Level.FINE, "{0} adds {1} for {2}",
                new Object[]{this, newAction, target});
        return Collections.singleton(newAction);
    }
}
