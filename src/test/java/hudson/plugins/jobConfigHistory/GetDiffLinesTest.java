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

import com.github.difflib.patch.DiffException;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Mirko Friedenhagen
 */
public class GetDiffLinesTest {

    /**
     * Test of get method, of class GetDiffLines.
     */
    @Test
    public void testGet() throws IOException, DiffException {
        GetDiffLines sut = createGetDiffLines();
        List<SideBySideView.Line> result = sut.get();
        assertEquals(24, result.size());
        SideBySideView.Line firstLine = result.get(0);
        assertEquals("import bmsi.util.Diff;", firstLine.getLeft().getText());
        assertEquals("import bmsi.util.Diff;", firstLine.getRight().getText());
        SideBySideView.Line fourthLine = result.get(3);
        final SideBySideView.Line.Item left = fourthLine.getLeft();
        final SideBySideView.Line.Item right = fourthLine.getRight();
        assertEquals("3", right.getLineNumber());
        assertNull(left.getText());
        assertEquals("import org.kohsuke.stapler.StaplerRequest;",
                right.getText());
        assertEquals("diff_original", left.getCssClass());
        assertEquals("diff_revised", right.getCssClass());
    }

    GetDiffLines createGetDiffLines() throws IOException {
        final String resourceName = "diff.txt";
        final List<String> lines = TUtils.readResourceLines(resourceName);
        return new GetDiffLines(lines);
    }
}
