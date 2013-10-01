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

import hudson.model.AbstractProject;
import hudson.model.Build;
import hudson.model.ItemGroup;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Mirko Friedenhagen
 */
public class JobConfigBadgeActionTest {

    private static final String ROOT_URL = "http://example.org/jenkins";
    private final JobConfigHistory mockedPlugin = mock(JobConfigHistory.class);
    private final HistoryDao mockedHistoryDao = mock(HistoryDao.class);
    private final Build mockedBuild = mock(Build.class);
    private final AbstractProject mockedProject = mock(AbstractProject.class);
    private final String[] configDates = {"2013_01_01", "2013_01_02"};
    private JobConfigBadgeAction sut = createSut();

    public JobConfigBadgeActionTest() {
        final ItemGroup mockedItemGroup = mock(ItemGroup.class);
        when(mockedItemGroup.getUrl()).thenReturn("/jobs/");
        when(mockedProject.getParent()).thenReturn(mockedItemGroup);
        when(mockedProject.getShortUrl()).thenReturn("jobname");
        when(mockedBuild.getProject()).thenReturn(mockedProject);
    }

    /**
     * Test of onAttached method, of class JobConfigBadgeAction.
     */
    @Test
    public void testOnAttached() {
        sut.onAttached(mockedBuild);
    }

    /**
     * Test of onLoad method, of class JobConfigBadgeAction.
     */
    @Test
    public void testOnLoad() {
        sut.onLoad(mockedBuild);
    }

    /**
     * Test of showBadge method, of class JobConfigBadgeAction.
     */
    @Test
    public void testShowBadge() {
        boolean expResult = false;
        when(mockedPlugin.showBuildBadges(mockedProject)).thenReturn(expResult);
        sut.onAttached(mockedBuild);
        boolean result = sut.showBadge();
        assertEquals(expResult, result);
    }

    /**
     * Test of oldConfigsExist method, of class JobConfigBadgeAction.
     */
    @Test
    public void testOldConfigsExist() {
        when(mockedHistoryDao.hasOldRevision(mockedProject, configDates[0])).thenReturn(true);
        when(mockedHistoryDao.hasOldRevision(mockedProject, configDates[1])).thenReturn(true);
        boolean expResult = true;
        sut.onAttached(mockedBuild);
        boolean result = sut.oldConfigsExist();
        assertEquals(expResult, result);
    }

    @Test
    public void testOldConfigsExistFalse() {
        when(mockedHistoryDao.hasOldRevision(mockedProject, configDates[0])).thenReturn(true);
        when(mockedHistoryDao.hasOldRevision(mockedProject, configDates[1])).thenReturn(false);
        boolean expResult = false;
        sut.onAttached(mockedBuild);
        boolean result = sut.oldConfigsExist();
        assertEquals(expResult, result);
    }

    /**
     * Test of createLink method, of class JobConfigBadgeAction.
     */
    @Test
    public void testCreateLink() {
        String expResult = "http://example.org/jenkins/jobs/jobnamejobConfigHistory/showDiffFiles?timestamp1=2013_01_02&timestamp2=2013_01_01";
        sut.onAttached(mockedBuild);
        String result = sut.createLink();
        assertEquals(expResult, result);
    }

    /**
     * Test of getTooltip method, of class JobConfigBadgeAction.
     */
    @Test
    public void testGetTooltip() {
        String expResult = "Config changed since last build";
        String result = sut.getTooltip();
        assertEquals(expResult, result);
    }

    /**
     * Test of getIcon method, of class JobConfigBadgeAction.
     */
    @Test
    public void testGetIcon() {
        String expResult = "/plugin/jobConfigHistory/img/buildbadge.png";
        String result = sut.getIcon();
        assertEquals(expResult, result);
    }

    /**
     * Test of getIconFileName method, of class JobConfigBadgeAction.
     */
    @Test
    public void testGetIconFileName() {
        String result = sut.getIconFileName();
        assertNull(result);
    }

    /**
     * Test of getDisplayName method, of class JobConfigBadgeAction.
     */
    @Test
    public void testGetDisplayName() {
        String result = sut.getDisplayName();
        assertNull(result);
    }

    /**
     * Test of getUrlName method, of class JobConfigBadgeAction.
     */
    @Test
    public void testGetUrlName() {
        String expResult = "";
        String result = sut.getUrlName();
        assertEquals(expResult, result);
    }

    JobConfigBadgeAction createSut() {
        return createSut(configDates);
    }

    JobConfigBadgeAction createSut(final String[] theConfigDates) {
        return new JobConfigBadgeAction(theConfigDates) {

            @Override
            JobConfigHistory getPlugin() {
                return mockedPlugin;
            }

            @Override
            HistoryDao getHistoryDao() {
                return mockedHistoryDao;
            }

            @Override
            String getRootUrl() {
                return ROOT_URL;
            }

        };
    }

}
