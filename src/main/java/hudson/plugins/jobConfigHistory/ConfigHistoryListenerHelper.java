package hudson.plugins.jobConfigHistory;

import hudson.XmlFile;

/**
 *
 * @author Mirko Friedenhagen
 */
public interface ConfigHistoryListenerHelper {

    public enum States {

        CREATED(Messages.ConfigHistoryListenerHelper_CREATED()),

        RENAMED(Messages.ConfigHistoryListenerHelper_RENAMED()),

        CHANGED(Messages.ConfigHistoryListenerHelper_CHANGED()),

        DELETED(Messages.ConfigHistoryListenerHelper_DELETED());

        /**
         * Name of the operation.
         */
        private final String operation;

        States(final String operation) {
            this.operation = operation;
        }

        /**
         * @return the operation
         */
        public String getOperation() {
            return operation;
        }
    }

    /**
     * Creates a new backup of the job configuration.
     *
     * @param xmlFile
     *            configuration file for the item we want to backup
     */
    void createNewHistoryEntry(final XmlFile xmlFile);
}
