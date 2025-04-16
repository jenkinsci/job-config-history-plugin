/*
 * Copyright 2025 Fritz Elfert
 */
package hudson.plugins.jobConfigHistory;


import org.htmlunit.WebAssert;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@WithJenkins
public class JobConfigHistoryJobChangeIT {

    @Test
    public void testConfigMessageDisabled(JenkinsRule j) throws Exception {
        HtmlPage p = j.createWebClient().goTo("configure");
        WebAssert.assertInputPresent(p, "_.showChangeReasonCommentWindow");
        WebAssert.assertInputPresent(p, "_.changeReasonCommentIsMandatory");
        final HtmlForm form = p.getFormByName("config");
        form.getInputByName("_.showChangeReasonCommentWindow")
                .setChecked(false);
        form.getInputByName("_.changeReasonCommentIsMandatory")
                .setChecked(false);
        j.submit(form);
        j.createFreeStyleProject("newjob");
        p = j.createWebClient().goTo("job/newjob/configure");
        WebAssert.assertInputNotPresent(p, "_.changeReasonComment");
    }

    @Test
    public void testConfigMessageEnabled(JenkinsRule j) throws Exception {
        HtmlPage p = j.createWebClient().goTo("configure");
        WebAssert.assertInputPresent(p, "_.showChangeReasonCommentWindow");
        WebAssert.assertInputPresent(p, "_.changeReasonCommentIsMandatory");
        final HtmlForm form = p.getFormByName("config");
        form.getInputByName("_.showChangeReasonCommentWindow")
                .setChecked(true);
        form.getInputByName("_.changeReasonCommentIsMandatory")
                .setChecked(false);
        j.submit(form);
        j.createFreeStyleProject("newjob");
        p = j.createWebClient().goTo("job/newjob/configure");
        WebAssert.assertInputPresent(p, "_.changeReasonComment");
    }

    @Test
    public void testConfigMessageNotMandatory(JenkinsRule j) throws Exception {
        HtmlPage p = j.createWebClient().goTo("configure");
        WebAssert.assertInputPresent(p, "_.showChangeReasonCommentWindow");
        WebAssert.assertInputPresent(p, "_.changeReasonCommentIsMandatory");
        HtmlForm form = p.getFormByName("config");
        form.getInputByName("_.showChangeReasonCommentWindow")
                .setChecked(true);
        form.getInputByName("_.changeReasonCommentIsMandatory")
                .setChecked(false);
        j.submit(form);
        j.createFreeStyleProject("newjob");
        p = j.createWebClient().goTo("job/newjob/configure");
        WebAssert.assertInputPresent(p, "_.changeReasonComment");
        form = p.getFormByName("config");
        HtmlInput input = form.getInputByName("_.changeReasonComment");
        input.setValue("");
        input.fireEvent("change");
        assertFalse(j.getButtonByCaption(form, "Save").isDisabled(), "Save button enabled");
        assertFalse(j.getButtonByCaption(form, "Apply").isDisabled(), "Apply button enabled");
    }

    @Test
    public void testConfigMessageMandatory(JenkinsRule j) throws Exception {
        HtmlPage p = j.createWebClient().goTo("configure");
        WebAssert.assertInputPresent(p, "_.showChangeReasonCommentWindow");
        WebAssert.assertInputPresent(p, "_.changeReasonCommentIsMandatory");
        HtmlForm form = p.getFormByName("config");
        form.getInputByName("_.showChangeReasonCommentWindow")
                .setChecked(true);
        form.getInputByName("_.changeReasonCommentIsMandatory")
                .setChecked(true);
        j.submit(form);
        j.createFreeStyleProject("newjob");
        p = j.createWebClient().goTo("job/newjob/configure");
        WebAssert.assertInputPresent(p, "_.changeReasonComment");
        form = p.getFormByName("config");
        HtmlInput input = form.getInputByName("_.changeReasonComment");
        input.setValue("");
        input.fireEvent("change");
        assertTrue(j.getButtonByCaption(form, "Save").isDisabled(), "Save button disabled");
        assertTrue(j.getButtonByCaption(form, "Apply").isDisabled(), "Apply button disabled");
        input.setValue("whatever");
        input.fireEvent("change");
        assertFalse(j.getButtonByCaption(form, "Save").isDisabled(), "Save button enabled");
        assertFalse(j.getButtonByCaption(form, "Apply").isDisabled(), "Apply button enabled");
    }
}
