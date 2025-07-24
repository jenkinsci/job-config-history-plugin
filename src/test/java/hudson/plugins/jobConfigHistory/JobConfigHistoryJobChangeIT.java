/*
 * Copyright 2025 Fritz Elfert
 */
package hudson.plugins.jobConfigHistory;

import org.htmlunit.WebAssert;
import org.htmlunit.WebClientUtil;
import org.htmlunit.html.DomNode;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.HtmlButton;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlFormUtil;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.model.FreeStyleProject;
import jenkins.model.Jenkins;

import java.util.List;
import java.util.logging.Logger;

@WithJenkins
@Execution(ExecutionMode.SAME_THREAD)
public class JobConfigHistoryJobChangeIT {

    private static final Logger LOGGER = Logger.getLogger(JobConfigHistoryJobChangeIT.class.getName());

    private static final String ALICE = "alice";
    private static final String BOB = "bob";
    private static final String CHARLIE = "charlie";

    private void initAuth(JenkinsRule j) {
        // Create users "alice", "bob" and "charlie"
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        MockAuthorizationStrategy authorizationStrategy = new MockAuthorizationStrategy();
        authorizationStrategy.grant(Jenkins.ADMINISTER).everywhere().toEveryone();
        j.jenkins.setAuthorizationStrategy(authorizationStrategy);
    }

    private String boldWhite(String txt) {
        return String.format("%s%s%s", "\033[1;97m", txt, "\033[0;37m");
    }

    private void configurePlugin(JenkinsRule j, boolean showDialog, boolean commentMandatory)
            throws Exception {
        HtmlPage p = j.createWebClient().goTo("configure");
        WebAssert.assertInputPresent(p, "_.showChangeReasonCommentWindow");
        WebAssert.assertInputPresent(p, "_.changeReasonCommentIsMandatory");
        HtmlForm form = p.getFormByName("config");
        form.getInputByName("_.showChangeReasonCommentWindow").setChecked(showDialog);
        form.getInputByName("_.changeReasonCommentIsMandatory").setChecked(commentMandatory);
        form.getInputByName("_.excludedUsers").setValue(CHARLIE);
        j.submit(form);
    }

    @Test
    public void testConfigMessageDisabled(JenkinsRule j) throws Exception {
        configurePlugin(j, false, false);
        String configUrl = j.createFreeStyleProject().getUrl() + "configure";
        HtmlPage p = j.createWebClient().goTo(configUrl);
        WebAssert.assertInputNotPresent(p, "_.changeReasonComment");
    }

    @Test
    public void testConfigMessageEnabled(JenkinsRule j) throws Exception {
        configurePlugin(j, true, false);
        String configUrl = j.createFreeStyleProject().getUrl() + "configure";
        HtmlPage p = j.createWebClient().goTo(configUrl);
        WebAssert.assertInputPresent(p, "_.changeReasonComment");
    }

    @Test
    public void testJobConfigExcludedUser(JenkinsRule j) throws Exception {
        initAuth(j);
        configurePlugin(j, true, true);
        String configUrl = j.createFreeStyleProject().getUrl() + "configure";
        HtmlPage p = j.createWebClient().login(CHARLIE).goTo(configUrl);
        WebAssert.assertInputNotPresent(p, "_.changeReasonComment");
    }

