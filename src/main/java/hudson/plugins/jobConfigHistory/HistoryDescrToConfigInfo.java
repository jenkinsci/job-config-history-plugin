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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts given {@link HistoryDescr} to {@link ConfigInfo}.
 *
 * @author Mirko Friedenhagen
 */
class HistoryDescrToConfigInfo {

    /**
     * Name of ConfigInfo.
     */
    private final String name;
    /**
     * Basedir of history entries.
     */
    private final File historyBaseDir;
    /**
     * List of history descriptions.
     */
    private final List<HistoryDescr> historyDescrs;
    /**
     * Is this a job?
     */
    private final boolean isJob;

    /**
     * Constructor.
     * @param name of the job or configuration.
     * @param historyBaseDir where history is located.
     * @param historyDescrs history descriptions.
     * @param isJob is this a job?
     */
    HistoryDescrToConfigInfo(String name, File historyBaseDir, List<HistoryDescr> historyDescrs, boolean isJob) {
        this.name = name;
        this.historyBaseDir = historyBaseDir;
        this.historyDescrs = historyDescrs;
        this.isJob = isJob;
    }

    /**
     * Converts to a list of {@link ConfigInfo}.
     *
     * @return list of {@link ConfigInfo}s.
     */
    List<ConfigInfo> convert() {
        final ArrayList<ConfigInfo> configInfos = new ArrayList<ConfigInfo>();
        for (HistoryDescr historyDescr : historyDescrs) {
            configInfos.add(ConfigInfo.create(name, true, historyDescr, isJob));
        }
        return configInfos;
    }

}
