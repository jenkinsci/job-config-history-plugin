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
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;

/**
 *
 * @author Mirko Friedenhagen
 */
public class ConfigInfoCollectorTest {

    @Rule
    public final UnpackResourceZip unpackResourceZip = UnpackResourceZip.INSTANCE;

    /**
     * Test of getConfigsForType method, of class ConfigInfoCollector.
     */
    @Test
    public void testGetConfigsForType() throws Exception {
        File itemDir = unpackResourceZip.getResource("config-history/jobs/Test1");
        String prefix = "";
        ConfigInfoCollector sut = new ConfigInfoCollector("deleted");
        List<ConfigInfo> result = sut.getConfigsForType(itemDir, prefix);
        assertEquals(0, result.size());
    }

    /**
     * Test of collect method, of class ConfigInfoCollector.
     */
    @Test
    public void testCollectDeleted() throws Exception {
        File rootDir = unpackResourceZip.getResource("config-history");
        String prefix = "";
        ConfigInfoCollector sut = new ConfigInfoCollector("deleted");
        List<ConfigInfo> result = sut.collect(rootDir, prefix);
        assertEquals(0, result.size());
    }

    /**
     * Test of collect method, of class ConfigInfoCollector.
     */
    @Test
    public void testCollectCreated() throws Exception {
        File rootDir = unpackResourceZip.getResource("config-history");
        String prefix = "";
        ConfigInfoCollector sut = new ConfigInfoCollector("created");
        List<ConfigInfo> result = sut.collect(rootDir, prefix);
        assertEquals(0, result.size());
    }

    /**
     * Test of collect method, of class ConfigInfoCollector.
     */
    @Test
    public void testCollectOther() throws Exception {
        File rootDir = unpackResourceZip.getResource("config-history");
        String prefix = "jobs";
        ConfigInfoCollector sut = new ConfigInfoCollector("other");
        List<ConfigInfo> result = sut.collect(rootDir, prefix);
        Collections.sort(result, ParsedDateComparator.DESCENDING);
        System.out.println("\ntestCollectOther\n" + StringUtils.join(result, "\n"));
        assertEquals(20, result.size());
    }

    /**
     * Test of collect method, of class ConfigInfoCollector.
     */
    @Test
    public void testCollectJobGroup() throws Exception {
        File rootDir = unpackResourceZip.getResource("config-history");
        // Create folder
        FileUtils.copyDirectory(
                unpackResourceZip.getResource("config-history/jobs/Test1"), 
                unpackResourceZip.getResource("config-history/GroupName/jobs/Test1"));
        String prefix = "GroupName";
        ConfigInfoCollector sut = new ConfigInfoCollector("other");
        List<ConfigInfo> result = sut.collect(rootDir, prefix);
        Collections.sort(result, ParsedDateComparator.DESCENDING);
        System.out.println("\ntestCollectJobGroup\n" + StringUtils.join(result, "\n"));
        assertEquals(100, result.size());
    }
}
