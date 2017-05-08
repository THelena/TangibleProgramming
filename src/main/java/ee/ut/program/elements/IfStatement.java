package ee.ut.program.elements;

import ee.ut.program.BinaryNode;
import ee.ut.program.TreeNode;

public class IfStatement extends BinaryNode {

    private Condition condition;

    public IfStatement(Condition condition) {
        this.condition = condition;
    }

    public Condition getCondition() {
        return condition;
    }

    public TreeNode getTrueNode() {
        return super.getRightChild();
    }

    public void setTrueNode(TreeNode trueNode) {
        super.setRightChild(trueNode);
    }

    public TreeNode getFalseNode() {
        return super.getLeftChild();
    }

    public void setFalseNode(TreeNode falseNode) {
        super.setLeftChild(falseNode);
    }
}