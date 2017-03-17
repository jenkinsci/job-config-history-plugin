/*
 * The MIT License
 *
 * Copyright 2015 Brandon Koepke.
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

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

/**
 * Master class for adding new backends to the history plugin.
 *
 * @author Brandon Koepke
 */
public abstract class JobConfigHistoryStrategy
		implements
			ExtensionPoint,
			Describable<JobConfigHistoryStrategy>,
			HistoryDao,
			ItemListenerHistoryDao,
			OverviewHistoryDao,
			NodeListenerHistoryDao {
	/**
	 * Data bound constructors need a public empty constructor.
	 */
	@DataBoundConstructor
	public JobConfigHistoryStrategy() {
	}

	/** {@inheritDoc} */
	@Override
	@SuppressWarnings("unchecked")
	public final Descriptor<JobConfigHistoryStrategy> getDescriptor() {
		return Jenkins.getInstance().getDescriptorOrDie(getClass());
	}

	/**
	 * Gets the extension point list for the JobConfigHistoryStrategy.
	 *
	 * @return the extension point list.
	 */
	public static DescriptorExtensionList<JobConfigHistoryStrategy, JobConfigHistoryDescriptor<JobConfigHistoryStrategy>> all() {
		return Jenkins.getInstance()
				.<JobConfigHistoryStrategy, JobConfigHistoryDescriptor<JobConfigHistoryStrategy>>getDescriptorList(
						JobConfigHistoryStrategy.class);
	}
}
