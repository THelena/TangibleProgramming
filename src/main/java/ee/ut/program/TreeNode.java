package ee.ut.program;

import javafx.scene.image.Image;

public abstract class TreeNode {
    private TreeNode parent;
    private TreeNode child;
    private Image imageWithADot;

    public TreeNode() {

    }

    public Image getImageWithADot() {
        return imageWithADot;
    }

    public void setImageWithADot(Image imageWithADot) {
        this.imageWithADot = imageWithADot;
    }

    public TreeNode(TreeNode parent) {
        this.parent = parent;
    }

    public TreeNode getParent() {
        return parent;
    }

    public void setParent(TreeNode parent) {
        this.parent = parent;
    }
}
