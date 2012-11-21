/*
 * JobConfigChangeBadge.java Nov 21, 2012
 * 
 * Copyright (c) 2012 1&1 Internet AG. All rights reserved.
 * 
 * $Id$
 */
package hudson.plugins.jobConfigHistory;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import jenkins.model.Jenkins;

import hudson.Extension;
import hudson.XmlFile;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildBadgeAction;
import hudson.model.Hudson;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

@Extension
public class JobConfigChangeBadge extends RunListener<AbstractBuild> implements BuildBadgeAction {

    private static final Logger LOG = Logger.getLogger(JobConfigChangeBadge.class.getName());
    
    public JobConfigChangeBadge() {
        super(AbstractBuild.class);
    }

    @Override
    public void onStarted(AbstractBuild build, TaskListener listener) {
        //testen, ob in config angeschaltet?
        //vergleichen: timestamp letzter build, timestamp letzter config-change
        AbstractProject project = (AbstractProject)build.getProject();
        AbstractBuild b = (AbstractBuild)project.getLastBuild();
        LOG.finest("AAAAAAAA: " + b.getTime());
        
        //timestamp config-change
        final ArrayList<ConfigInfo> configs = new ArrayList<ConfigInfo>();
        final File historyRootDir = Hudson.getInstance().getPlugin(JobConfigHistory.class).getHistoryDir(project.getConfigFile());
        try {
            if (historyRootDir.exists()) {
                for (final File historyDir : historyRootDir.listFiles(JobConfigHistory.HISTORY_FILTER)) {
                    final XmlFile historyXml = new XmlFile(new File(historyDir, JobConfigHistoryConsts.HISTORY_FILE));
                    final HistoryDescr histDescr = (HistoryDescr) historyXml.read();
                    final ConfigInfo config = ConfigInfo.create(project, historyDir, histDescr);
                    configs.add(config);
                }
            }
        } catch (Exception ex) {
            
        }

        Collections.sort(configs, ConfigInfoComparator.INSTANCE);
        ConfigInfo lastChange = Collections.min(configs, ConfigInfoComparator.INSTANCE);
        LOG.finest("CCCCCCCCCCCCCCCCC - " + lastChange.getDate());
        //2012-11-07_18-03-12
        
        try {
            Date date = DateFormat.getDateInstance().parse(lastChange.getDate());
            LOG.finest("DDDDDDDDDDDDDDDDDDDDDD - " +  date);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
        }
        
        //Datumswerte von Build und Config vergleichen
        
        
        super.onStarted(build, listener);
    }

    
    
    // non use interface methods
    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return "Huhu";
    }

    public String getUrlName() {
        return "";
    }
}
