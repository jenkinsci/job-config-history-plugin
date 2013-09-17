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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;

/**
 *
 * @author Mirko Friedenhagen
 */
public class ConfigInfoCollectorTest {

    @Rule
    public final UnpackResourceZip unpackResourceZip = UnpackResourceZip.INSTANCE;

    @Test
    public void testGetConfigsForType() throws IOException {
        final ConfigInfoCollector sut = createSut("created");
        final File historyDirWithNoEntries = unpackResourceZip.getResource("config-history/jobs/Test2");
        historyDirWithNoEntries.mkdir();
        sut.getConfigsForType(historyDirWithNoEntries, "");
        assertEquals(0, sut.getConfigs().size());
    }
    /**
     * Test of collect method, of class ConfigInfoCollector.
     */
    @Test
    public void testCollectDeleted() throws Exception {
        assertRootFolderHasYItemsOfTypeZ(1, "deleted");
    }

    /**
     * Test of collect method, of class ConfigInfoCollector.
     */
    @Test
    public void testCollectCreated() throws Exception {
        assertRootFolderHasYItemsOfTypeZ(1, "created");
    }

    /**
     * Test of collect method, of class ConfigInfoCollector.
     */
    @Test
    public void testCollectOther() throws Exception {
        FileUtils.copyDirectory(
                unpackResourceZip.getResource("config-history/jobs/Test1"),
                unpackResourceZip.getResource("config-history/jobs/Test2"));
        assertRootFolderHasYItemsOfTypeZ(13, "other");
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
                unpackResourceZip.getResource("config-history/jobs/" + folderName + "/Test1"));
        FileUtils.copyDirectory(
                unpackResourceZip.getResource("config-history/jobs/Test1"),
                unpackResourceZip.getResource("config-history/jobs/" + folderName + "/Test2"));
        assertFolderXHasYItemsOfTypeZ(folderName, 10, "other");
        assertFolderXHasYItemsOfTypeZ(folderName, 2, "created");
    }

    void assertRootFolderHasYItemsOfTypeZ(int noOfHistoryItems, final String type) throws IOException {
        assertFolderXHasYItemsOfTypeZ("", noOfHistoryItems, type);
    }

    void assertFolderXHasYItemsOfTypeZ(String folderName, int noOfHistoryItems, final String type) throws IOException {
        ConfigInfoCollector sut = createSut(type);
        List<ConfigInfo> result = sut.collect(unpackResourceZip.getResource("config-history/jobs"), folderName);
        Collections.sort(result, ParsedDateComparator.DESCENDING);
        assertEquals(StringUtils.join(result, "\n"), noOfHistoryItems, result.size());
    }

    private ConfigInfoCollector createSut(final String type) {
        return new ConfigInfoCollector(type, null);
    }
}
