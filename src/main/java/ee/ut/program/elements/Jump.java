package ee.ut.program.elements;

import ee.ut.program.UnaryNode;

public class Jump extends UnaryNode{
    private Land land;
    private char symbol;

    public Jump() {
    }

    public Jump(char symbol) {
        this.symbol = symbol;
    }

    public char getSymbol() {
        return symbol;
    }

    public void setSymbol(char symbol) {
        this.symbol = symbol;
    }

    public Land getLand() {
        return land;
    }

    public void setLand(Land land) {
        this.land = land;
    }
}