    /**
     * Test our dialog behaviour.
     *
     * @param j The JenkinsRule to be used.
     * @param p The job config page to work with.
     * @param mandatory Whether the change message is mandatory.
     * @param submit If true, the actual submit is done.
     * @param buttonLabel The label of the main form's button to be clicked.
     * @param expectedMsg The expected message of our dialog.
     *
     * @throws Exception if an assertion is thrown.
     *
     * The following actions are performed.
     * <ol>
     *   <li>click the main button with specified label
     *   <li>verify that our dialog message is the expected message
     *   <li>verify that our dialog is shown
     *   <li>verify that our dialog's ok button is enabled/disabled depending
     *       on the mandatory flag
     *   <li>enter some text in the dialog's textfield and verify that the
     *       dialog's ok button is enabled afterwards
     *   <li>click our dialog's cancel button
     *   <li>verify, that our dialog is gone.
     * </ol>
     */
    private void testDialog(JenkinsRule j, HtmlPage p, boolean mandatory, boolean submit,
            String buttonLabel, String expectedMsg) throws Exception {

        String changeMsg = String.format("Created via \"%s\"-Button with %s change-message",
                buttonLabel, mandatory ? "mandatory" : "optional");
        HtmlForm form = p.getFormByName("config");
        form.getTextAreaByName("description").setText(changeMsg);
        j.getButtonByCaption(form, buttonLabel).fireEvent("click");
        WebClientUtil.waitForJSExec(p.getWebClient());
        DomElement dlg = p.querySelector("dialog.jenkins-dialog");
        assertNotNull(dlg, "Dialog node after click on " + buttonLabel);
        assertTrue(dlg.isDisplayed(), "Dialog is visible");
        DomNode msg = dlg.querySelector("div.jenkins-dialog__title");
        assertNotNull(msg, "Dialog's message");
        assertEquals(expectedMsg, msg.getTextContent());
        HtmlButton ok = dlg.querySelector("button.jenkins-button[data-id='ok']");
        assertEquals(mandatory, ok.isDisabled(), "Dialog's ok button is disabled");
        HtmlInput input = dlg.querySelector("input.jenkins-input");
        input.setValue(changeMsg);
        input.fireEvent("input");
        WebClientUtil.waitForJSExec(p.getWebClient());

        assertFalse(ok.isDisabled(), "Dialog's ok button is disabled");
        if (submit) {
            ok.fireEvent("click");
            WebClientUtil.waitForJSExec(p.getWebClient());
            if (buttonLabel.equals("Apply")) {
                dlg = p.querySelector("dialog.jenkins-dialog");
                assertNull(dlg, "Dialog after it's ok button was clicked");
            } else {
                if (mandatory) {
                    // Form submissal would normally be done in our JS code
                    // using dispatchEvent(new Event('click')), but that
                    // triggers a JS exception in htmlunit's WebClient.
                    // Therefore we submit the form here.
                    HtmlFormUtil.submit(form);
                }
                // if we use The "Save" button to trigger the dialog, we
                // cannot check the state of the dialog after saving, because
                // submitting the form redirects to a different page.
            }
        } else {
            DomElement cancel = dlg.querySelector("button.jenkins-button[data-id='cancel']");
            assertNotNull(cancel, "Dialog's cancel button");
            cancel.fireEvent("click");
            dlg = p.querySelector("dialog.jenkins-dialog");
            assertNull(dlg, "Dialog after it was cancelled");
        }
    }

    /**
     * Test the job config page  behaviour.
     *
     * @param j The JenkinsRule to be used.
     * @param mandatory Whether the change message is mandatory.
     *
     * @throws Exception if an assertion is thrown.
     */
    private void testJobConfigPageBehaviour(JenkinsRule j, boolean mandatory, HtmlPage p)
            throws Exception {

        /*
         * - Check that we have our invisible input element
         * - Check it's various data attributes are set
         */
        WebAssert.assertInputPresent(p, "_.changeReasonComment");
        DomElement input = p.querySelector("input.change-reason-comment");
        assertNotNull(input, "input element");
        assertFalse(input.isDisplayed(), "input is visible");
        assertTrue(input.hasAttributes(), "input has attributes");
        String expectedMandatoryAttribute = mandatory ? "true" : "false";
        assertEquals(expectedMandatoryAttribute, input.getAttribute("data-mandatory"));
        assertNotEquals("", input.getAttribute("data-dialog-message"));
        assertNotEquals("", input.getAttribute("data-dialog-cancel"));
        assertNotEquals("", input.getAttribute("data-dialog-optional"));

        /* The expected message of our dialog */
        String expectedMsg = input.getAttribute("data-dialog-message");
        if (!mandatory) {
            expectedMsg += " " + input.getAttribute("data-dialog-optional");
        }

        /* Initially our dialog must not exist */
        DomElement e = p.querySelector("dialog.jenkins-dialog");
        assertNull(e, "Initial dialog");

        testDialog(j, p, mandatory, false, "Apply", expectedMsg);
        testDialog(j, p, mandatory, false, "Save", expectedMsg);
    }

