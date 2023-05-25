/*
 * The MIT License
 *
 * Copyright 2013 mirko.
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

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertEquals;

/**
 * @author mirko
 */
public class PluginUtilsTest {

    /**
     * Test of parsedDate method, of class PluginUtils.
     */
    @Test
    public void testParsedDate() {
        String timeStamp = "2012-11-21_11-29-12";
        Date expResult = new Date(112, Calendar.NOVEMBER, 21, 11, 29, 12);
        Date result = PluginUtils.parsedDate(timeStamp);
        assertEquals(expResult, result);
    }

    /**
     * Test of parsedDate method, of class PluginUtils.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testParsedDateError() {
        String timeStamp = "abc";
        PluginUtils.parsedDate(timeStamp);
    }
}
