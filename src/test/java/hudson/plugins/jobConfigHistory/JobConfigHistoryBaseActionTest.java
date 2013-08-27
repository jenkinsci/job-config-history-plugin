/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hudson.plugins.jobConfigHistory;

import hudson.model.Hudson;
import hudson.security.AccessControlled;
import java.io.File;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import org.junit.Ignore;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author Mirko Friedenhagen
 */
public class JobConfigHistoryBaseActionTest {

    private final Hudson hudsonMock = mock(Hudson.class);
    
    /**
     * Test of getDisplayName method, of class JobConfigHistoryBaseAction.
     */
    @Test
    public void testGetDisplayName() {
        System.out.println("getDisplayName");
        JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
        String expResult = "Job Config History";
        String result = sut.getDisplayName();
        assertEquals(expResult, result);
    }

    /**
     * Test of getUrlName method, of class JobConfigHistoryBaseAction.
     */
    @Test
    public void testGetUrlName() {
        JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
        String expResult = "jobConfigHistory";
        String result = sut.getUrlName();
        assertEquals(expResult, result);
    }

    /**
     * Test of getOutputType method, of class JobConfigHistoryBaseAction.
     */
    @Test
    @Ignore
    public void testGetOutputType() {
        System.out.println("getOutputType");
        JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
        String expResult = "";
        String result = sut.getOutputType();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of checkTimestamp method, of class JobConfigHistoryBaseAction.
     */
    @Test
    public void testCheckTimestamp() {
        JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
        assertFalse(sut.checkTimestamp("null"));
        assertFalse(sut.checkTimestamp(null));
        assertTrue(sut.checkTimestamp("2013-08-31_23-59-59"));
    }

    /**
     * Test of getRequestParameter method, of class JobConfigHistoryBaseAction.
     */
    @Test
    @Ignore
    public void testGetRequestParameter() {
        System.out.println("getRequestParameter");
        String parameterName = "";
        JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
        String expResult = "";
        String result = sut.getRequestParameter(parameterName);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of checkConfigurePermission method, of class JobConfigHistoryBaseAction.
     */
    @Test
    public void testCheckConfigurePermission() {
        JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
        sut.checkConfigurePermission();
    }

    /**
     * Test of hasConfigurePermission method, of class JobConfigHistoryBaseAction.
     */
    @Test
    public void testHasConfigurePermission() {
        JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
        boolean expResult = false;
        boolean result = sut.hasConfigurePermission();
        assertEquals(expResult, result);
    }

    /**
     * Test of getHudson method, of class JobConfigHistoryBaseAction.
     */
    @Test
    public void testGetHudson() {
        System.out.println("getHudson");
        JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
        Hudson expResult = hudsonMock;
        Hudson result = sut.getHudson();
        assertEquals(expResult, result);
    }

    /**
     * Test of getAccessControlledObject method, of class JobConfigHistoryBaseAction.
     */
    @Test
    public void testGetAccessControlledObject() {
        JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
        AccessControlled expResult = null;
        AccessControlled result = sut.getAccessControlledObject();
        assertEquals(expResult, result);
    }

    /**
     * Test of getDiffLines method, of class JobConfigHistoryBaseAction.
     */
    @Test
    @Ignore
    public void testGetDiffLines() throws Exception {
        System.out.println("getDiffLines");
        List<String> diffLines = null;
        JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
        List<JobConfigHistoryBaseAction.SideBySideView.Line> expResult = null;
        List<JobConfigHistoryBaseAction.SideBySideView.Line> result = sut.getDiffLines(diffLines);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getDiffAsString method, of class JobConfigHistoryBaseAction.
     */
    @Test
    @Ignore
    public void testGetDiffAsString() {
        System.out.println("getDiffAsString");
        File file1 = null;
        File file2 = null;
        String[] file1Lines = null;
        String[] file2Lines = null;
        JobConfigHistoryBaseAction sut = new JobConfigHistoryBaseActionImpl();
        String expResult = "";
        String result = sut.getDiffAsString(file1, file2, file1Lines, file2Lines);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    public class JobConfigHistoryBaseActionImpl extends JobConfigHistoryBaseAction {

        public JobConfigHistoryBaseActionImpl() {
            super(hudsonMock);
        }
                
        public void checkConfigurePermission() {
        }

        public boolean hasConfigurePermission() {
            return false;
        }

        public AccessControlled getAccessControlledObject() {
            return null;
        }

        public String getIconFileName() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
    
}
