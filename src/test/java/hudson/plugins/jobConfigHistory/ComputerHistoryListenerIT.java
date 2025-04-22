package hudson.plugins.jobConfigHistory;

import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlInput;
import hudson.model.Slave;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.SortedMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 *
 * Integration tests for ComputerHistoryListenerIT.
 *
 * @author lucinka
 */
@WithJenkins
class ComputerHistoryListenerIT {

    private JenkinsRule rule;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        this.rule = rule;
    }

    @Test
    void testOnConfigurationChange_create() throws Exception {
        Slave agentOne = rule.createOnlineSlave();
        JobConfigHistoryStrategy dao = PluginUtils.getHistoryDao();
        SortedMap<String, HistoryDescr> revisions = dao.getRevisions(agentOne);
        assertNotNull(revisions, "Revisions should exists.");
        assertFalse(revisions.isEmpty(), "Revisions should not be empty.");
        assertEquals(1,
                revisions.size(),
                "Revisions should contains 1 revision.");
        String firstKey = revisions.firstKey();
        HistoryDescr descr = dao.getRevisions(agentOne).get(firstKey);
        assertEquals(Messages.ConfigHistoryListenerHelper_CREATED(),
                descr.getOperation(),
                "Revisions should have status created.");
    }

    @Test
    void testOnConfigurationChange_rename() throws Exception {
        Slave agentOne = rule.createOnlineSlave();
        Slave agentTwo = rule.createOnlineSlave();
        Slave agentThree = rule.createOnlineSlave();
        HtmlForm form = rule.createWebClient().getPage(agentTwo, "configure")
                .getFormByName("config");
        HtmlInput element = form.getInputByName("_.name");
        element.setValue("newAgentName");
        rule.submit(form);
        agentTwo = (Slave) rule.jenkins.getNode("newAgentName");
        JobConfigHistoryStrategy dao = PluginUtils.getHistoryDao();
        assertEquals(
                1, dao.getRevisions(agentOne).size(), "Revisions of " + agentOne.getNodeName()
                        + " should contains 1 revision.");
        assertEquals(
                2, dao.getRevisions(agentTwo).size(), "Revisions of " + agentTwo.getNodeName()
                        + " should contains 2 revision.");
        assertEquals(
                1, dao.getRevisions(agentThree).size(), "Revisions of " + agentThree.getNodeName()
                        + " should contains 1 revision.");
        String key = dao.getRevisions(agentTwo).lastKey();
        assertEquals(
                Messages.ConfigHistoryListenerHelper_RENAMED(),
                dao.getRevisions(agentTwo).get(key).getOperation(),
                "The last revision of agent " + agentTwo.getNodeName()
                        + " should have state renamed.");
    }

    @Test
    void testOnConfigurationChange_delete() throws Exception {
        Slave agentOne = rule.createOnlineSlave();
        Slave agentTwo = rule.createOnlineSlave();
        Slave agentThree = rule.createOnlineSlave();
        rule.jenkins.removeNode(agentTwo);
        JobConfigHistoryStrategy dao = PluginUtils.getHistoryDao();
        assertEquals(
                1, dao.getRevisions(agentOne).size(), "Revisions of " + agentOne.getNodeName()
                        + " should contains 1 revision.");
        assertEquals(0,
                dao.getRevisions(agentTwo).size(),
                agentTwo.getNodeName() + " should have any revision.");
        assertEquals(
                1, dao.getRevisions(agentThree).size(), "Revisions of " + agentThree.getNodeName()
                        + " should contains 1 revision.");
    }

    @Test
    void testOnConfigurationChange_change() throws Exception {
        Slave agentOne = rule.createOnlineSlave();
        Slave agentTwo = rule.createOnlineSlave();
        Slave agentThree = rule.createOnlineSlave();
        HtmlForm form = rule.createWebClient().getPage(agentTwo, "configure")
                .getFormByName("config");
        HtmlInput element = form.getInputByName("_.numExecutors");
        element.setValue("5");
        rule.submit(form);
        JobConfigHistoryStrategy dao = PluginUtils.getHistoryDao();
        assertEquals(
                1, dao.getRevisions(agentOne).size(), "Revisions of " + agentOne.getNodeName()
                        + " should contains 1 revision.");
        assertEquals(
                2, dao.getRevisions(agentTwo).size(), "Revisions of " + agentTwo.getNodeName()
                        + " should contains 2 revision.");
        assertEquals(
                1, dao.getRevisions(agentThree).size(), "Revisions of " + agentThree.getNodeName()
                        + " should contains 1 revision.");
        String key = dao.getRevisions(agentTwo).lastKey();
        assertEquals(
                Messages.ConfigHistoryListenerHelper_CHANGED(),
                dao.getRevisions(agentTwo).get(key).getOperation(),
                "The last revision of agent " + agentTwo.getNodeName()
                        + " should have state changed.");
    }

}
