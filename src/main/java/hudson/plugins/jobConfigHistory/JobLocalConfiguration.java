package hudson.plugins.jobConfigHistory;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;

import java.text.Normalizer;

public class JobLocalConfiguration extends JobProperty<Job<?, ?>> {
//
//	@Extension
//	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();


	//TODO maybe optionally mandatory (configurable globally!)
	private String changeReasonComment;


	@DataBoundConstructor
	public JobLocalConfiguration(String  changeReasonComment) {
		System.out.println("crc: " + changeReasonComment);

		this.changeReasonComment = changeReasonComment;
	}

	public String getChangeReasonComment() {
		return changeReasonComment;
	}
//
//	@Override
//	public DescriptorImpl getDescriptor() {
//		return DESCRIPTOR;
//	}
//


	/** Constructor loads previously saved form data.  TODO maybe not do this.*/
	@Extension
	public static class DescriptorImpl extends JobPropertyDescriptor {

		public DescriptorImpl() {
			super(JobLocalConfiguration.class);
			System.out.println("DESCRIPTOR IMPLE");
			load();
		}

		@Override
		public String getDisplayName() {
			return "aaaaaJOBCONFIGHISTORY!";
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
