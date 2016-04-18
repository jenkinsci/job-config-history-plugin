/**
 * Copyright 2010 Mirko Friedenhagen
 */

package hudson.plugins.jobConfigHistory;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.jvnet.hudson.test.HudsonTestCase;

/**
 * This class will delete the instance dir of hudson after tearDown to avoid an
 * overflowing tmpdir.
 *
 * @author mfriedenhagen
 */
public abstract class AbstractHudsonTestCaseDeletingInstanceDir extends HudsonTestCase {

    /** Our logger. */
    private static final Logger LOG = Logger.getLogger(AbstractHudsonTestCaseDeletingInstanceDir.class.getName());

    static {
        new File("target/tmp").mkdir();
        System.setProperty("java.io.tmpdir", "target/tmp");
        // Possible workaround for https://issues.jenkins-ci.org/browse/JENKINS-19244
        // jenkins random hang during startup - Solaris and Linux
        System.setProperty("hudson.model.Hudson.parallelLoad", "false");
    }

    /** {@inheritDoc} */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        final File rootDir = hudson.getRootDir();
        LOG.log(Level.INFO, "Deleting {0} in tearDown", rootDir);
        FileUtils.deleteDirectory(rootDir);
    }
}
