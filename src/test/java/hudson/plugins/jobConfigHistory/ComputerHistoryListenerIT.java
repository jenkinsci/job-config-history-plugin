/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hudson.plugins.jobConfigHistory;

import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import hudson.model.Slave;
import java.util.SortedMap;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import static org.junit.Assert.*;

/**
 *
 * @author lucinka
 */
public class ComputerHistoryListenerIT {
    
    @Rule
    public JenkinsRule rule = new JenkinsRule();
    
    @Test
    public void testOnConfigurationChange_create() throws Exception{
       Slave slave1 = rule.createOnlineSlave();
       HistoryDaoBackend dao = PluginUtils.getHistoryDao();
       SortedMap<String, HistoryDescr> revisions = dao.getRevisions(slave1);
       assertNotNull("Revisions should exists.", revisions);
       assertFalse("Revisions should not be empty.", revisions.isEmpty());
       assertEquals("Revisions should contains 1 revision.", 1, revisions.size());
       String firstKey = revisions.firstKey();
       HistoryDescr descr = dao.getRevisions(slave1).get(firstKey);
       assertEquals("Revisoins should have status created.", Messages.ConfigHistoryListenerHelper_CREATED(), descr.getOperation());
    }
    
    @Test
    public void testOnConfigurationChange_rename() throws Exception{
        Slave slave1 = rule.createOnlineSlave();
        Slave slave2 = rule.createOnlineSlave();
        Slave slave3 = rule.createOnlineSlave();
        HtmlForm form = rule.createWebClient().getPage(slave2, "configure").getFormByName("config");
        HtmlInput element = form.getInputByName("_.name");
        element.setValueAttribute("newSlaveName");
        rule.submit(form);
        slave2 = (Slave) rule.jenkins.getNode("newSlaveName");
        HistoryDaoBackend dao = PluginUtils.getHistoryDao();
        assertEquals("Revisions of " + slave1.getNodeName() + " should contains 1 revision.", 1, dao.getRevisions(slave1).size());
        assertEquals("Revisions of " + slave2.getNodeName() + " should contains 2 revision.", 2, dao.getRevisions(slave2).size());
        assertEquals("Revisions of " + slave3.getNodeName() + " should contains 1 revision.", 1, dao.getRevisions(slave3).size());
        String key = dao.getRevisions(slave2).lastKey();
        assertEquals("The last revision of slave " + slave2.getNodeName() + " should have state renamed.", Messages.ConfigHistoryListenerHelper_RENAMED(), dao.getRevisions(slave2).get(key).getOperation());
    }
    
    @Test
    public void testOnConfigurationChange_delete() throws Exception{
        Slave slave1 = rule.createOnlineSlave();
        Slave slave2 = rule.createOnlineSlave();
        Slave slave3 = rule.createOnlineSlave();
        rule.jenkins.removeNode(slave2);
        HistoryDaoBackend dao = PluginUtils.getHistoryDao();
        assertEquals("Revisions of " + slave1.getNodeName() + " should contains 1 revision.", 1, dao.getRevisions(slave1).size());
        assertEquals(slave2.getNodeName() + " should have anyrevision.", 0, dao.getRevisions(slave2).size());
        assertEquals("Revisions of " + slave3.getNodeName() + " should contains 1 revision.", 1, dao.getRevisions(slave3).size());
    }
    
    @Test
    public void testOnConfigurationChange_change() throws Exception{
        Slave slave1 = rule.createOnlineSlave();
        Slave slave2 = rule.createOnlineSlave();
        Slave slave3 = rule.createOnlineSlave();
        HtmlForm form = rule.createWebClient().getPage(slave2, "configure").getFormByName("config");
        HtmlInput element = form.getInputByName("_.nodeDescription");
        element.setValueAttribute("Node description");
        rule.submit(form);
        HistoryDaoBackend dao = PluginUtils.getHistoryDao();
        assertEquals("Revisions of " + slave1.getNodeName() + " should contains 1 revision.", 1, dao.getRevisions(slave1).size());
        assertEquals("Revisions of " + slave2.getNodeName() + " should contains 2 revision.", 2, dao.getRevisions(slave2).size());
        assertEquals("Revisions of " + slave3.getNodeName() + " should contains 1 revision.", 1, dao.getRevisions(slave3).size());
        String key = dao.getRevisions(slave2).lastKey();
        assertEquals("The last revision of slave " + slave2.getNodeName() + " should have state changed.", Messages.ConfigHistoryListenerHelper_CHANGED(), dao.getRevisions(slave2).get(key).getOperation());
    }
    
}
