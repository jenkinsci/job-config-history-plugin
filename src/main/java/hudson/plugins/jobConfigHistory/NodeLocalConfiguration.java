package hudson.plugins.jobConfigHistory;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.Util;
import hudson.XmlFile;
import hudson.model.AbstractProject;
import hudson.model.Node;
import hudson.model.Descriptor.FormException;
import hudson.slaves.AbstractCloudSlave;
import hudson.slaves.EphemeralNode;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.slaves.SlaveComputer;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerRequest2;

import java.util.logging.Logger;

public class NodeLocalConfiguration extends NodeProperty<Node> {

    static final Logger LOG = Logger
            .getLogger(NodeLocalConfiguration.class.getName());

    private static final Map<Node, String> lastChangeReasonCommentByNode = Collections.synchronizedMap(new HashMap<>());

    static Optional<String> lastChangeReasonComment(Node node) {
        return Optional.ofNullable(lastChangeReasonCommentByNode.remove(node));
    }

    private final String changeReasonComment;


    @DataBoundConstructor
    public NodeLocalConfiguration(String changeReasonComment) {
        this.changeReasonComment = changeReasonComment;
    }

    public String getChangeReasonComment() {
        return "";
    }

    @Extension
    public static class DescriptorImpl extends NodePropertyDescriptor {

        public DescriptorImpl() {
            super(NodeLocalConfiguration.class);
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "changeReasonComment_holder";
        }

        public boolean isApplicable(Node n) {
            return n != null && !(n instanceof AbstractCloudSlave
                || n instanceof EphemeralNode);
        }

        @Override
        @SuppressFBWarnings(value = "NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", justification = "JavaDoc says it is always Non-null")
        public NodeProperty<Node> newInstance(StaplerRequest2 req, JSONObject formData) throws FormException {
            NodeLocalConfiguration nlc = (NodeLocalConfiguration) super.newInstance(req, formData);
            SlaveComputer sc = req.findAncestorObject(SlaveComputer.class);
            Node n = null == sc ? null : sc.getNode();
            if (isApplicable(n)) {
                lastChangeReasonCommentByNode.put(n, Util.fixEmptyAndTrim(nlc.changeReasonComment));
            }
            return null;
        }

        @Override
        public boolean configure(StaplerRequest2 request, JSONObject jsonObject) throws FormException {
            throw new FormException("form exception", "localValues.changeReasonComment");
        }

        public boolean getShowChangeReasonCommentWindow() {
            return PluginUtils.getPlugin().getShowChangeReasonCommentWindow();
        }

        public boolean getChangeReasonCommentIsMandatory() {
            return PluginUtils.getPlugin().getChangeReasonCommentIsMandatory();
        }

        public boolean isDialogEnabled() {
            return !PluginUtils.isUserExcluded(PluginUtils.getPlugin()) && getShowChangeReasonCommentWindow();
        }
    }
}
