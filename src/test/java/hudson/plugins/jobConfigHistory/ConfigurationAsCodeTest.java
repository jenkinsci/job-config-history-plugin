package hudson.plugins.jobConfigHistory;

import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import io.jenkins.plugins.casc.model.CNode;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;

import static io.jenkins.plugins.casc.misc.Util.getUnclassifiedRoot;
import static io.jenkins.plugins.casc.misc.Util.toStringFromYamlFile;
import static io.jenkins.plugins.casc.misc.Util.toYamlString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@WithJenkinsConfiguredWithCode
class ConfigurationAsCodeTest {

    @Issue("JENKINS-55667")
    @Test
    @ConfiguredWithCode("configuration-as-code.yml")
    void should_support_configuration_as_code(JenkinsConfiguredWithCodeRule r) {
        JobConfigHistory plugin = PluginUtils.getPlugin();
        assertEquals("/var/jenkins_home", plugin.getHistoryRootDir());
        assertEquals("30", plugin.getMaxDaysToKeepEntries());
        assertEquals("20", plugin.getMaxEntriesPerPage());
        assertEquals("10000", plugin.getMaxHistoryEntries());
        assertEquals("test1\\.xml|test2\\.xml", plugin.getExcludePattern());
        assertEquals("test1\\.xml|test2\\.xml", plugin.getExcludeRegexpPattern().pattern());
        assertEquals("SYSTEM,user1,user2", plugin.getExcludedUsers());
        assertEquals("never", plugin.getShowBuildBadges());
        assertTrue(plugin.getShowChangeReasonCommentWindow());
        assertTrue(plugin.getSkipDuplicateHistory());
        assertFalse(plugin.getSaveModuleConfiguration());
        assertFalse(plugin.getChangeReasonCommentIsMandatory());
    }

    @Issue("JENKINS-55667")
    @Test
    @ConfiguredWithCode("configuration-as-code.yml")
    void should_support_configuration_export(JenkinsConfiguredWithCodeRule r) throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CNode jobConfigHistoryAttribute = getUnclassifiedRoot(context).get("jobConfigHistory");

        String exported = toYamlString(jobConfigHistoryAttribute);

        String expected = toStringFromYamlFile(this, "configuration-as-code-expected.yml");

        assertEquals(expected, exported);
    }

    @Issue("JENKINS-55667")
    @Test
    void should_support_configuration_export_default(JenkinsConfiguredWithCodeRule r) throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CNode jobConfigHistoryAttribute = getUnclassifiedRoot(context).get("jobConfigHistory");

        String exported = toYamlString(jobConfigHistoryAttribute);

        String expected = toStringFromYamlFile(this, "configuration-as-code-default-expected.yml");

        assertEquals(expected, exported);
    }
}
