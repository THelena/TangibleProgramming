package ee.ut.program.elements;

import ee.ut.program.UnaryNode;

public class Turn extends UnaryNode {

    private Direction turnDirection;

    public Turn(Direction turnDirection) {
        this.turnDirection = turnDirection;
    }

    public Direction getDirection() {
        return turnDirection;
    }

    public Direction getNextDirection(Direction oldirection) {
        switch (oldirection) {
            case RIGHT:
                return turnDirection == Direction.LEFT ? Direction.BACK : Direction.FRONT;
            case LEFT:
                return turnDirection == Direction.LEFT ? Direction.FRONT : Direction.BACK;
            case FRONT:
                return turnDirection == Direction.LEFT ? Direction.RIGHT : Direction.LEFT;
            case BACK:
                return turnDirection == Direction.LEFT ? Direction.LEFT : Direction.RIGHT;
            default:
                throw new RuntimeException("Couldn't find the next direction.");
        }
    }
}
