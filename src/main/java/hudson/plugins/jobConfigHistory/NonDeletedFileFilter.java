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
import java.io.FileFilter;

/**
 * A filter to return only those directories of a file listing that do not
 * represent deleted jobs history directories, the names of which do not contain
 * {@link DeletedFileFilter#DELETED_MARKER}.
 *
 * @author Mirko Friedenhagen
 */
class NonDeletedFileFilter implements FileFilter {

    /**
     * Only one instance needed.
     */
    static final NonDeletedFileFilter INSTANCE = new NonDeletedFileFilter();

    /**
     * Is this item not deleted?
     *
     * @param file to inspect
     * @return true when file does not have the special deleted mark.
     */
    public static boolean accepts(File file) {
        return INSTANCE.accept(file);
    }

    @Override
    public boolean accept(File pathname) {
        return !DeletedFileFilter.accepts(pathname);
    }

}
