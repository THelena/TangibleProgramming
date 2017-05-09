package ee.ut.imageProcessing;

public enum PuzzlePiece {
    START("start.png", 109),
    STOP("stop.png", 115),
    MOVE("move.png", 47),
    IF("if.png", 31),
    WALL("wall.png",61),
    TRAP("trap.png", 79),
    CARROT("carrot.png", 167),
    JUMPX("jumpX.png", 91),
    LANDX("landX.png", 103),
    JUMPY("jumpY.png", 93),
    LANDY("landY.png", 107),
    LEFT("left.png", 55),
    RIGHT("right.png", 59),
    NUMBER3("number3.png", 143),
    NUMBER4("number4.png", 151),
    TRANSITION("transition.png", 87);

    private final String fileName;
    private final int topCode;

    PuzzlePiece(String fileName, int topCode) {
        this.fileName = fileName;
        this.topCode = topCode;
    }

    public String getFileName() {
        return fileName;
    }

    public int getTopCode() {
        return topCode;
    }
}
