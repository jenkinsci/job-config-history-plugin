/*
 * The MIT License
 *
 * Copyright 2013 Mirko Friedenhagen, Kojima Takanori.
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

import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Chunk;
import com.github.difflib.patch.DiffException;
import com.github.difflib.patch.Patch;
import com.github.difflib.text.DiffRow;
import com.github.difflib.text.DiffRowGenerator;
import org.apache.commons.lang.StringUtils;

import java.util.List;

/**
 * Returns side-by-side (i.e. human-readable) diff view lines.
 *
 * @author Mirko Friedenhagen
 * @author Kojima Takanori
 */
public class GetDiffLines {

    /**
     * Lines.
     */
    private final List<String> diffLines;
    /**
     * View.
     */
    private final SideBySideView view;
    /**
     * Generator for diff rows.
     */
    private final DiffRowGenerator dfg;

    /**
     * Constructor.
     *
     * @param diffLines to construct the {@link SideBySideView} for.
     */
    public GetDiffLines(List<String> diffLines) {

        final DiffRowGenerator.Builder builder = DiffRowGenerator.create();
        builder.columnWidth(Integer.MAX_VALUE);
        dfg = builder.build();
        this.diffLines = diffLines;
        view = new SideBySideView();
    }

    /**
     * Returns a list of {@link SideBySideView} lines.
     *
     * @return list of {@link SideBySideView} lines.
     */
    public List<SideBySideView.Line> get() throws DiffException {

        final Patch<String> diff = UnifiedDiffUtils.parseUnifiedDiff(diffLines);
        int previousLeftPos = 0;
        for (final AbstractDelta<String> delta : diff.getDeltas()) {
            previousLeftPos = deltaLoop(delta, previousLeftPos);
        }
        view.clearDuplicateLines();
        return view.getLines();
    }

    /**
     * Extends view with lines of a single delta.
     *
     * @param delta           to inspect.
     * @param previousLeftPos indentation.
     * @return new previousLeftPos
     */
    int deltaLoop(final AbstractDelta<String> delta, int previousLeftPos) {
        return new DeltaLoop(view, dfg, delta).loop(previousLeftPos);
    }

    /**
     * DeltaLoop.
     */
    static class DeltaLoop {

        /**
         * View.
         */
        private final SideBySideView view;
        /**
         * Dfg.
         */
        private final DiffRowGenerator dfg;
        /**
         * delta.
         */
        private final AbstractDelta<?> delta;
        /**
         * Current leftPos.
         */
        private int leftPos;
        /**
         * Current rightPos.
         */
        private int rightPos;

        /**
         * @param view  to extend.
         * @param dfg   dfg
         * @param delta delta
         */
        public DeltaLoop(SideBySideView view, DiffRowGenerator dfg,
                         AbstractDelta<?> delta) {
            this.view = view;
            this.dfg = dfg;
            this.delta = delta;
        }

        /**
         * Loop through Delta.
         *
         * @param previousLeftPos previous indentation
         * @return current indentation
         */
        int loop(int previousLeftPos) {
            final Chunk<?> original = delta.getSource();
            final Chunk<?> revised = delta.getTarget();
            @SuppressWarnings("unchecked") final List<DiffRow> diffRows = dfg.generateDiffRows(
                    (List<String>) original.getLines(),
                    (List<String>) revised.getLines());
            // Chunk#getPosition() returns 0-origin line numbers, but we need
            // 1-origin line numbers
            leftPos = original.getPosition() + 1;
            rightPos = revised.getPosition() + 1;
            if (previousLeftPos > 0 && leftPos - previousLeftPos > 1) {
                final SideBySideView.Line skippingLine = new SideBySideView.Line();
                skippingLine.setSkipping(true);
                view.addLine(skippingLine);
            }
            for (final DiffRow row : diffRows) {
                previousLeftPos = processDiffRow(row);
            }
            return previousLeftPos;
        }

        /**
         * Processes one DiffRow.
         *
         * @param row to process
         * @return indentation
         */
        int processDiffRow(final DiffRow row) {
            final DiffRow.Tag tag = row.getTag();
            final SideBySideView.Line line = new SideBySideView.Line();
            final SideBySideView.Line.Item left = line.getLeft();
            final SideBySideView.Line.Item right = line.getRight();
            if (tag == DiffRow.Tag.INSERT) {
                left.setCssClass("diff_original");
                right.setLineNumber(rightPos);
                right.setText(row.getNewLine());
                right.setCssClass("diff_revised");
                rightPos++;
            } else if (tag == DiffRow.Tag.CHANGE) {
                if (StringUtils.isNotEmpty(row.getOldLine())) {
                    left.setLineNumber(leftPos);
                    left.setText(row.getOldLine());
                    leftPos++;
                }
                left.setCssClass("diff_original");
                if (StringUtils.isNotEmpty(row.getNewLine())) {
                    right.setLineNumber(rightPos);
                    right.setText(row.getNewLine());
                    rightPos++;
                }
                right.setCssClass("diff_revised");
            } else if (tag == DiffRow.Tag.DELETE) {
                left.setLineNumber(leftPos);
                left.setText(row.getOldLine());
                left.setCssClass("diff_original");
                leftPos++;
                right.setCssClass("diff_revised");
            } else if (tag == DiffRow.Tag.EQUAL) {
                left.setLineNumber(leftPos);
                left.setText(row.getOldLine());
                leftPos++;
                right.setLineNumber(rightPos);
                right.setText(row.getNewLine());
                rightPos++;
            } else {
                throw new IllegalStateException("Unknown tag pattern: " + tag);
            }
            line.setTag(tag);
            view.addLine(line);
            return leftPos;
        }

    }

}
