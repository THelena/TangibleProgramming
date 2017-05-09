package ee.ut.program;

import ee.ut.exceptions.NoStartPieceError;
import ee.ut.imageProcessing.ImageParser;
import ee.ut.imageProcessing.MatchPair;
import ee.ut.imageProcessing.OpenCVUtil;
import ee.ut.imageProcessing.SymbolStyle;
import ee.ut.program.elements.*;
import javafx.scene.image.Image;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import topcodes.TopCode;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static ee.ut.imageProcessing.PuzzlePiece.*;
import static ee.ut.program.elements.Condition.ISCARROT;
import static ee.ut.program.elements.Condition.ISTRAP;
import static ee.ut.program.elements.Condition.ISWALL;

public class ProgramGenerator {
    private List<MatchPair> matches;
    private int templateHeight;
    private String imageResource;
    private SymbolStyle symbolStyle;
    private Mat inputImageMat;
    private ImageParser imageParser;

    public ProgramGenerator(String imageResource, SymbolStyle symbolStyle) {
        this.imageResource = imageResource;
        this.symbolStyle = symbolStyle;
    }

    public static void main(String[] args) throws NoStartPieceError, IOException {
        ProgramGenerator programGenerator = new ProgramGenerator("customPrograms/4.jpg", SymbolStyle.CUSTOM);
        TreeNode treeNode = programGenerator.generateProgram();
    }

    private void updateParentsAndChildren(TreeNode parent, TreeNode child, boolean isTrueNode) {
        child.setParent(parent);
        if (parent instanceof UnaryNode) {
            ((UnaryNode) parent).setChild(child);
        } else if (parent instanceof IfStatement) {
            if (isTrueNode) ((IfStatement) parent).setTrueNode(child);
            else ((IfStatement) parent).setFalseNode(child);
        }
    }

    public TreeNode generateProgram() throws NoStartPieceError, IOException {
        imageParser = new ImageParser(symbolStyle, imageResource);
        matches = imageParser.getTemplateLocations();
        templateHeight = imageParser.getTemplateHeight();
        matches.sort(Comparator.comparing(x -> x.getLocation().x));
        inputImageMat = OpenCVUtil.readInputStreamIntoMat(getClass().getClassLoader().getResourceAsStream(imageResource));
        int indeks = 0;
        for (MatchPair matchPair : matches) {
            if (matchPair.getPuzzlePiece() == START) {
                break;
            } else {
                indeks++;
            }
        }
        return generateProgram(getMatchesForLevel(matches.remove(indeks)), new ArrayList<>(), false);
    }

    private List<MatchPair> getMatchesForLevel(MatchPair startingMatch) {
        List<MatchPair> result = new ArrayList<>(Collections.singletonList(startingMatch));
        for (MatchPair matchPair : matches) {
            MatchPair lastMatch = result.get(result.size() - 1);
            if (matchPair.getLocation().x < lastMatch.getLocation().x + 6 * templateHeight &&
                    matchPair.getLocation().y > lastMatch.getLocation().y - templateHeight &&
                    matchPair.getLocation().y < lastMatch.getLocation().y + 2 * templateHeight) result.add(matchPair);
        }
        matches = matches.stream().filter(x -> !result.contains(x)).collect(Collectors.toList());
        return result;
    }

    private MatchPair getArgumentForMove(MatchPair moveMatch) {
        MatchPair argumentMatch = null;
        for (MatchPair matchPair : matches) {
            if (matchPair.getLocation().x < moveMatch.getLocation().x + templateHeight &&
                    matchPair.getLocation().x > moveMatch.getLocation().x - templateHeight &&
                    matchPair.getLocation().y > moveMatch.getLocation().y &&
                    matchPair.getLocation().y < moveMatch.getLocation().y + 6 * templateHeight) {
                argumentMatch = matchPair;
                matches.remove(matchPair);
                break;
            }
        }
        return argumentMatch;
    }

    private Image generateImageForNode(Point coordinates) {
        Point location;
        if (symbolStyle == SymbolStyle.TOPCODES) {
            Point closestPoint = getClosestPointForTopCode(coordinates);
            Point topleftCorner = new Point(closestPoint.x - templateHeight / 2, closestPoint.y - templateHeight / 2);
            location = new Point(topleftCorner.x - 2 * templateHeight / 3, topleftCorner.y + templateHeight / 2);
        } else {
            location = new Point(coordinates.x - 2 * templateHeight / 3, coordinates.y + templateHeight / 2);
        }
        Mat clone = inputImageMat.clone();

        Imgproc.circle(clone, location, templateHeight / 3, new Scalar(0,0,255), -1);
        return OpenCVUtil.mat2Image(clone);
    }

