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

import hudson.XmlFile;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 *
 * Tests for HistoryDescrToConfigInfo.
 *
 * @author Mirko Friedenhagen
 */
public class HistoryDescrToConfigInfoTest {
    @Rule
    public final UnpackResourceZip unpackResourceZip = UnpackResourceZip
            .create();
    private FileHistoryDao historyDao;
    private File jenkinsHome;

    @Before
    public void setUp() {
        jenkinsHome = unpackResourceZip.getRoot();
        File historyRoot = unpackResourceZip.getResource("config-history");
        historyDao = new FileHistoryDao(historyRoot, jenkinsHome, null, 0,
                true);
    }

    @Test
    public void shouldConvertCorrectly() {
        List<ConfigInfo> result = HistoryDescrToConfigInfo.convert("Test1",
                true,
                new ArrayList<>(historyDao
                        .getRevisions(new XmlFile(
                                new File(jenkinsHome, "jobs/Test1/config.xml")))
                        .values()),
                true);
        assertEquals(5, result.size());
    }
}
