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

import java.util.ArrayList;
import java.util.Collection;
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
     * Does the configuration file exist?
     */
    private final boolean configExists;
    /**
     * List of history descriptions.
     */
    private final Collection<HistoryDescr> historyDescrs;
    /**
     * Is this a job?
     */
    private final boolean isJob;

    /**
     * Constructor.
     * @param name of the job or configuration.
     * @param configExists Does the configuration file exist?
     * @param historyDescrs history descriptions.
     * @param isJob is this a job?
     */
    HistoryDescrToConfigInfo(String name, boolean configExists, Collection<HistoryDescr> historyDescrs, boolean isJob) {
        this.name = name;
        this.configExists = configExists;
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
            configInfos.add(ConfigInfo.create(name, configExists, historyDescr, isJob));
        }
        return configInfos;
    }

    /**
     * Converts to a list of {@link ConfigInfo}.
     *
     * @param name of the job or configuration.
     * @param configExists Does the configuration file exist?
     * @param historyDescrs history descriptions.
     * @param isJob is this a job?
     * @return list of {@link ConfigInfo}s.
     */
    static List<ConfigInfo> convert(String name, boolean configExists, Collection<HistoryDescr> historyDescrs, boolean isJob) {
       return new HistoryDescrToConfigInfo(name, configExists, historyDescrs, isJob).convert();
    }

}
