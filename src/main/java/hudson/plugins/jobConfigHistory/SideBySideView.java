/*
 * The MIT License
 *
 * Copyright 2013 Mirko Friedenhagen, Kojima Takanori
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

import com.github.difflib.text.DiffRow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

/**
 * Holds information for the SideBySideView.
 *
 * @author Kojima Takanori
 * @author Mirko Friedenhagen
 */
public class SideBySideView {
    /**
     * All lines of the view.
     */
    private final List<Line> lines = new ArrayList<>();

    /**
     * Returns the lines of the {@link SideBySideView}.
     *
     * @return an unmodifiable view of the lines.
     */
    public List<Line> getLines() {
        return Collections.unmodifiableList(lines);
    }

    /**
     * Adds a line.
     *
     * @param line A single line.
     */
    public void addLine(Line line) {
        lines.add(line);
    }

    /**
     * Deletes all dupes in the given lines.
     */
    public void clearDuplicateLines() {
        final TreeMap<Integer, Line> linesByNumbers = new TreeMap<>();
        final ArrayList<Line> dupes = new ArrayList<>();
        final Iterator<Line> iter = lines.iterator();
        while (iter.hasNext()) {
            final Line line = iter.next();
            final String lineNum = line.left.getLineNumber();
            if (lineNum.length() != 0) {
                final int lineNumInt = Integer.parseInt(lineNum);
                if (linesByNumbers.containsKey(lineNumInt)) {
                    if (line.getTag() == DiffRow.Tag.EQUAL) {
                        iter.remove();
                    } else {
                        dupes.add(linesByNumbers.get(lineNumInt));
                    }
                } else {
                    linesByNumbers.put(lineNumInt, line);
                }
            }
        }
        lines.removeAll(dupes);
    }

    /**
     * Holds information about a single line, which consists of the left and
     * right information of the diff.
     */
    public static class Line {

        /**
         * The left version of a modified line.
         */
        private final Item left = new Item();
        /**
         * The right version of a modified line.
         */
        private final Item right = new Item();
        /**
         * True when line should be skipped.
         */
        private boolean skipping;
        /**
         * EQUAL, INSERT, CHANGE or DELETE.
         */
        private DiffRow.Tag tag;

        /**
         * Returns the left version of a modified line.
         *
         * @return left item.
         */
        public Item getLeft() {
            return left;
        }

        /**
         * Returns the right version of a modified line.
         *
         * @return right item.
         */
        public Item getRight() {
            return right;
        }

        /**
         * Should we skip this line.
         *
         * @return true when the line should be skipped.
         */
        public boolean isSkipping() {
            return skipping;
        }

        /**
         * Sets skipping.
         *
         * @param skipping skip?
         */
        public void setSkipping(boolean skipping) {
            this.skipping = skipping;
        }

        /**
         * @return the tag
         */
        public DiffRow.Tag getTag() {
            return tag;
        }

        /**
         * @param tag the tag to set
         */
        public void setTag(DiffRow.Tag tag) {
            this.tag = tag;
        }

        /**
         * Simple representation of a diff element.
         * <p>
         * Additional to the text this includes the linenumber as well as
         * the corresponding cssClass which signals whether the item was
         * modified, added or deleted.
         */
        public static class Item {

            /**
             * Line number of Item.
             */
            private Integer lineNumber;
            /**
             * Text of Item.
             */
            private String text;
            /**
             * CSS Class of Item.
             */
            private String cssClass;

            /**
             * Returns the line number of the Item.
             *
             * @return lineNumber.
             */
            public String getLineNumber() {
                return lineNumber == null ? "" : String.valueOf(lineNumber);
            }

            /**
             * @param lineNumber the lineNumber to set
             */
            public void setLineNumber(Integer lineNumber) {
                this.lineNumber = lineNumber;
            }

            /**
             * Returns the text of the Item.
             *
             * @return text.
             */
            public String getText() {
                return text;
            }

            /**
             * @param text the text to set
             */
            public void setText(String text) {
                this.text = text;
            }

            /**
             * Returns the cssClass of the Item.
             *
             * @return cssClass.
             */
            public String getCssClass() {
                return cssClass;
            }

            /**
             * Sets cssClass.
             *
             * @param cssClass cssClass
             */
            public void setCssClass(String cssClass) {
                this.cssClass = cssClass;
            }
        }
    }

}
