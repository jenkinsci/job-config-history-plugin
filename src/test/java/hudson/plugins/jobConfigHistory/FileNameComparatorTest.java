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

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 *
 * Test of compare method, of class FileNameComparator.
 *
 * @author Mirko Friedenhagen
 */
class FileNameComparatorTest {

    private static final int RESULT_SAME_NAME = 0;

    @Test
    void sameFileNamesShouldBeEqual() {
        File f1 = new File("a");
        File f2 = new File("a");
        int result = FileNameComparator.INSTANCE.compare(f1, f2);
        assertEquals(RESULT_SAME_NAME, result);
    }

    @Test
    void differentFileNamesShouldNotBeEqual() {
        assertNotEquals(RESULT_SAME_NAME,
                FileNameComparator.INSTANCE.compare(
                        new File("abc"),
                        new File("def")
                ));
    }

    @Test
    void sameFileShouldEqualItsOwnname() {
        File file = new File("test.txt");
        assertEquals(RESULT_SAME_NAME, FileNameComparator.INSTANCE.compare(file, file));
    }

}
