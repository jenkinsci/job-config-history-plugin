package hudson.plugins.jobConfigHistory;

import hudson.XmlFile;

/**
 *
 * @author Mirko Friedenhagen
 */
public interface ConfigHistoryListenerHelper {

    /**
     * Possible Events.
     */
    public enum Events {

        /** Job created */
        CREATED(Messages.ConfigHistoryListenerHelper_CREATED()),

        /** Job renamed */
        RENAMED(Messages.ConfigHistoryListenerHelper_RENAMED()),

        /** Job modified */
        CHANGED(Messages.ConfigHistoryListenerHelper_CHANGED()),

        /** Job deleted */
        DELETED(Messages.ConfigHistoryListenerHelper_DELETED());

        /**
         * Name of the operation.
         */
        private final String operation;

        /**
         * @param operation localized version.
         */
        Events(final String operation) {
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
