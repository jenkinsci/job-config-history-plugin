package hudson.plugins.jobConfigHistory;

import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.model.CNode;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

import static io.jenkins.plugins.casc.misc.Util.getUnclassifiedRoot;
import static io.jenkins.plugins.casc.misc.Util.toStringFromYamlFile;
import static io.jenkins.plugins.casc.misc.Util.toYamlString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConfigurationAsCodeTest {
    
    @Rule
    public JenkinsConfiguredWithCodeRule r = new JenkinsConfiguredWithCodeRule();

    @Issue("JENKINS-55667")
    @Test
    @ConfiguredWithCode("configuration-as-code.yml")
    public void should_support_configuration_as_code() {
        JobConfigHistory plugin = PluginUtils.getPlugin();
        assertEquals("/var/jenkins_home", plugin.getHistoryRootDir());
        assertEquals("30", plugin.getMaxDaysToKeepEntries());
        assertEquals("20", plugin.getMaxEntriesPerPage());
        assertEquals("10000", plugin.getMaxHistoryEntries());
        assertEquals("test1\\.xml|test2\\.xml", plugin.getExcludePattern());
        assertEquals("SYSTEM,user1,user2", plugin.getExcludedUsers());
        assertEquals("never", plugin.getShowBuildBadges());
        assertTrue(!plugin.getShowChangeReasonCommentWindow());
        assertTrue(!plugin.getSkipDuplicateHistory());
        assertTrue(!plugin.getSaveModuleConfiguration());
    }

    @Issue("JENKINS-55667")
    @Test
    @ConfiguredWithCode("configuration-as-code.yml")
    public void should_support_configuration_export() throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CNode jobConfigHistoryAttribute = getUnclassifiedRoot(context).get("jobConfigHistory");

        String exported = toYamlString(jobConfigHistoryAttribute);

        String expected = toStringFromYamlFile(this, "configuration-as-code-expected.yml");

        assertEquals(expected, exported);
    }

    @Issue("JENKINS-55667")
    @Test
    public void should_support_configuration_export_default() throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CNode jobConfigHistoryAttribute = getUnclassifiedRoot(context).get("jobConfigHistory");

        String exported = toYamlString(jobConfigHistoryAttribute);

        String expected = toStringFromYamlFile(this, "configuration-as-code-default-expected.yml");

        assertEquals(expected, exported);
    }
}
