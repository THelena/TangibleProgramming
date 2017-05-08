package ee.ut.imageProcessing;

import org.opencv.core.Point;

public class MatchPair {
    private PuzzlePiece puzzlePiece;
    private Point location;

    public MatchPair(PuzzlePiece puzzlePiece, Point location) {
        this.puzzlePiece = puzzlePiece;
        this.location = location;
    }

    public PuzzlePiece getPuzzlePiece() {
        return puzzlePiece;
    }

    public void setPuzzlePiece(PuzzlePiece puzzlePiece) {
        this.puzzlePiece = puzzlePiece;
    }

    public Point getLocation() {
        return location;
    }

    public void setLocation(Point location) {
        this.location = location;
    }

    @Override
    public String toString() {
        return "MatchPair{" +
                "puzzlePiece=" + puzzlePiece +
                ", location=" + location +
                '}';
    }
}
