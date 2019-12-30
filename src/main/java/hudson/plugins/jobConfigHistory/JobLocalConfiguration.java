package hudson.plugins.jobConfigHistory;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

public class JobLocalConfiguration extends JobProperty<Job<?, ?>> {

	@Extension
	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();


	//TODO maybe optionally mandatory (configurable globally!)
	private String changeReasonComment;


	@DataBoundConstructor
	public JobLocalConfiguration(String  changeReasonComment) {
		System.out.println("crc: " + changeReasonComment);
		this.changeReasonComment = changeReasonComment;
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return DESCRIPTOR;
	}

	/** Constructor loads previously saved form data.  TODO maybe not do this.*/
	public static final class DescriptorImpl extends JobPropertyDescriptor {
		DescriptorImpl() {
			super(JobLocalConfiguration.class);
			System.out.println("DESCRIPTOR IMPLE");
			load();
		}

		@Override
		public String getDisplayName() {
			return "aaaaaJOBCONFIGHISTORY!";
		}

		public boolean isApplicable(AbstractProject<?, ?> item) {
			System.out.println("ISAPPLICABLE not yet implemented! " + item.getFullName());
			return true;
		}
	}
}
