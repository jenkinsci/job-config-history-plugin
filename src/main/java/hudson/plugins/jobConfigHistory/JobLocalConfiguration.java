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

public class JobLocalConfiguration extends JobProperty<Job<?, ?>> {

	private String changeReasonComment;


	@DataBoundConstructor
	public JobLocalConfiguration(String  changeReasonComment) {
		System.out.println("crc: " + changeReasonComment);

		this.changeReasonComment = changeReasonComment;
	}

	public String getChangeReasonComment() {
		return changeReasonComment;
	}

	/** Constructor loads previously saved form data.  TODO maybe not do this.*/
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
			System.out.println("CONFIGURE");
			throw new FormException("form exception", "localValues.changeReasonComment");
		}


		public FormValidation doCheckChangeReasonComment(@QueryParameter String changeReasonComment, @AncestorInPath Item item) {
			//TODO maybe use this instead of javascript. (need to figure out how to relocate the message...)
			return FormValidation.ok();
		}

	}

}
