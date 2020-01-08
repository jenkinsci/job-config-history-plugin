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
import java.util.SortedMap;

/**
 * HistoryDao which returns all jobs, system configurations or deleted jobs.
 *
 * @author Mirko Friedenhagen
 */
public interface OverviewHistoryDao {

	/**
	 * Returns a list of deleted jobs with a history.
	 *
	 * @param folderName
	 *            name of folder.
	 * @return list of deleted jobs with a history, emtpy when no history
	 *         exists.
	 */
	File[] getDeletedJobs(String folderName);

	/** @return the total number of system configuration revision entries. */
	int getSystemRevisionAmount();

	/** @return the total number of job configuration revision entries. */
	int getJobRevisionAmount();

	/** @return the number of configuration revision entries for a certain system config. */
	int getSystemRevisionAmount(String jobName);

	/** @return the total number of deleted Jobs. */
	int getDeletedJobAmount();

	/** @return the number of configuration revision entries for a certain system config. */
	int getJobRevisionAmount(String jobName);

	/** @return the total number of configuration revision entries, excluding agent config entries <-- TODO future work. */
	int getTotalRevisionAmount();

	/**
	 * Returns a list of deleted jobs with a history, including all those
	 * contained in folders.
	 *
	 * @return list of deleted jobs with a history, emtpy when no history
	 *         exists.
	 */
	File[] getDeletedJobs();

	/**
	 * Returns a list of jobs with a history.
	 *
	 * @param folderName
	 *            name of folder
	 * @return list of jobs with a history, empty when no history exists.
	 */
	File[] getJobs(String folderName);

	/**
	 * Returns a list of jobs with a history, including all those contained in folders.
	 *
	 * @return list of all jobs with a history, empty when no history exists.
	 */
	File[] getJobs();

	/**
	 * Returns a list of all system configuration files with a history.
	 *
	 * @return list of all system configuration files with a history, empty when
	 *         no history exists.
	 */
	File[] getSystemConfigs();

	/**
	 *
	 * @return a map mapping timestamps to historydescrs. Contains all system config revision entries.
	 */
	SortedMap<String, HistoryDescr> getSystemConfigsMap();

	/**
	 * Returns a sorted map of all HistoryDescr for a given job.
	 * 
	 * @param jobName
	 *            of the job
	 * @return sorted map.
	 */
	SortedMap<String, HistoryDescr> getJobHistory(final String jobName);

	/**
	 * Returns a sorted map of all HistoryDescr for a given system
	 * configuration.
	 * 
	 * @param name
	 *            of the configuration
	 * @return sorted map.
	 */
	SortedMap<String, HistoryDescr> getSystemHistory(final String name);
}
