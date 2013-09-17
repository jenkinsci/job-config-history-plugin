/*
 * The MIT License
 *
 * Copyright 2013 Mirko Friedenhagen.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.plugins.jobConfigHistory;

import hudson.XmlFile;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Collects all configs of a special type. For Jobs these follow the pattern:
 * <tt>config-history/jobs/FOLDERNAME/JOBNAME/TIMESTAMP</tt>,
 * where <tt>FOLDERNAME</tt> may be empty.
 *
 * Extracted from {@link JobConfigHistoryRootAction} for easier testability.
 *
 * @author Mirko Friedenhagen.
 */
final class ConfigInfoCollector {

    /**
     * outparameter.
     */
    private final List<ConfigInfo> configs = new ArrayList<ConfigInfo>();

    /**
     * type to collect.
     */
    private final String type;


    /**
     * Collects configs of the given type.
     *
     * @param type may be one of deleted, created or jobs?
     */
    public ConfigInfoCollector(String type) {
        this.type = type;
    }

    /**
     * Gets config history entries for the view options 'created', 'deleted' and
     * 'jobs'. While 'jobs' displays all available job config history entries,
     * 'deleted' and 'created' only show the last or the first one respectively.
     *
     * @param itemDir
     *            The job directory as File
     * @param folderName
     *            Something Jesse Glick came up with but never documented, probably the folderName.
     * @return List of ConfigInfo, may be empty
     * @throws IOException
     *             If one of the entries cannot be read.
     */
    void getConfigsForType(File itemDir, String folderName) throws IOException {
        final File[] historyDirs = itemDir.listFiles(HistoryFileFilter.INSTANCE);
        if (historyDirs.length == 0) {
            return;
        }
        Arrays.sort(historyDirs, FileNameComparator.INSTANCE);
        final String itemName = folderName + itemDir.getName();
        if ("created".equals(type)) {
            if (DeletedFileFilter.accepts(itemDir)) {
                return;
            }
            File historyDir = historyDirs[0];
            HistoryDescr histDescr = readHistoryXml(historyDir);
            if ("Created".equals(histDescr.getOperation())) {
                final ConfigInfo config = ConfigInfo.create(itemName, historyDir, histDescr, true);
                configs.add(config);
            } else {
                historyDir = historyDirs[1];
                histDescr = readHistoryXml(historyDir);
                if ("Created".equals(histDescr.getOperation())) {
                    final ConfigInfo config = ConfigInfo.create(itemName, historyDir, histDescr, true);
                    configs.add(config);
                }
            }
        } else if ("deleted".equals(type)) {
            final File historyDir = historyDirs[historyDirs.length - 1];
            final HistoryDescr histDescr = readHistoryXml(historyDir);
            if ("Deleted".equals(histDescr.getOperation())) {
                final ConfigInfo config = ConfigInfo.create(itemName, historyDir, histDescr, false);
                configs.add(config);
            }
        } else {
            for (final File historyDir : historyDirs) {
                final ConfigInfo config;
                final HistoryDescr histDescr = readHistoryXml(historyDir);
                if (DeletedFileFilter.accepts(itemDir)) {
                    config = ConfigInfo.create(itemName, historyDir, histDescr, false);
                } else {
                    config = ConfigInfo.create(itemName, historyDir, histDescr, true);
                }
                configs.add(config);
            }
        }
    }

    /**
     * Collects configs.
     *
     * @param rootDir
     *            of config-history.
     * @param folderName
     *            folderName, usually just the empty string.
     * @throws IOException if an entry could not be read.
     */
    public List<ConfigInfo> collect(final File rootDir, final String folderName) throws IOException {
        final File rootWithFolder = new File(rootDir, folderName);
        final File[] itemDirs;
        if ("deleted".equals(type)) {
            itemDirs = rootWithFolder.listFiles(DeletedFileFilter.INSTANCE);
        } else {
            itemDirs = rootWithFolder.listFiles();
        }
        for (final File itemDir : itemDirs) {
            getConfigsForType(itemDir, folderName);
        }
        return configs;
    }

    /**
     * Extract HistoryDescriptor from history directory.
     *
     * @param historyDir
     *            History directory as File. (e.g. 2013-10-10_00-08-15)
     * @return History descriptor
     * @throws IOException
     *             If xml file cannot be read.
     */
    private HistoryDescr readHistoryXml(File historyDir) throws IOException {
        final XmlFile historyXml = new XmlFile(new File(historyDir, JobConfigHistoryConsts.HISTORY_FILE));
        return (HistoryDescr) historyXml.read();
    }
}
