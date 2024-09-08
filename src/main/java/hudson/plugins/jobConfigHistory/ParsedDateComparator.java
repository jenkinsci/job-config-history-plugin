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

import java.io.Serializable;
import java.util.Comparator;

/**
 * Comparator for {@link ParsedDate}, sort order depends on
 * {@link ParsedDate#parsedDate()}.
 * <p>
 * Sort in descending order.
 *
 * @author Mirko Friedenhagen
 */
final public class ParsedDateComparator
        implements
        Comparator<ParsedDate>,
        Serializable {


    /**
     * No need to create more than one instance for descending.
     */
    public static final ParsedDateComparator DESCENDING = new ParsedDateComparator();
    private static final long serialVersionUID = 3193386602761561750L;

    /**
     * {@inheritDoc}
     */
    public int compare(final ParsedDate ci1, final ParsedDate ci2) {
        return ci2.parsedDate().compareTo(ci1.parsedDate());
    }
}
