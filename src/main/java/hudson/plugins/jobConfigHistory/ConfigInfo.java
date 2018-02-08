/*
 * The MIT License
 *
 * Copyright 2013 Stefan Brausch.
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

import java.util.Date;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * Holder object for displaying information.
 *
 *
 * @author Stefan Brausch
 */
@ExportedBean(defaultVisibility = 999)
public class ConfigInfo implements ParsedDate {

	/** The display name of the user. */
	private final String user;

	/** The id of the user. */
	private final String userID;

	/** The date of the change. */
	private final String date;

	/** Does the configuration exist?. */
	private final boolean configExists;

	/** The name of the job or file. */
	private final String job;

	/** One of created, changed, renamed or deleted. */
	private final String operation;

	/**
	 * true if this information is for a Jenkins job, as opposed to information
	 * for a system configuration file.
	 */
	private boolean isJob;

	/**
	 * The current job name after the renaming.
	 */
	private final String currentName;

	/**
	 * The old job name before renaming.
	 */
	private final String oldName;

	/**
	 * Returns a new ConfigInfo object for a system configuration file.
	 * 
	 * @param name
	 *            Name of the configuration entity we are saving.
	 * @param configExists
	 *            Does the config file exist?
	 * @param histDescr
	 *            metadata of the change.
	 * @param isJob
	 *            whether it is a job's config info or not.
	 * @return a new ConfigInfo object.
	 */
	public static ConfigInfo create(final String name,
			final boolean configExists, final HistoryDescr histDescr,
			final boolean isJob) {
		return new ConfigInfo(name, configExists, histDescr.getTimestamp(),
				histDescr.getUser(), histDescr.getOperation(),
				histDescr.getUserID(), isJob, histDescr.getCurrentName(),
				histDescr.getOldName());
	}

	/**
	 * @param job
	 *            see {@link ConfigInfo#job}.
	 * @param configExists
	 *            see {@link ConfigInfo#configExists}.
	 * @param date
	 *            see {@link ConfigInfo#date}
	 * @param user
	 *            see {@link ConfigInfo#user}
	 * @param operation
	 *            see {@link ConfigInfo#operation}
	 * @param userID
	 *            see {@link ConfigInfo#userID}
	 * @param isJob
	 *            see {@link ConfigInfo#isJob}
	 */
	ConfigInfo(String job, boolean configExists, String date, String user,
			String operation, String userID, boolean isJob, String currentName,
			String oldName) {
		this.job = job;
		this.configExists = configExists;
		this.date = date;
		this.user = user;
		this.operation = operation;
		this.userID = userID;
		this.isJob = isJob;
		this.currentName = currentName;
		this.oldName = oldName;
	}

	/**
	 * Returns the display name of the user.
	 *
	 * @return display name
	 */
	@Exported
	public String getUser() {
		return user;
	}

	/**
	 * Returns the id of the user.
	 *
	 * @return user id
	 */
	@Exported
	public String getUserID() {
		return userID;
	}

	/**
	 * Returns the date of the change.
	 *
	 * @return timestamp in the format of
	 *         {@link JobConfigHistoryConsts#ID_FORMATTER}
	 */
	@Exported
	public String getDate() {
		return date;
	}

	/**
	 * Does the configuration of the file exist?
	 *
	 * @return URL encoded filename
	 */
	@Exported
	public boolean hasConfig() {
		return configExists;
	}

	/**
	 * Returns the name of the job.
	 *
	 * @return name of the job
	 */
	@Exported
	public String getJob() {
		return job;
	}

	/**
	 * Returns the type of the operation.
	 *
	 * @return name of the operation
	 */
	@Exported
	public String getOperation() {
		return operation;
	}

	/**
	 * Returns true if this object represents a Jenkins job as opposed to
	 * representing a system configuration.
	 * 
	 * @return true if this object stores a Jenkins job configuration
	 */
	public boolean getIsJob() {
		return isJob;
	}

	@Override
	public String toString() {
		return operation + " on " + job + " @" + date;
	}

	/**
	 * Returns a {@link Date}.
	 *
	 * @return The parsed date as a java.util.Date.
	 */
	@Override
	public Date parsedDate() {
		return PluginUtils.parsedDate(getDate());
	}

	/**
	 * Returns the current job name after renaming.
	 * 
	 * @return current job name
	 */
	@Exported
	public String getCurrentName() {
		return currentName;
	}

	/**
	 * Returns the old job name before renaming.
	 * 
	 * @return old job name before renaming
	 */
	@Exported
	public String getOldName() {
		return oldName;
	}

}
