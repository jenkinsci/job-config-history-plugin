package hudson.plugins.jobConfigHistory;

import org.apache.commons.io.IOUtils;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Takes the URL of zip with test resources and unpacks it to a temporary
 * folder, which may be retrieved with {@link UnpackResourceZip#getRoot()}.
 *
 * @author Mirko Friedenhagen
 */
public class UnpackResourceZip extends ExternalResource {

    private final TemporaryFolder temporaryFolder = new TemporaryFolder(
            new File("target"));

    private final URL resourceURL;

    public UnpackResourceZip(URL resourceURL) {
        this.resourceURL = resourceURL;
    }

    public static UnpackResourceZip create() {
        return new UnpackResourceZip(UnpackResourceZip.class
                .getResource("JobConfigHistoryPurgerIT.zip"));
    }

    @Override
    protected void before() throws Throwable {
        temporaryFolder.create();
        unpackZip();
    }

    @Override
    protected void after() {
        temporaryFolder.delete();
    }

    /**
     * @return the root location of the unpacked resources.
     */
    public File getRoot() {
        return temporaryFolder.getRoot();
    }

    /**
     * @param resourceName name
     * @return an unpacked resource.
     */
    public File getResource(final String resourceName) {
        return new File(getRoot(), resourceName);
    }

    private void unpackZip() throws IOException {
        // IOUtils.copy below buffers InputStream!
        try (ZipInputStream in = new ZipInputStream(resourceURL.openStream())) {
            ZipEntry entry = in.getNextEntry();
            while (entry != null) {
                final File file = new File(temporaryFolder.getRoot(),
                        entry.getName());
                if (!entry.isDirectory()) {
                    createFileResource(file, in);
                } else {
                    createDirectoryResource(file);
                }
                entry = in.getNextEntry();
            }
        }
    }

    private void createFileResource(final File file, final ZipInputStream in)
            throws IOException {
        createDirectoryResource(file.getParentFile());
        try (BufferedOutputStream out = new BufferedOutputStream(
                new FileOutputStream(file))) {
            IOUtils.copy(in, out);
        }
    }

    private void createDirectoryResource(final File file) {
        file.mkdirs();
    }
}
