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

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * @author Mirko Friedenhagen
 */
public class TUtils {

    public static ServletInputStream createServletInputStreamFromMultiPartFormData(
            final String boundary) {
        final String body = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"timestamp1\"\r\n\r\n"
                + "2014-02-05_10-42-37\r\n" + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"timestamp2\"\r\n\r\n"
                + "2014-03-12_11-02-12\r\n" + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"name\"\r\n\r\n"
                + "foo\r\n" + "--" + boundary + "--\r\n";
        final ByteArrayInputStream bodyByteStream = new ByteArrayInputStream(
                body.getBytes());
        return new ServletInputStream() {
            @Override
            public int read() {
                return bodyByteStream.read();
            }

            @Override
            public boolean isFinished() {
                // Not needed.
                return false;
            }

            @Override
            public boolean isReady() {
                // Not needed.
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // Not needed.

            }
        };
    }

    static List<String> readResourceLines(final String resourceName)
            throws IOException {
        try (InputStream stream = TUtils.class
                .getResourceAsStream(resourceName)) {
            return IOUtils.readLines(stream, "UTF-8");
        }
    }

    /**
     * Checks if the path ends by the specified suffix. The method converts
     * actual values to system path using {@link FilePathSuffixMatcher}.
     *
     * @param expectedSuffix The expected suffix
     * @return A matcher for further comparison.
     */
    public static Matcher<String> pathEndsWith(String expectedSuffix) {
        return new FilePathSuffixMatcher(expectedSuffix);
    }

    /**
     * A suffix {@link Matcher}, which automatically handles file separators.
     *
     * @since TODO: define a version
     */
    public static class FilePathSuffixMatcher implements Matcher<String> {

        private final String expectedSuffix;

        public FilePathSuffixMatcher(String expectedSuffix) {
            this.expectedSuffix = FilenameUtils
                    .separatorsToSystem(expectedSuffix);
        }

        @Override
        public boolean matches(Object actual) {
            if (actual instanceof String) {
                String actualSystemPath = FilenameUtils
                        .separatorsToSystem((String) actual);
                return actualSystemPath.endsWith(expectedSuffix);
            }
            return false;
        }

        @Override
        public void _dont_implement_Matcher___instead_extend_BaseMatcher_() {
            // nop
        }

        @Override
        public void describeTo(Description d) {
            d.appendText("path with suffix " + expectedSuffix);
        }

        @Override
        public void describeMismatch(Object o, Description d) {
            d.appendText("path " + o + " has no such suffix");
        }
    }
}
