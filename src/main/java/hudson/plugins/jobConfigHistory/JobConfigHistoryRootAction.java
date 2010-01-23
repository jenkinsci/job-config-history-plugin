package hudson.plugins.jobConfigHistory;

import hudson.Extension;
import hudson.XmlFile;
import hudson.model.Hudson;
import hudson.model.RootAction;
import hudson.util.RunList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

/**
 *
 * @author Stefan Brausch, mfriedenhagen
 */

@Extension
public class JobConfigHistoryRootAction extends JobConfigHistoryBaseAction
        implements RootAction {

    /**
     * {@inheritDoc}
     *
     * This actions always starts from the context directly, so prefix
     * {@link JsConsts} with a slash.
     */
    @Override
    public String getUrlName() {
        return "/" + JobConfigHistoryConsts.URLNAME;
    }

    /**
     * Returns some or all known runs of this hudson instance, depending on
     * parameter count.
     *
     * @param request
     *            evalutes parameter <tt>count</tt>
     * @return runlist
     */
    public RunList getRunList(StaplerRequest request) {
        final RunList allRuns = new RunList(Hudson.getInstance()
                .getPrimaryView());
        final String countParameter = request.getParameter("count");
        if (countParameter == null) {
            return allRuns;
        } else {
            final int count = Integer.valueOf(countParameter);
            if (count > allRuns.size()) {
                return allRuns;
            } else {
                final RunList runList = new RunList();
                for (int i = 0; i < count; i++) {
                    runList.add(allRuns.get(i));
                }
                return runList;
            }
        }
    }

    @Exported
    public List<ConfigInfo> getConfigs() {
        ArrayList<ConfigInfo> configs = new ArrayList<ConfigInfo>();
        File jobList = new File(Hudson.getInstance().getRootDir(), "jobs");
        File[] jobDirs = jobList.listFiles();
        for (int j = 0; j < jobDirs.length; j++) {
            try {
                File dirList = new File(jobDirs[j], "config-history");
                File[] configDirs = dirList.listFiles();
                for (int i = 0; i < configDirs.length; i++) {
                    File configDir = configDirs[i];

                    XmlFile myConfig = new XmlFile(new File(configDir,
                            "history.xml"));
                    HistoryDescr histDescr = new HistoryDescr("", "", "", "");
                    try {
                        histDescr = (HistoryDescr) myConfig.read();
                    } catch (IOException e) {

                        Logger.getLogger("IO-Exception: " + e.getMessage());
                    }

                    ConfigInfo config = new ConfigInfo();
                    config.setDate(histDescr.getTimestamp());
                    config.setUser(histDescr.getUser());
                    config.setUserId(histDescr.getUserID());
                    config.setOperation(histDescr.getOperation());
                    config.setJob(jobDirs[j].getName());
                    config.setFile(configDir.getAbsolutePath());
                    configs.add(config);

                }
            } catch (Exception e) {

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
     * See {@link JobConfigHistoryBaseAction#getConfigFileContent()}.
     *
     * @return content of the file.
     */
    @Exported
    public String getFile() {
        return getConfigFileContent();
    }

    @Exported
    public String getType() {
        return Stapler.getCurrentRequest().getParameter("type");
    }

}
