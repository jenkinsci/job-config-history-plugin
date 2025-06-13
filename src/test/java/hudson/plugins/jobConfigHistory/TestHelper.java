/**
 * Misc. helper functions for testing
 */
package hudson.plugins.jobConfigHistory;

import org.htmlunit.WebClientUtil;
import org.htmlunit.html.HtmlDivision;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlPage;

class TestHelper {

    public static void triggerValidation(HtmlElement el) {
        el.fireEvent("change");
        WebClientUtil.waitForJSExec(el.getPage().getWebClient());
    }

    public static String getFormError(HtmlPage p) {
        HtmlDivision div = p.querySelector("div.error");
        if (null != div) {
            return div.getTextContent();
        }
        return "";
    }

    public static String getFormWarning(HtmlPage p) {
        HtmlDivision div = p.querySelector("div.warning");
        if (null != div) {
            return div.getTextContent();
        }
        return "";
    }
}
