package hudson.plugins.jobConfigHistory;

import hudson.XmlFile;
import hudson.model.Action;

import java.io.File;
import java.io.IOException;

import org.kohsuke.stapler.Stapler;

/**
 * Implements some basic methods for returning baseUrl and image paths. This is the base class for javascript actions.
 *
 * @author mfriedenhagen
 */
public abstract class JobConfigHistoryBaseAction implements Action {

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
     * Make method final, as we always want the same icon file.
     */
    // @Override
    public final String getIconFileName() {
        return JobConfigHistoryConsts.ICONFILENAME;
    }

    /** {@inheritDoc} */
    // @Override
    public String getUrlName() {
        return JobConfigHistoryConsts.URLNAME;
    }

    /**
     * Do we want 'raw' output?
     *
     * @return true if request parameter type is 'raw'
     */
    public boolean wantRawOutput() {
        return Stapler.getCurrentRequest().getParameter("type").equalsIgnoreCase("raw");

    }

    /**
     * Do we want 'xml' output?
     *
     * @return true if request parameter type is 'xml'
     */
    public boolean wantXmlOutput() {
        return Stapler.getCurrentRequest().getParameter("type").equalsIgnoreCase("xml");

    }

    /**
     * Returns {@link JobConfigHistoryBaseAction#getConfigXml(String)} as String.
     *
     * @return content of the {@code config.xml} found in directory given by the request parameter {@code file}.
     * @throws IOException
     *             if the config file could not be read.
     */
    public String getFile() throws IOException {
        return getConfigXml(getRequestParameter("file")).asString();
    }

    /**
     * Returns the {@code config.xml} located in {@code diffDir}.
     *
     * @param diffDir
     *            timestamped history directory.
     * @return xmlfile.
     */
    protected XmlFile getConfigXml(final String diffDir) {
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
}
