/*
 * Copyright 2010 Mirko Friedenhagen
 */
package hudson.plugins.jobConfigHistory;

import org.apache.commons.io.FileUtils;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class will delete the instance dir of Jenkins after tearDown to avoid an
 * overflowing tmpdir.
 *
 * @author mfriedenhagen
 */
public abstract class AbstractHudsonTestCaseDeletingInstanceDir
        extends
        JenkinsRule {

    /**
     * Our logger.
     */
    private static final Logger LOG = Logger.getLogger(
            AbstractHudsonTestCaseDeletingInstanceDir.class.getName());

    static {
        new File("target/tmp").mkdir();
        System.setProperty("java.io.tmpdir", "target/tmp");
        // Possible workaround for
        // https://issues.jenkins-ci.org/browse/JENKINS-19244
        // jenkins random hang during startup - Solaris and Linux
        System.setProperty("hudson.model.Hudson.parallelLoad", "false");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void after() throws Exception {
        super.after();
        final File rootDir = jenkins.getRootDir();
        LOG.log(Level.INFO, "Deleting {0} in tearDown", rootDir);
        FileUtils.deleteDirectory(rootDir);
    }
}