    /**
     * Workaround for an odd behavior of the plugin.
     *
     * Usually, there is a superfluous entry BEFORE the Create operation
     * which makes no sense. A Create operation should always be the
     * last operation in the list.
     */
    private int getCreatedIndex(List<ConfigInfo> list) {
        for (ConfigInfo ci : list) {
            if (ci.getOperation().equals("Created")) {
                return list.indexOf(ci);
            }
        }
        throw new IllegalStateException("No \"Created\" operation in list");
    }

    @Test
    public void testConfigMessageNotMandatory(JenkinsRule j) throws Exception {
        LOGGER.info("Running " + boldWhite("#testConfigMessageNotMandatory"));
        configurePlugin(j, true, false);
        String configUrl = j.createFreeStyleProject().getUrl() + "configure";
        HtmlPage p = j.createWebClient().goTo(configUrl);
        testJobConfigPageBehaviour(j, false, p);
    }

    @Test
    public void testConfigMessageMandatory(JenkinsRule j) throws Exception {
        LOGGER.info("Running " + boldWhite("#testConfigMessageMandatory"));
        configurePlugin(j, true, true);
        String configUrl = j.createFreeStyleProject().getUrl() + "configure";
        HtmlPage p = j.createWebClient().goTo(configUrl);
        testJobConfigPageBehaviour(j, true, p);
    }

    @Test
    public void testJobConfigApplyMandatory(JenkinsRule j) throws Exception {
        LOGGER.info("Running " + boldWhite("#testJobConfigApplyMandatory"));
        configurePlugin(j, true, true);
        FreeStyleProject project = j.createFreeStyleProject();
        HtmlPage p = j.createWebClient().goTo(project.getUrl() + "configure");

        /*
         * - Check that we have our invisible input element
         * - Check it's various data attributes are set
         */
        WebAssert.assertInputPresent(p, "_.changeReasonComment");
        DomElement input = p.querySelector("input.change-reason-comment");
        assertNotNull(input, "input element");
        assertFalse(input.isDisplayed(), "input is visible");
        assertTrue(input.hasAttributes(), "input has attributes");
        String expectedMandatoryAttribute = "true";
        assertEquals(expectedMandatoryAttribute, input.getAttribute("data-mandatory"));
        assertNotEquals("", input.getAttribute("data-dialog-message"));
        assertNotEquals("", input.getAttribute("data-dialog-cancel"));
        assertNotEquals("", input.getAttribute("data-dialog-optional"));

        /* The expected message of our dialog */
        String expectedMsg = input.getAttribute("data-dialog-message");

        /* Initially our dialog must not exist */
        DomElement e = p.querySelector("dialog.jenkins-dialog");
        assertNull(e, "Initial dialog");

        testDialog(j, p, true, true, "Apply", expectedMsg);

        /* Now verify, that the history reflects our changes */
        JobConfigHistoryProjectAction ha = new JobConfigHistoryProjectAction(project);
        List<ConfigInfo> cilist = ha.getJobConfigs();
        assertEquals(1, getCreatedIndex(cilist), "Number of history entries");
        assertEquals(project.getDescription(), cilist.get(0).getChangeReasonComment(), "changeReason");
        assertEquals("unknown", cilist.get(0).getUser(), "user");
        assertEquals("unknown", cilist.get(0).getUserID(), "userId");
        assertEquals("Changed", cilist.get(0).getOperation(), "operation");
        assertEquals("SYSTEM", cilist.get(1).getUser(), "user");
        assertEquals("SYSTEM", cilist.get(1).getUserID(), "userId");
        assertEquals("Created", cilist.get(1).getOperation(), "operation");
    }

