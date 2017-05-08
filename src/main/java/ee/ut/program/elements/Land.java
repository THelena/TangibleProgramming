package ee.ut.program.elements;

import ee.ut.program.UnaryNode;

public class Land extends UnaryNode {

    private char symbol;

    public Land() {
    }

    public Land(char symbol) {
        this.symbol = symbol;
    }

    public char getSymbol() {
        return symbol;
    }

    public void setSymbol(char symbol) {
        this.symbol = symbol;
    }
}
