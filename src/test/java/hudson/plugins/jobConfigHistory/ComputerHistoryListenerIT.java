package hudson.plugins.jobConfigHistory;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import hudson.model.Slave;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.SortedMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author lucinka
 */
public class ComputerHistoryListenerIT {

    @Rule
    public JenkinsRule rule = new JenkinsRule();

    @Test
    public void testOnConfigurationChange_create() throws Exception {
        Slave agentOne = rule.createOnlineSlave();
        JobConfigHistoryStrategy dao = PluginUtils.getHistoryDao();
        SortedMap<String, HistoryDescr> revisions = dao.getRevisions(agentOne);
        assertNotNull("Revisions should exists.", revisions);
        assertFalse("Revisions should not be empty.", revisions.isEmpty());
        assertEquals("Revisions should contains 1 revision.", 1,
                revisions.size());
        String firstKey = revisions.firstKey();
        HistoryDescr descr = dao.getRevisions(agentOne).get(firstKey);
        assertEquals("Revisions should have status created.",
                Messages.ConfigHistoryListenerHelper_CREATED(),
                descr.getOperation());
    }

    @Test
    public void testOnConfigurationChange_rename() throws Exception {
        Slave agentOne = rule.createOnlineSlave();
        Slave agentTwo = rule.createOnlineSlave();
        Slave agentThree = rule.createOnlineSlave();
        HtmlForm form = rule.createWebClient().getPage(agentTwo, "configure")
                .getFormByName("config");
        HtmlInput element = form.getInputByName("_.name");
        element.setValueAttribute("newAgentName");
        rule.submit(form);
        agentTwo = (Slave) rule.jenkins.getNode("newAgentName");
        JobConfigHistoryStrategy dao = PluginUtils.getHistoryDao();
        assertEquals(
                "Revisions of " + agentOne.getNodeName()
                        + " should contains 1 revision.",
                1, dao.getRevisions(agentOne).size());
        assertEquals(
                "Revisions of " + agentTwo.getNodeName()
                        + " should contains 2 revision.",
                2, dao.getRevisions(agentTwo).size());
        assertEquals(
                "Revisions of " + agentThree.getNodeName()
                        + " should contains 1 revision.",
                1, dao.getRevisions(agentThree).size());
        String key = dao.getRevisions(agentTwo).lastKey();
        assertEquals(
                "The last revision of agent " + agentTwo.getNodeName()
                        + " should have state renamed.",
                Messages.ConfigHistoryListenerHelper_RENAMED(),
                dao.getRevisions(agentTwo).get(key).getOperation());
    }

    @Test
    public void testOnConfigurationChange_delete() throws Exception {
        Slave agentOne = rule.createOnlineSlave();
        Slave agentTwo = rule.createOnlineSlave();
        Slave agentThree = rule.createOnlineSlave();
        rule.jenkins.removeNode(agentTwo);
        JobConfigHistoryStrategy dao = PluginUtils.getHistoryDao();
        assertEquals(
                "Revisions of " + agentOne.getNodeName()
                        + " should contains 1 revision.",
                1, dao.getRevisions(agentOne).size());
        assertEquals(agentTwo.getNodeName() + " should have any revision.", 0,
                dao.getRevisions(agentTwo).size());
        assertEquals(
                "Revisions of " + agentThree.getNodeName()
                        + " should contains 1 revision.",
                1, dao.getRevisions(agentThree).size());
    }

    @Test
    public void testOnConfigurationChange_change() throws Exception {
        Slave agentOne = rule.createOnlineSlave();
        Slave agentTwo = rule.createOnlineSlave();
        Slave agentThree = rule.createOnlineSlave();
        HtmlForm form = rule.createWebClient().getPage(agentTwo, "configure")
                .getFormByName("config");
        HtmlInput element = form.getInputByName("_.nodeDescription");
        element.setValueAttribute("Node description");
        rule.submit(form);
        JobConfigHistoryStrategy dao = PluginUtils.getHistoryDao();
        assertEquals(
                "Revisions of " + agentOne.getNodeName()
                        + " should contains 1 revision.",
                1, dao.getRevisions(agentOne).size());
        assertEquals(
                "Revisions of " + agentTwo.getNodeName()
                        + " should contains 2 revision.",
                2, dao.getRevisions(agentTwo).size());
        assertEquals(
                "Revisions of " + agentThree.getNodeName()
                        + " should contains 1 revision.",
                1, dao.getRevisions(agentThree).size());
        String key = dao.getRevisions(agentTwo).lastKey();
        assertEquals(
                "The last revision of agent " + agentTwo.getNodeName()
                        + " should have state changed.",
                Messages.ConfigHistoryListenerHelper_CHANGED(),
                dao.getRevisions(agentTwo).get(key).getOperation());
    }

}