    @Test
    public void testJobConfigApplyOptional(JenkinsRule j) throws Exception {
        LOGGER.info("Running " + boldWhite("#testJobConfigApplyMandatory"));
        configurePlugin(j, true, false);
        FreeStyleProject project = j.createFreeStyleProject();
        HtmlPage p = j.createWebClient().goTo(project.getUrl() + "configure");

        /*
         * - Check that we have our invisible input element
         * - Check it's various data attributes are set
         */
        WebAssert.assertInputPresent(p, "_.changeReasonComment");
        DomElement input = p.querySelector("input.change-reason-comment");
        assertNotNull(input, "input element");
        assertFalse(input.isDisplayed(), "input is visible");
        assertTrue(input.hasAttributes(), "input has attributes");
        String expectedMandatoryAttribute = "false";
        assertEquals(expectedMandatoryAttribute, input.getAttribute("data-mandatory"));
        assertNotEquals("", input.getAttribute("data-dialog-message"));
        assertNotEquals("", input.getAttribute("data-dialog-cancel"));
        assertNotEquals("", input.getAttribute("data-dialog-optional"));

        /* The expected message of our dialog */
        String expectedMsg = input.getAttribute("data-dialog-message") + " "
            + input.getAttribute("data-dialog-optional");

        /* Initially our dialog must not exist */
        DomElement e = p.querySelector("dialog.jenkins-dialog");
        assertNull(e, "Initial dialog");

        testDialog(j, p, false, true, "Apply", expectedMsg);

        /* Now verify, that the history reflects our changes */
        JobConfigHistoryProjectAction ha = new JobConfigHistoryProjectAction(project);
        List<ConfigInfo> cilist = ha.getJobConfigs();
        assertEquals(1, getCreatedIndex(cilist), "Number of history entries");
        assertEquals(project.getDescription(), cilist.get(0).getChangeReasonComment(), "changeReason");
        assertEquals("unknown", cilist.get(0).getUser(), "user");
        assertEquals("unknown", cilist.get(0).getUserID(), "userId");
        assertEquals("Changed", cilist.get(0).getOperation(), "operation");
        assertEquals("SYSTEM", cilist.get(1).getUser(), "user");
        assertEquals("SYSTEM", cilist.get(1).getUserID(), "userId");
        assertEquals("Created", cilist.get(1).getOperation(), "operation");
    }

    @Test
    public void testJobConfigSaveMandatory(JenkinsRule j) throws Exception {
        LOGGER.info("Running " + boldWhite("#testJobConfigSaveMandatory"));
        configurePlugin(j, true, true);
        FreeStyleProject project = j.createFreeStyleProject();
        HtmlPage p = j.createWebClient().goTo(project.getUrl() + "configure");

        // This test needs to activate a flag in our JS object which
        // prevents a script exception. I blame this on the script
        // engine in htmlunit's WebClient implementation.
        p.executeJavaScript("window.jchpLib.underTest = true;");

        /*
         * - Check that we have our invisible input element
         * - Check that it's various data attributes are set
         */
        WebAssert.assertInputPresent(p, "_.changeReasonComment");
        DomElement input = p.querySelector("input.change-reason-comment");
        assertNotNull(input, "input element");
        assertFalse(input.isDisplayed(), "input is visible");
        assertTrue(input.hasAttributes(), "input has attributes");
        String expectedMandatoryAttribute = "true";
        assertEquals(expectedMandatoryAttribute, input.getAttribute("data-mandatory"));
        assertNotEquals("", input.getAttribute("data-dialog-message"));
        assertNotEquals("", input.getAttribute("data-dialog-cancel"));
        assertNotEquals("", input.getAttribute("data-dialog-optional"));

        /* The expected message of our dialog */
        String expectedMsg = input.getAttribute("data-dialog-message");

        /* Initially our dialog must not exist */
        DomElement e = p.querySelector("dialog.jenkins-dialog");
        assertNull(e, "Initial dialog");

        testDialog(j, p, true, true, "Save", expectedMsg);

        /* Now verify, that the history reflects our changes */
        JobConfigHistoryProjectAction ha = new JobConfigHistoryProjectAction(project);
        List<ConfigInfo> cilist = ha.getJobConfigs();
        assertEquals(1, getCreatedIndex(cilist), "Number of history entries");
        assertEquals(project.getDescription(), cilist.get(0).getChangeReasonComment(), "changeReason");
        assertEquals("unknown", cilist.get(0).getUser(), "user");
        assertEquals("unknown", cilist.get(0).getUserID(), "userId");
        assertEquals("Changed", cilist.get(0).getOperation(), "operation");
        assertEquals("SYSTEM", cilist.get(1).getUser(), "user");
        assertEquals("SYSTEM", cilist.get(1).getUserID(), "userId");
        assertEquals("Created", cilist.get(1).getOperation(), "operation");
    }
}
