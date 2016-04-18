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
 * Filters all directories not ending on "jobs".
 *
 * @author Mirko Friedenhagen
 */
class NonJobsDirectoryFileFilter implements FileFilter {

    /** Only one instance needed. */
    static final NonJobsDirectoryFileFilter INSTANCE = new NonJobsDirectoryFileFilter();

    @Override
    public boolean accept(File pathname) {
        return pathname.isDirectory() && !pathname.getName().endsWith("jobs");
    }

    /**
     * Is this file not a job history directory?
     *
     * @param file to inspect
     * @return true when directory is not a jobs history directory.
     */
    public static boolean accepts(File file) {
        return INSTANCE.accept(file);
    }
}
