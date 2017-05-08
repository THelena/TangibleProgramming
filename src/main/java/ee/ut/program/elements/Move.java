package ee.ut.program.elements;

import ee.ut.program.UnaryNode;

public class Move extends UnaryNode {
    private int steps;

    public Move(int steps) {
        this.steps = steps;
    }

    public int getSteps() {
        return steps;
    }

    public Move() {
        this.steps = 1;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }
}
