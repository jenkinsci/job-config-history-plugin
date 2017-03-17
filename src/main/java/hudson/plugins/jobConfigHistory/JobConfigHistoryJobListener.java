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

import static java.util.logging.Level.FINEST;

import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.Extension;
import hudson.model.AbstractItem;
import hudson.model.Item;
import hudson.model.listeners.ItemListener;

/**
 * Saves the job configuration if the job is created or renamed.
 *
 * @author Stefan Brausch
 */
@Extension
public class JobConfigHistoryJobListener extends ItemListener {

	/**
	 * Our logger.
	 */
	private static final Logger LOG = Logger
			.getLogger(JobConfigHistoryJobListener.class.getName());

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreated(Item item) {
		LOG.log(FINEST, "In onCreated for {0}", item);
		switchHistoryDao(item).createNewItem((item));
		LOG.log(FINEST, "onCreated for {0} done.", item);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * Also checks if we have history stored under the old name. If so, copies
	 * all history to the folder for new name, and deletes the old history
	 * folder.
	 */
	@Override
	public void onRenamed(Item item, String oldName, String newName) {
		final String onRenameDesc = " old name: " + oldName + ", new name: "
				+ newName;
		LOG.log(FINEST, "In onRenamed for {0}{1}",
				new Object[]{item, onRenameDesc});
		switchHistoryDao(item).renameItem(item, oldName, newName);
		LOG.log(FINEST, "Completed onRename for {0} done.", item);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onDeleted(Item item) {
		LOG.log(FINEST, "In onDeleted for {0}", item);
		switchHistoryDao(item).deleteItem(item);
		LOG.log(FINEST, "onDeleted for {0} done.", item);
	}

	/**
	 * Returns ItemListenerHistoryDao depending on the item type.
	 *
	 * @param item
	 *            the item to switch on.
	 * @return dao
	 */
	private ItemListenerHistoryDao switchHistoryDao(Item item) {
		return item instanceof AbstractItem
				? getHistoryDao()
				: NoOpItemListenerHistoryDao.INSTANCE;
	}

	/**
	 * Just for tests.
	 *
	 * @return ItemListenerHistoryDao.
	 */
	ItemListenerHistoryDao getHistoryDao() {
		return PluginUtils.getHistoryDao();
	}

	/**
	 * No operation ItemListenerHistoryDao.
	 */
	private static class NoOpItemListenerHistoryDao
			implements
				ItemListenerHistoryDao {

		/**
		 * The instance.
		 */
		static final NoOpItemListenerHistoryDao INSTANCE = new NoOpItemListenerHistoryDao();

		@Override
		public void createNewItem(Item item) {
			LOG.log(Level.FINEST,
					"onCreated: not an AbstractItem {0}, skipping.", item);
		}

		@Override
		public void renameItem(Item item, String oldName, String newName) {
			LOG.log(Level.FINEST,
					"onRenamed: not an AbstractItem {0}, skipping.", item);
		}

		@Override
		public void deleteItem(Item item) {
			LOG.log(Level.FINEST,
					"onDeleted: not an AbstractItem {0}, skipping.", item);
		}

	}
}
