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

import com.thoughtworks.xstream.mapper.CannotResolveClassException;
import hudson.XmlFile;

import java.io.IOException;

/**
 * Lazy loader for HistoryDescr as preparation for paging.
 *
 * @author Mirko Friedenhagen
 */
public class LazyHistoryDescr extends HistoryDescr {

    private final XmlFile historyDescriptionFile;
    HistoryDescr historyDescr = HistoryDescr.EMPTY_HISTORY_DESCR;

    public LazyHistoryDescr(XmlFile historyDescriptionFile) {
        super(null, null, null, null, null, null);
        this.historyDescriptionFile = historyDescriptionFile;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public String getUser() {
        return loadAndGetHistory().getUser();
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public String getUserID() {
        return loadAndGetHistory().getUserID();
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public String getOperation() {
        return loadAndGetHistory().getOperation();
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public String getCurrentName() {
        return loadAndGetHistory().getCurrentName();
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public String getOldName() {
        return loadAndGetHistory().getOldName();
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public String getTimestamp() {
        return loadAndGetHistory().getTimestamp();
    }


    /**
     * {@inheritDoc}.
     */
    @Override
    public String getChangeReasonComment() {
        return loadAndGetHistory().getChangeReasonComment();
    }

    /**
     * Loads configurations on first access of any property.
     *
     * @return historyDescr
     */
    private HistoryDescr loadAndGetHistory() {
        if (historyDescr == HistoryDescr.EMPTY_HISTORY_DESCR) {
            try {
                historyDescr = (HistoryDescr) historyDescriptionFile.read();
            } catch (IOException ex) {
                throw new RuntimeException(
                        "Unable to read " + historyDescriptionFile.getFile(),
                        ex);
            } catch (CannotResolveClassException ex) {
                throw new RuntimeException(historyDescriptionFile.getFile()
                        + " is not a history description", ex);
            }
        }
        return historyDescr;
    }

}