    private Image generateImageForTwoNodes(Point coordinates, Point coordinates2) {
        Point location1;
        if (symbolStyle == SymbolStyle.TOPCODES) {
            Point closestPoint = getClosestPointForTopCode(coordinates);
            Point topleftCorner = new Point(closestPoint.x - templateHeight / 2, closestPoint.y - templateHeight / 2);
            location1 = new Point(topleftCorner.x - 2 * templateHeight / 3, topleftCorner.y + templateHeight / 2);
        } else {
            location1 = new Point(coordinates.x - 2 * templateHeight / 3, coordinates.y + templateHeight / 2);
        }

        Point location2;
        if (symbolStyle == SymbolStyle.TOPCODES) {
            Point closestPoint = getClosestPointForTopCode(coordinates2);
            Point topleftCorner = new Point(closestPoint.x - templateHeight / 2, closestPoint.y - templateHeight / 2);
            location2 = new Point(topleftCorner.x - 2 * templateHeight / 3, topleftCorner.y + templateHeight / 2);
        } else {
            location2 = new Point(coordinates2.x - 2 * templateHeight / 3, coordinates2.y + templateHeight / 2);
        }

        Mat clone = inputImageMat.clone();

        Imgproc.circle(clone, location1, templateHeight / 3, new Scalar(0,0,255), -1);
        Imgproc.circle(clone, location2, templateHeight / 3, new Scalar(0,0,255), -1);
        return OpenCVUtil.mat2Image(clone);
    }

    private Point getClosestPointForTopCode(Point coordinates) {
        List<TopCode> topCodes = imageParser.getScan();
        Point closest = null;
        double smallestDistance = 0;
        for (TopCode topCode : topCodes) {
            if (closest == null) {
                closest = new Point(topCode.getCenterX(), topCode.getCenterY());
                smallestDistance = Math.sqrt(Math.pow(coordinates.x - topCode.getCenterX(), 2) + Math.pow(coordinates.y - topCode.getCenterY(), 2));
            }

            double newDistance = Math.sqrt(Math.pow(coordinates.x - topCode.getCenterX(), 2) + Math.pow(coordinates.y - topCode.getCenterY(), 2));
            if (newDistance < smallestDistance) {
                closest = new Point(topCode.getCenterX(), topCode.getCenterY());
                smallestDistance = newDistance;
            }
        }
        return closest;
    }

