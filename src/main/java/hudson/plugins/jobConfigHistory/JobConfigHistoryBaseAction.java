package hudson.plugins.jobConfigHistory;

import hudson.XmlFile;
import hudson.model.Action;
import hudson.model.Hudson;
import hudson.security.AccessControlled;
import hudson.security.Permission;

import java.io.File;
import java.io.IOException;

import org.kohsuke.stapler.Stapler;

/**
 * Implements some basic methods needed by the {@link JobConfigHistoryRootAction} and {@link JobConfigHistoryProjectAction}.
 *
 * @author mfriedenhagen
 */
public abstract class JobConfigHistoryBaseAction implements Action {

    /**
     * The hudson instance.
     */
    private final Hudson hudson;

    /**
     * Set the {@link Hudson} instance.
     */
    public JobConfigHistoryBaseAction() {
        hudson = Hudson.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * Make method final, as we always want the same display name.
     */
    // @Override
    public final String getDisplayName() {
        return JobConfigHistoryConsts.DISPLAYNAME;
    }

    /**
     * {@inheritDoc}
     *
     * Make method final, as we always want the same icon file. Returns {@code null} to hide the icon if the user is not
     * allowed to configure jobs.
     */
    // @Override
    public final String getIconFileName() {
        return hasConfigurePermission() ? JobConfigHistoryConsts.ICONFILENAME : null;
    }

    /**
     * {@inheritDoc}
     *
     * Do not make this method final as {@link JobConfigHistoryRootAction} overrides this method.
     */
    // @Override
    public String getUrlName() {
        return JobConfigHistoryConsts.URLNAME;
    }

    /**
     * Do we want 'raw' output?
     *
     * @return true if request parameter type is 'raw'.
     */
    public final boolean wantRawOutput() {
        return isTypeParameter("raw");
    }

    /**
     * Do we want 'xml' output?
     *
     * @return true if request parameter type is 'xml'.
     */
    public final boolean wantXmlOutput() {
        return isTypeParameter("xml");
    }

    /**
     * Returns {@link JobConfigHistoryBaseAction#getConfigXml(String)} as String.
     *
     * @return content of the {@code config.xml} found in directory given by the request parameter {@code file}.
     * @throws IOException
     *             if the config file could not be read.
     */
    public final String getFile() throws IOException {
        checkConfigurePermission();
        return getConfigXml(getRequestParameter("file")).asString();
    }

    /**
     * Checks whether the type parameter of the current request equals {@code toCompare}.
     *
     * @param toCompare
     *            the string we want to compare.
     * @return true if {@code toCompare} equals request parameter type.
     */
    private boolean isTypeParameter(final String toCompare) {
        return getRequestParameter("type").equalsIgnoreCase(toCompare);
    }

    /**
     * Returns the {@code config.xml} located in {@code diffDir}. {@code diffDir} must start with {@code HUDSON_HOME}
     * and contain {@code config-history}, otherwise an {@link IllegalArgumentException} will be thrown. This is to
     * ensure that this plugin will not be abused to get arbitrary {@code config.xml} files located anywhere on the
     * system.
     *
     * @param diffDir
     *            timestamped history directory.
     * @return xmlfile.
     */
    protected XmlFile getConfigXml(final String diffDir) {
        final File rootDir = getHudson().getRootDir();
        final String absoluteRootDirPath = rootDir.getAbsolutePath();
        if (!diffDir.startsWith(absoluteRootDirPath) || !diffDir.contains("config-history") || diffDir.contains("..")) {
            throw new IllegalArgumentException(diffDir + " does not start with " + absoluteRootDirPath
                    + ", does not contain 'config-history' or contains '..'");
        }
        return new XmlFile(new File(diffDir, "config.xml"));
    }

    /**
     * Returns the parameter named {@code parameterName} from current request.
     *
     * @param parameterName
     *            name of the parameter.
     * @return value of the request parameter or null if it does not exist.
     */
    protected String getRequestParameter(final String parameterName) {
        return Stapler.getCurrentRequest().getParameter(parameterName);
    }

    /**
     * See whether the current user may read configurations in the object returned by
     * {@link JobConfigHistoryBaseAction#getAccessControlledObject()}.
     */
    protected final void checkConfigurePermission() {
        getAccessControlledObject().checkPermission(Permission.CONFIGURE);
    }

    /**
     * Returns whether the current user may read configurations in the object returned by
     * {@link JobConfigHistoryBaseAction#getAccessControlledObject()}.
     *
     * @return true if the current user may read configurations.
     */
    protected final boolean hasConfigurePermission() {
        return getAccessControlledObject().hasPermission(Permission.CONFIGURE);
    }

    /**
     * Returns the hudson instance.
     *
     * @return the hudson
     */
    protected final Hudson getHudson() {
        return hudson;
    }

    /**
     * Returns the object for which we want to provide access control.
     *
     * @return the access controlled object.
     */
    protected abstract AccessControlled getAccessControlledObject();

}
