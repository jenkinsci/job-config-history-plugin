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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Mirko Friedenhagen
 */
public class ConfigInfoCollectorTest {

    @Rule
    public final UnpackResourceZip unpackResourceZip = UnpackResourceZip
            .create();

    @Test
    public void testGetConfigsForType() throws IOException {
        unpackResourceZip.getResource("config-history/jobs/Test2").mkdirs();
        assertThatFolderXHasYItemsOfTypeZ("Test2", 0, "does not matter");
    }

    /**
     * Test of collect method, of class ConfigInfoCollector.
     */
    @Test
    public void testCollectDeleted() throws Exception {
        assertThatRootFolderHasYItemsOfTypeZ(1, "deleted");
    }

    /**
     * Test of collect method, of class ConfigInfoCollector.
     */
    @Test
    public void testCollectCreated() throws Exception {
        assertThatRootFolderHasYItemsOfTypeZ(1, "created");
    }

    /**
     * Test of collect method, of class ConfigInfoCollector.
     */
    @Test
    public void testCollectCreatedWithChangeEventBeforeCreated()
            throws Exception {
        FileUtils.copyDirectory(
                unpackResourceZip.getResource(
                        "config-history/jobs/Test1/2012-11-21_11-35-12"),
                unpackResourceZip.getResource(
                        "config-history/jobs/Test1/2012-11-21_11-28-12"));
        assertThatRootFolderHasYItemsOfTypeZ(1, "created");
    }

    /**
     * Test of collect method, of class ConfigInfoCollector.
     */
    @Test
    public void testCollectOther() throws Exception {
        FileUtils.copyDirectory(
                unpackResourceZip.getResource("config-history/jobs/Test1"),
                unpackResourceZip.getResource("config-history/jobs/Test2"));
        assertThatRootFolderHasYItemsOfTypeZ(13, "other");
    }

    /**
     * Test of collect method, of class ConfigInfoCollector.
     */
    @Test
    public void testCollectJobFolder() throws Exception {
        String folderName = "FolderName";
        // Create folders
        FileUtils.copyDirectory(
                unpackResourceZip.getResource("config-history/jobs/Test1"),
                unpackResourceZip.getResource(
                        "config-history/jobs/" + folderName + "/jobs/Test1"));
        FileUtils.copyDirectory(
                unpackResourceZip.getResource("config-history/jobs/Test1"),
                unpackResourceZip.getResource(
                        "config-history/jobs/" + folderName + "/jobs/Test2"));
        assertThatFolderXHasYItemsOfTypeZ(folderName, 10, "other");
        assertThatFolderXHasYItemsOfTypeZ(folderName, 2, "created");
    }

    void assertThatRootFolderHasYItemsOfTypeZ(int noOfHistoryItems,
                                              final String type) throws IOException {
        assertThatFolderXHasYItemsOfTypeZ("", noOfHistoryItems, type);
    }

    void assertThatFolderXHasYItemsOfTypeZ(String folderName,
                                           int noOfHistoryItems, final String type) throws IOException {
        ConfigInfoCollector sut = createSut(type);
        List<ConfigInfo> result = sut.collect(folderName);
        result.sort(ParsedDateComparator.DESCENDING);
        assertEquals(StringUtils.join(result, "\n"), noOfHistoryItems,
                result.size());
    }

    private ConfigInfoCollector createSut(final String type) {
        final OverviewHistoryDao historyDao = new FileHistoryDao(
                unpackResourceZip.getResource("config-history"),
                unpackResourceZip.getRoot(), null, 0, true);
        return new ConfigInfoCollector(type, historyDao);
    }
}
