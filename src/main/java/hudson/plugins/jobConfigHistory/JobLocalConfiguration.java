package hudson.plugins.jobConfigHistory;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.util.logging.Logger;

public class JobLocalConfiguration extends JobProperty<Job<?, ?>> {

    private static final Logger LOG = Logger
            .getLogger(JobLocalConfiguration.class.getName());

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

        @Override
        public String getDisplayName() {
            return "changeReasonComment_holder";
        }

        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        public boolean configure(StaplerRequest request, JSONObject jsonObject) throws FormException {
            LOG.info("CONFIGURE");
            throw new FormException("form exception", "localValues.changeReasonComment");
        }

        public FormValidation doCheckChangeReasonComment(@QueryParameter String changeReasonComment, @AncestorInPath Item item) {
            //TODO maybe use this instead of javascript. (need to figure out how to relocate the message...)
            return FormValidation.ok();
        }

        public boolean getShowChangeReasonCommentWindow() {
            return PluginUtils.getPlugin().getShowChangeReasonCommentWindow();
        }
    }
}
