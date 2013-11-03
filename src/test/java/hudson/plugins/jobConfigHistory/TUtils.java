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

import hudson.util.IOUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.servlet.ServletInputStream;

/**
 *
 * @author Mirko Friedenhagen
 */
public class TUtils {

    static ServletInputStream createServletInputStreamFromMultiPartFormData(final String boundary) {
        final String body =
                "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"timestamp1\"\r\n\r\n" +
                "111\r\n" +
                "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"timestamp2\"\r\n\r\n" +
                "112\r\n" +
                "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"name\"\r\n\r\n" +
                "foo\r\n" +
                "--" + boundary + "--\r\n";
        final ByteArrayInputStream bodyByteStream = new ByteArrayInputStream(body.getBytes());
        return new ServletInputStream() {
            @Override
            public int read() throws IOException {
                return bodyByteStream.read();
            }
        };
    }

    static List<String> readResourceLines(final String resourceName) throws IOException {
        final InputStream stream = TUtils.class.getResourceAsStream(resourceName);
        try {
            return IOUtils.readLines(stream, "UTF-8");
        } finally {
            stream.close();
        }
    }

}
