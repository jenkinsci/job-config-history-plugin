/*
 * Copyright 2010 Mirko Friedenhagen
 */
package hudson.plugins.jobConfigHistory;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class will delete the instance dir of Jenkins after tearDown to avoid an
 * overflowing tmpdir.
 *
 * @author mfriedenhagen
 */
@WithJenkins
abstract class AbstractHudsonTestCaseDeletingInstanceDir {

    private static final Logger LOG = Logger.getLogger(
            AbstractHudsonTestCaseDeletingInstanceDir.class.getName());

    protected JenkinsRule rule;

    static {
        new File("target/tmp").mkdir();
        System.setProperty("java.io.tmpdir", "target/tmp");
        // Possible workaround for
        // https://issues.jenkins-ci.org/browse/JENKINS-19244
        // jenkins random hang during startup - Solaris and Linux
        System.setProperty("hudson.model.Hudson.parallelLoad", "false");
    }

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        this.rule = rule;
    }

    @AfterEach
    void tearDown() throws Exception {
        final File rootDir = rule.jenkins.getRootDir();
        LOG.log(Level.INFO, "Deleting {0} in tearDown", rootDir);
        FileUtils.deleteDirectory(rootDir);
    }
}
