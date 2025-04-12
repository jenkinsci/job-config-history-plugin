package hudson.plugins.jobConfigHistory;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.Util;
import hudson.XmlFile;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.util.FormValidation;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest2;

import java.util.logging.Logger;

public class JobLocalConfiguration extends JobProperty<Job<?, ?>> {

    private static final Logger LOG = Logger
            .getLogger(JobLocalConfiguration.class.getName());

    private static final Map<File, String> lastChangeReasonCommentByXmlFile = Collections.synchronizedMap(new HashMap<>());

    static Optional<String> lastChangeReasonComment(XmlFile file) {
        return Optional.ofNullable(lastChangeReasonCommentByXmlFile.remove(file.getFile()));
    }

    private final String changeReasonComment;


    @DataBoundConstructor
    public JobLocalConfiguration(String changeReasonComment) {
        this.changeReasonComment = changeReasonComment;
    }

    public String getChangeReasonComment() {
        return "";
    }

    @Extension
    public static class DescriptorImpl extends JobPropertyDescriptor {

        public DescriptorImpl() {
            super(JobLocalConfiguration.class);
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "changeReasonComment_holder";
        }

        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        @Override
        @SuppressFBWarnings(value = "NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", justification = "JavaDoc says it is always Non-null")
        public JobProperty<?> newInstance(StaplerRequest2 req, JSONObject formData) throws FormException {
            JobLocalConfiguration jp = (JobLocalConfiguration) super.newInstance(req, formData);
            Job<?, ?> job = req.findAncestorObject(Job.class);
            if (job != null) {
                lastChangeReasonCommentByXmlFile.put(job.getConfigFile().getFile(), Util.fixEmptyAndTrim(jp.changeReasonComment));
            }
            return null;
        }

        public boolean configure(StaplerRequest2 request, JSONObject jsonObject) throws FormException {
            LOG.info("CONFIGURE");
            throw new FormException("form exception", "localValues.changeReasonComment");
        }

        public FormValidation doCheckChangeReasonComment(@QueryParameter String changeReasonComment, @AncestorInPath Item item) {
            //TODO maybe use this instead of javascript. (need to figure out how to relocate the message...)
            if (getChangeReasonCommentIsMandatory() && null == Util.fixEmptyAndTrim(changeReasonComment)) {
                return FormValidation.error("Missing change reason");
            }
            return FormValidation.ok();
        }

        public boolean getShowChangeReasonCommentWindow() {
            return PluginUtils.getPlugin().getShowChangeReasonCommentWindow();
        }

        public boolean getChangeReasonCommentIsMandatory() {
            return PluginUtils.getPlugin().getChangeReasonCommentIsMandatory();
        }
    }
}