    private TreeNode generateProgram(List<MatchPair> matchesForLevel, List<Land> lands, boolean isTrueNode) {
        TreeNode currentNode = null;
        TreeNode parentNode = null;
        Condition ifStatementCondition = null;
        Point ifStatementConditionLocation = null;
        while (!matchesForLevel.isEmpty()) {
            MatchPair pair = matchesForLevel.remove(0);

            if (pair.getPuzzlePiece() == START) {
                Start start = new Start();
                start.setImageWithADot(generateImageForNode(pair.getLocation()));
                parentNode = start;
                currentNode = start;
            } else if (pair.getPuzzlePiece() == MOVE) {
                Move move = new Move();
                MatchPair argumentForMove = getArgumentForMove(pair);

                if (argumentForMove != null) {
                    if (argumentForMove.getPuzzlePiece() == NUMBER3) move.setSteps(3);
                    else if (argumentForMove.getPuzzlePiece() == NUMBER4) move.setSteps(4);
                    move.setImageWithADot(generateImageForTwoNodes(pair.getLocation(), argumentForMove.getLocation()));
                } else {
                    move.setImageWithADot(generateImageForNode(pair.getLocation()));
                }

                updateParentsAndChildren(currentNode, move, isTrueNode);
                currentNode = move;
                if (parentNode == null) parentNode = move;
            } else if (pair.getPuzzlePiece() == LEFT) {
                Turn left = new Turn(Direction.LEFT);
                left.setImageWithADot(generateImageForNode(pair.getLocation()));
                updateParentsAndChildren(currentNode, left, isTrueNode);
                currentNode = left;
                if (parentNode == null) parentNode = left;
            } else if (pair.getPuzzlePiece() == RIGHT) {
                Turn right = new Turn(Direction.RIGHT);
                right.setImageWithADot(generateImageForNode(pair.getLocation()));
                updateParentsAndChildren(currentNode, right, isTrueNode);
                currentNode = right;
                if (parentNode == null) parentNode = right;
            } else if (pair.getPuzzlePiece() == LANDX) {
                Land land = new Land('X');
                land.setImageWithADot(generateImageForNode(pair.getLocation()));
                updateParentsAndChildren(currentNode, land, isTrueNode);
                lands.add(land);
                currentNode = land;
                if (parentNode == null) parentNode = land;
            } else if (pair.getPuzzlePiece() == LANDY) {
                Land land = new Land('Y');
                land.setImageWithADot(generateImageForNode(pair.getLocation()));
                updateParentsAndChildren(currentNode, land, isTrueNode);
                lands.add(land);
                currentNode = land;
                if (parentNode == null) parentNode = land;
            } else if (pair.getPuzzlePiece() == JUMPX) {
                Jump jump = new Jump('X');
                jump.setImageWithADot(generateImageForNode(pair.getLocation()));

                for (Land land : lands) {
                    if (land.getSymbol() == jump.getSymbol()) {
                        jump.setLand(land);
                        break;
                    }
                }

                updateParentsAndChildren(currentNode, jump, isTrueNode);
                currentNode = jump;
                if (parentNode == null) parentNode = jump;
            } else if (pair.getPuzzlePiece() == JUMPY) {
                Jump jump = new Jump('Y');
                jump.setImageWithADot(generateImageForNode(pair.getLocation()));

                for (Land land : lands) {
                    if (land.getSymbol() == jump.getSymbol()) {
                        jump.setLand(land);
                        break;
                    }
                }

                updateParentsAndChildren(currentNode, jump, isTrueNode);
                currentNode = jump;
                if (parentNode == null) parentNode = jump;
            } else if (pair.getPuzzlePiece() == STOP) {
                Stop stop = new Stop();
                stop.setImageWithADot(generateImageForNode(pair.getLocation()));
                updateParentsAndChildren(currentNode, stop, isTrueNode);
                currentNode = stop;
                if (parentNode == null) parentNode = stop;
            } else if (pair.getPuzzlePiece() == IF) {
                IfStatement ifStatement = new IfStatement(ifStatementCondition);

                if (ifStatementCondition != null) {
                    ifStatement.setImageWithADot(generateImageForTwoNodes(pair.getLocation(), ifStatementConditionLocation));
                } else {
                    ifStatement.setImageWithADot(generateImageForNode(pair.getLocation()));
                }

                List<MatchPair> transitions = getTransitionsForIf(pair);
                MatchPair trueTransition = transitions.get(0);
                MatchPair falseTransition = transitions.get(1);

                if (trueTransition == null) {
                    ifStatement.setTrueNode(null);
                } else {
                    List<MatchPair> matchesForTrueLevel = getMatchesForLevel(trueTransition);
                    matchesForTrueLevel.remove(0);

                    TreeNode trueNode = generateProgram(matchesForTrueLevel, lands, true);
                    ifStatement.setTrueNode(trueNode);
                }

                if (falseTransition == null) {
                    ifStatement.setFalseNode(null);
                } else {
                    List<MatchPair> matchesForFalseLevel = getMatchesForLevel(falseTransition);
                    matchesForFalseLevel.remove(0);

                    TreeNode falseNode = generateProgram(matchesForFalseLevel, lands, false);
                    ifStatement.setFalseNode(falseNode);
                }

                updateParentsAndChildren(currentNode, ifStatement, isTrueNode);
                currentNode = ifStatement;
                if (parentNode == null) parentNode = ifStatement;
            } else if (pair.getPuzzlePiece() == WALL) {
                ifStatementCondition = ISWALL;
                ifStatementConditionLocation = pair.getLocation();
            } else if (pair.getPuzzlePiece() == TRAP) {
                ifStatementCondition = ISTRAP;
                ifStatementConditionLocation = pair.getLocation();
            } else if (pair.getPuzzlePiece() == CARROT) {
                ifStatementCondition = ISCARROT;
                ifStatementConditionLocation = pair.getLocation();
            }
        }
        return parentNode;
    }

    private List<MatchPair> getTransitionsForIf(MatchPair ifMatch) {
        MatchPair trueTransition = null;
        MatchPair falseTransition = null;

        List<MatchPair> transitions = matches.stream().filter(x -> x.getPuzzlePiece() == TRANSITION).collect(Collectors.toList());

        for (MatchPair transition : transitions) {
            if (transition.getLocation().x < (ifMatch.getLocation().x + templateHeight) && transition.getLocation().x > (ifMatch.getLocation().x - templateHeight)) {

                if (trueTransition == null && transition.getLocation().y < ifMatch.getLocation().y) {
                    trueTransition = transition;
                } else if (falseTransition == null && transition.getLocation().y > ifMatch.getLocation().y) {
                    falseTransition = transition;
                }

                if (transition.getLocation().y < ifMatch.getLocation().y) {
                    if (transition.getLocation().y > trueTransition.getLocation().y) trueTransition = transition;
                } else {
                    if (transition.getLocation().y < falseTransition.getLocation().y) falseTransition = transition;
                }
            }
        }
        matches.remove(trueTransition);
        matches.remove(falseTransition);
        return new ArrayList<>(Arrays.asList(trueTransition, falseTransition));
    }
}
