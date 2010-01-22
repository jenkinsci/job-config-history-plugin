package hudson.plugins.jobConfigHistory;

import hudson.XmlFile;
import hudson.model.AbstractProject;
import hudson.util.MultipartFormDataParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;

/**
 * @author Stefan Brausch
 */

public class JobConfigHistoryProjectAction extends JobConfigHistoryBaseAction {

	public JobConfigHistoryProjectAction(AbstractProject<?, ?> project) {
		super();
		this.project = project;
	}

	/** The project. */
	private final transient AbstractProject<?, ?> project;

	@Exported
	public List<ConfigInfo> getConfigs() {

		ArrayList<ConfigInfo> configs = new ArrayList<ConfigInfo>();
		if (!configs.isEmpty()) {
			configs.clear();
		}
		File dirList = new File(project.getRootDir(), "config-history");
		File[] configDirs = dirList.listFiles();
		for (int i = 0; i < configDirs.length; i++) {
			File configDir = configDirs[i];

			XmlFile myConfig = new XmlFile(new File(configDir, "history.xml"));
			HistoryDescr histDescr = null;
			try {
				histDescr = (HistoryDescr) myConfig.read();
				ConfigInfo config = new ConfigInfo();
				config.setDate(histDescr.getTimestamp());
				config.setUser(histDescr.getUser());
				config.setOperation(histDescr.getOperation());
				config.setFile(configDir.getAbsolutePath());
				configs.add(config);
			} catch (IOException e) {
				Logger.getLogger("Exception: " + e.getMessage());
			}

		}
		Collections.sort(configs, new Comparator<ConfigInfo>() {
			public int compare(ConfigInfo o1, ConfigInfo o2) {
				return o2.getDate().compareTo(o1.getDate());
			}
		});

		return configs;
	}

	/**
	 * Returns the job for which the health report will be generated.
	 *
	 * @return job
	 */
	public AbstractProject<?, ?> getProject() {
		return project;
	}

	@Exported
	public String getFile() {

		String filePath = Stapler.getCurrentRequest().getParameter("file");
		String rawFile = "not found for: " + filePath;
		XmlFile myConfig = new XmlFile(new File(filePath, "config.xml"));
		try {
			rawFile = myConfig.asString();
		} catch (IOException e) {
			Logger.getLogger("Exception: " + e.getMessage());
		}
		return rawFile;
	}

	@Exported
	public String getType() {
		return Stapler.getCurrentRequest().getParameter("type");

	}

	public boolean isRawOutput() {
		return Stapler.getCurrentRequest().getParameter("type")
				.equalsIgnoreCase("raw");

	}

	public boolean isXmlOutput() {
		return Stapler.getCurrentRequest().getParameter("type")
				.equalsIgnoreCase("xml");

	}

	public void doDiffFiles(StaplerRequest req, StaplerResponse rsp)
			throws ServletException, IOException {

		MultipartFormDataParser parser = new MultipartFormDataParser(req);
		rsp.sendRedirect("showDiffFiles?diffFile1=" + parser.get("DiffFile1")
				+ "&diffFile2=" + parser.get("DiffFile2"));

	}

	@Exported
	public String getDiffFile() {

		String diffFile1 = Stapler.getCurrentRequest()
				.getParameter("diffFile1");
		String diffFile2 = Stapler.getCurrentRequest()
				.getParameter("diffFile2");
		String diff = "\nDiffs:\n\n";
		XmlFile myConfig1 = new XmlFile(new File(diffFile1, "config.xml"));
		XmlFile myConfig2 = new XmlFile(new File(diffFile2, "config.xml"));

		try {
			// http://www.cs.princeton.edu/introcs/96optimization/Diff.java.html

			String[] x;
			x = myConfig1.asString().split("\\n");

			String[] y;

			y = myConfig2.asString().split("\\n");

			if (x != null && y != null) {
				// number of lines of each file
				int M = x.length;
				int N = y.length;

				// opt[i][j] = length of LCS of x[i..M] and y[j..N]
				int[][] opt = new int[M + 1][N + 1];

				// compute length of LCS and all subproblems via dynamic
				// programming
				for (int i = M - 1; i >= 0; i--) {
					for (int j = N - 1; j >= 0; j--) {
						if (x[i].equals(y[j]))
							opt[i][j] = opt[i + 1][j + 1] + 1;
						else
							opt[i][j] = Math.max(opt[i + 1][j], opt[i][j + 1]);
					}
				}

				// recover LCS itself and print out non-matching lines to
				// standard
				// output
				int i = 0, j = 0;
				while (i < M && j < N) {
					if (x[i].equals(y[j])) {
						i++;
						j++;
					} else if (opt[i + 1][j] >= opt[i][j + 1])
						diff += ("< " + x[i++]) + "\n";
					else
						diff += ("> " + y[j++]) + "\n";
				}

				// dump out one remainder of one string if the other is
				// exhausted
				while (i < M || j < N) {
					if (i == M)
						diff += ("> " + y[j++]) + "\n";
					else if (j == N)
						diff += ("< " + x[i++]) + "\n";
				}

			}
		} catch (IOException e) {
			Logger.getLogger("Exception: " + e.getMessage());
		}
		return diff;
	}

}
