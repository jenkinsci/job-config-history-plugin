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

import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for PluginUtils.
 *
 * @author mirko
 */
class PluginUtilsTest {

    @Test
    void utilShouldPraseDateCorrectly() {
        String timeStamp = "2012-11-21_11-29-12";
        Date expResult = new GregorianCalendar(2012, Calendar.NOVEMBER, 21, 11, 29, 12).getTime();
        Date result = PluginUtils.parsedDate(timeStamp);
        assertEquals(expResult, result);
    }

    @Test
    void parsingInvalidDateShouldThrow() {
        String timeStamp = "abc";
        assertThrows(IllegalArgumentException.class, () ->
            PluginUtils.parsedDate(timeStamp));
    }
}
