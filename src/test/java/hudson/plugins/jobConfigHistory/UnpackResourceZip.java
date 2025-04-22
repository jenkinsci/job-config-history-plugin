package hudson.plugins.jobConfigHistory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Takes the URL of zip with test resources and unpacks it to a temporary
 * folder, which may be retrieved with {@link UnpackResourceZip#getRoot()}.
 *
 * @author Mirko Friedenhagen
 */
class UnpackResourceZip {

    private final File temporaryFolder;

    private final URL resourceURL;

    private UnpackResourceZip(URL resourceURL) throws Exception {
        this.temporaryFolder = Files.createTempDirectory(new File("target").toPath(), "junit").toFile();
        this.resourceURL = resourceURL;

        unpackZip();
    }

    public static UnpackResourceZip create() throws Exception {
        return new UnpackResourceZip(UnpackResourceZip.class
                .getResource("JobConfigHistoryPurgerIT.zip"));
    }

    public void cleanUp() throws Exception {
        try {
            FileUtils.deleteDirectory(getRoot());
        } catch (IOException ex) {
            FileUtils.forceDeleteOnExit(getRoot());
        }
    }

    /**
     * @return the root location of the unpacked resources.
     */
    public File getRoot() {
        return temporaryFolder;
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
                final File file = new File(getRoot(), entry.getName());
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
