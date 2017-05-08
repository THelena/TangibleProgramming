package ee.ut.program;

public class UnaryNode extends TreeNode {
    private TreeNode child;

    public UnaryNode() {
    }

    public UnaryNode(TreeNode child) {
        this.child = child;
    }

    public TreeNode getChild() {
        return child;
    }

    public void setChild(TreeNode child) {
        this.child = child;
    }
}
