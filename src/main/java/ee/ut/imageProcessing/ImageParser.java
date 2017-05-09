package ee.ut.imageProcessing;

import ee.ut.exceptions.NoStartPieceError;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import topcodes.Scanner;
import topcodes.TopCode;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.*;

import static ee.ut.imageProcessing.PuzzlePiece.*;
import static ee.ut.imageProcessing.SymbolStyle.TOPCODES;

public class ImageParser {
    private Mat inputImage;
    private SymbolStyle symbolStyle;
    private int templateHeight;
    private final String templateFolder = "symbols";
    private int match_method = Imgproc.TM_CCORR_NORMED;
    private String imageResource;
    private Map<PuzzlePiece, Mat> argumentModifiedTemplates;
    private Float orientation;
    private List<TopCode> scan;

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public ImageParser(SymbolStyle symbolStyle, String imageResource) {
        this.symbolStyle = symbolStyle;
        this.imageResource = imageResource;
    }

    public float getOrientation() {
        return orientation;
    }

    public int getTemplateHeight() {
        return templateHeight;
    }

    public List<TopCode> getScan() {
        return scan;
    }

    /**
     * Scans the images for topcodes and rotates its coordinates to correspond to a horizontal image
     * @return
     */
    private List<MatchPair> getLocationsForTopcodes() throws NoStartPieceError {
        Scanner scanner = new Scanner();
        try {
            scan = scanner.scan(ImageIO.read(getClass().getClassLoader().getResourceAsStream(imageResource)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        List<Point> topCodePoints = new ArrayList<>();
        TopCode startTopCode = null;

        for (TopCode topCode : scan) {
            if (getPieceForTopCode(topCode.getCode()) == START) {
                if (orientation == null) orientation = topCode.getOrientation();
                templateHeight = (int) topCode.getDiameter();
                startTopCode = topCode;
            }

            Point e = new Point(topCode.getCenterX(), topCode.getCenterY());
            topCodePoints.add(e);
        }

        if (startTopCode == null) {
            throw new NoStartPieceError();
        }

        Mat srcMat;
        try {
            srcMat = OpenCVUtil.readInputStreamIntoMat(getClass().getClassLoader().getResourceAsStream(imageResource));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // Multiply with 57.2957795 to transform radians into degrees
        List<Point> points = rotatePoints(srcMat, orientation * 57.2957795d, topCodePoints);
        List<MatchPair> res = new ArrayList<>();
        for (int i = 0; i < scan.size(); i++) {
            res.add(new MatchPair(getPieceForTopCode(scan.get(i).getCode()), points.get(i)));
        }

        return res;

    }

    private List<Point> rotatePoints(Mat src, double angle, List<Point> topPoints) {
        Mat rotmat = Imgproc.getRotationMatrix2D(new Point(src.cols() / 2, src.rows() / 2), angle, 1.0d);
        List<Point> result = new ArrayList<>();

        for (Point topPoint : topPoints) {
            Point resPoint = new Point();
            resPoint.x = rotmat.get(0, 0)[0] * topPoint.x +  rotmat.get(0, 1)[0] * topPoint.y + rotmat.get(0,2)[0];
            resPoint.y = rotmat.get(1, 0)[0] * topPoint.x +  rotmat.get(1, 1)[0] * topPoint.y + rotmat.get(1,2)[0];
            result.add(resPoint);
        }

        return result;
    }

    public PuzzlePiece getPieceForTopCode(int topCode) {
        for (PuzzlePiece puzzlePiece : PuzzlePiece.values()) {
            if (puzzlePiece.getTopCode() == topCode){
                return puzzlePiece;
            }
        }
        return null;
    }

    public List<MatchPair> getTemplateLocations() throws NoStartPieceError, IOException {
        inputImage = OpenCVUtil.readInputStreamIntoMat(getClass().getClassLoader().getResourceAsStream(imageResource));
        if (symbolStyle == TOPCODES) {
            return getLocationsForTopcodes();
        } else {
            Map<PuzzlePiece, Mat> allImagesFromFolder = getAllImagesFromFolder(templateFolder);
//            Mat templateForScaling = allImagesFromFolder.get(START);
//            double optimalScaleFactor = getTemplateDimension(inputImage, templateForScaling);
            double optimalScaleFactor = 0.2d;
            Map<PuzzlePiece, Mat> modifiedTemplates = getModifiedTemplates(allImagesFromFolder, optimalScaleFactor);

            Imgproc.cvtColor(inputImage, inputImage, Imgproc.COLOR_BGR2GRAY);

            Imgproc.threshold(inputImage, inputImage, 100, 255, Imgproc.THRESH_TOZERO);
            List<Point> startPoints = matchTemplate(inputImage, modifiedTemplates.get(START));
            if (startPoints.isEmpty()) throw new NoStartPieceError();
            Point startPoint = startPoints.get(0);

            this.templateHeight = modifiedTemplates.get(START).height();

            Rect startRect = createHorisontalCropRect(startPoint);
            Mat croppedImg = new Mat(inputImage, startRect);

            modifiedTemplates.remove(START);
            List<MatchPair> matches = new ArrayList<>();
            matches.add(new MatchPair(START, startPoint));
            matches.addAll(getLocationsForCustom(croppedImg, modifiedTemplates, match_method, startRect.tl()));
            return matches;
        }
    }

    private static int counter = 1;

    private Rect createVerticalCropRect(Point startingPoint) {
        int x = (int) (startingPoint.x - templateHeight);
        if (x < 0) x = 0;
        if (x >= inputImage.width()) throw new RuntimeException("Couldn't create cropRect");

        int width = templateHeight * 3;
        if (width > inputImage.width() || x + width > inputImage.width()) throw new RuntimeException("Couldn't create cropRect");

        int y = 0;

        int height = inputImage.height();

        return new Rect(x, y, width, height);
    }

    private Rect createHorisontalCropRect(Point startingPoint) {
        int x = (int) (startingPoint.x + templateHeight);
        if (x < 0) x = 0;
        if (x >= inputImage.width()) throw new RuntimeException("Couldn't create cropRect");

        int width = inputImage.width() - x;
        if (width > inputImage.width() || x + width > inputImage.width() || width <= 0) throw new RuntimeException("Couldn't create cropRect");

        int y = (int) (startingPoint.y - templateHeight);
        if (y < 0) y = 0;
        if (y > inputImage.height()) throw new RuntimeException("Couldn't create cropRect");

        int height = templateHeight * 3;
        if (height > inputImage.height() || y + height > inputImage.height()) throw new RuntimeException("Couldn't create cropRect");

        return new Rect(x, y, width, height);
    }

    private Rect createRectForMove(Point startingPoint) {
        int x = (int) (startingPoint.x - templateHeight);
        if (x < 0) x = 0;
        if (x >= inputImage.width()) throw new RuntimeException("Couldn't create cropRect");

        int width = templateHeight * 3;
        if (width > inputImage.width() || x + width > inputImage.width()) throw new RuntimeException("Couldn't create cropRect");

        int y = (int) (startingPoint.y + templateHeight);

        int height = 5 * templateHeight;

        return new Rect(x, y, width, height);
    }

    private List<Point> getTransitionPointsForIfStatement(Point ifLocation, List<Point> transitionPoints) {
        Point transitionAboveIf = null;
        for (Point transitionPoint : transitionPoints) {
            if (transitionPoint.y > ifLocation.y) continue;
            if (transitionAboveIf == null || transitionPoint.y > transitionAboveIf.y) {
                transitionAboveIf = transitionPoint;
            }
        }

        Point transitionBelowIf = null;
        for (Point transitionPoint : transitionPoints) {
            if (transitionPoint.y < ifLocation.y) continue;
            if (transitionBelowIf == null || transitionPoint.y < transitionBelowIf.y) {
                transitionBelowIf = transitionPoint;
            }
        }
        return Arrays.asList(transitionAboveIf, transitionBelowIf);
    }

    private Point getAbsolutePointCoordinates(Point cropPosition, Point matchPosition) {
        return new Point(cropPosition.x + matchPosition.x, cropPosition.y + matchPosition.y);
    }

    private List<MatchPair> getLocationsForCustom(Mat croppedImg, Map<PuzzlePiece, Mat> modifiedTemplates, int match_method, Point cropPosition) {
        List<MatchPair> matches = new ArrayList<>();
        for (Map.Entry<PuzzlePiece, Mat> matEntry : modifiedTemplates.entrySet()) {
            List<Point> templateResult = matchTemplate(croppedImg, matEntry.getValue());
            templateResult.forEach(x -> matches.add(new MatchPair(matEntry.getKey(), getAbsolutePointCoordinates(cropPosition, x))));
        }

        MatchPair ifStatementMatch = null;
        List<MatchPair> moveMatches = new ArrayList<>();
        for (MatchPair matchPair : matches) {
            if (matchPair.getPuzzlePiece() == IF) {
                ifStatementMatch = matchPair;
            } else if (matchPair.getPuzzlePiece() == MOVE) {
                moveMatches.add(matchPair);
            }
        }


        while (!moveMatches.isEmpty()) {
            MatchPair moveMatch = moveMatches.remove(0);
            Rect moveRect = createRectForMove(moveMatch.getLocation());
            Mat moveCrop = new Mat(inputImage, moveRect);

            Point moveCropLocation = moveRect.tl();

            MatchPair argumentMatch = null;
            for (Map.Entry<PuzzlePiece, Mat> matEntry : argumentModifiedTemplates.entrySet()) {
                List<Point> argumentMatchPoints = matchTemplate(moveCrop, matEntry.getValue());
                if (argumentMatchPoints.size() == 1) {
                    argumentMatch = new MatchPair(matEntry.getKey(), getAbsolutePointCoordinates(moveCropLocation, argumentMatchPoints.get(0)));
                    break;
                }
            }
            if (argumentMatch != null) matches.add(argumentMatch);
        }


        if (ifStatementMatch != null) {
            Rect verticalRect = createVerticalCropRect(ifStatementMatch.getLocation());
            Mat verticalCrop = new Mat(inputImage, verticalRect);

            Point verticalCropLocation = verticalRect.tl();

            List<Point> transitionPoints = matchTemplate(verticalCrop, modifiedTemplates.get(TRANSITION));
            if (transitionPoints.size() != 0 && transitionPoints.size() % 2 != 0) throw new RuntimeException("Couldn't find even amount of transition pieces.");

            List<Point> transitions = getTransitionPointsForIfStatement(ifStatementMatch.getLocation(), transitionPoints);

            Point transitionAbsolutePointCoordinates = getAbsolutePointCoordinates(verticalCropLocation, transitions.get(0));
            Rect horisontalCropRect1 = createHorisontalCropRect(transitionAbsolutePointCoordinates);
            matches.add(new MatchPair(TRANSITION, transitionAbsolutePointCoordinates));
            Point transitionAbsolutePointCoordinates2 = getAbsolutePointCoordinates(verticalCropLocation, transitions.get(1));
            Rect horisontalCropRect2 = createHorisontalCropRect(transitionAbsolutePointCoordinates2);
            matches.add(new MatchPair(TRANSITION, transitionAbsolutePointCoordinates2));

            Mat horisontalCrop1 = new Mat(inputImage, horisontalCropRect1);
            Mat horisontalCrop2 = new Mat(inputImage, horisontalCropRect2);

            matches.addAll(getLocationsForCustom(horisontalCrop1, modifiedTemplates, match_method, horisontalCropRect1.tl()));
            matches.addAll(getLocationsForCustom(horisontalCrop2, modifiedTemplates, match_method, horisontalCropRect2.tl()));
        }
        return matches;
    }

    private Map<PuzzlePiece, Mat> getModifiedTemplates(Map<PuzzlePiece, Mat> templatesBefore, double optimalScaleFactor) {
        Map<PuzzlePiece, Mat> result = new HashMap<>();
        argumentModifiedTemplates = new HashMap<>();
        for (Map.Entry<PuzzlePiece, Mat> matEntry : templatesBefore.entrySet()) {

            Mat template = matEntry.getValue().clone();
            Imgproc.resize(template, template, new Size(), optimalScaleFactor, optimalScaleFactor, Imgproc.INTER_AREA);
            Imgproc.cvtColor(template, template, Imgproc.COLOR_BGR2GRAY);

            if (matEntry.getKey() == NUMBER3 || matEntry.getKey() == NUMBER4) {
                argumentModifiedTemplates.put(matEntry.getKey(), template);
            } else {
                result.put(matEntry.getKey(), template);
            }
        }
        return result;
    }

    private List<Point> matchTemplate(Mat img, Mat template) {
        List<Point> resultList = new ArrayList<>();

        int result_cols = img.cols() - template.cols() + 1;
        int result_rows = img.rows() - template.rows() + 1;

        double maxValue = Double.NEGATIVE_INFINITY;
        while (true) {
            Mat result = new Mat(result_rows, result_cols, CvType.CV_32FC1);

            Imgproc.matchTemplate(img, template, result, match_method);

            if (maxValue == Double.NEGATIVE_INFINITY) {
                Core.MinMaxLocResult extremum = Core.minMaxLoc(result);
                maxValue = extremum.maxVal;
            }

            Core.MinMaxLocResult mmr = Core.minMaxLoc(result);

            Point matchLoc;
            if (match_method == Imgproc.TM_SQDIFF || match_method == Imgproc.TM_SQDIFF_NORMED) {
                matchLoc = mmr.minLoc;
            } else {
                matchLoc = mmr.maxLoc;
            }
            if (mmr.maxVal < 0.91)
                break;

            resultList.add(matchLoc);

            Imgproc.rectangle(img, matchLoc,
                    new Point(matchLoc.x + template.cols(), matchLoc.y + template.rows()),
                    new Scalar(0, 255, 0), -1);
        }
        return resultList;
    }

    private Mat rotate (Mat src, double angle) {
        Mat result = new Mat();
        Point center = new Point(src.cols() / 2, src.rows() / 2);
        Mat rotationMat = Imgproc.getRotationMatrix2D(center, angle, 1.0);
        Rect bbox = new RotatedRect(center, src.size(), angle).boundingRect();
        rotationMat.put(0, 2, rotationMat.get(0, 2)[0] + bbox.width / 2.0 - center.x);
        rotationMat.put(1, 2, rotationMat.get(1, 2)[0] + bbox.height / 2.0 - center.y);
        Imgproc.warpAffine(src, result, rotationMat, bbox.size(), Imgproc.INTER_NEAREST);
        return result;
    }

    private double getTemplateAngle(Mat image, Mat template) {
        Mat templateCopy = template.clone();
        Mat imageCopy = image.clone();
        Mat rotatedImage = image.clone();

        Imgproc.cvtColor(templateCopy, templateCopy, Imgproc.COLOR_BGR2GRAY);

        double maximumMatchValue = Double.NEGATIVE_INFINITY;
        double bestAngle = 0;
        int i = 0;
        while (i < 36) {
            Imgproc.cvtColor(rotatedImage, rotatedImage, Imgproc.COLOR_BGR2GRAY);

            int result_cols = rotatedImage.cols() - templateCopy.cols() + 1;
            int result_rows = rotatedImage.rows() - templateCopy.rows() + 1;

            Mat result = new Mat(result_rows, result_cols, CvType.CV_32FC1);
            Imgproc.matchTemplate(rotatedImage, templateCopy, result, match_method);

            Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
            if (mmr.maxVal > maximumMatchValue) {
                maximumMatchValue = mmr.maxVal;
                bestAngle = i * 10;
            }
            i++;
            rotatedImage = rotate(imageCopy, i * 10);
        }

        return bestAngle;
    }


    private double getTemplateDimension(Mat image, Mat template) {
        Mat templateCopy = template.clone();
        Mat imageCopy = image.clone();

        Imgproc.cvtColor(templateCopy, templateCopy, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(imageCopy, imageCopy, Imgproc.COLOR_BGR2GRAY);

        double maximumMatchValue = Double.NEGATIVE_INFINITY;
        double bestScale = 0;
        int i = 40;
        while (--i > 0) {

            int result_cols = imageCopy.cols() - templateCopy.cols() + 1;
            int result_rows = imageCopy.rows() - templateCopy.rows() + 1;

            Mat result = new Mat(result_rows, result_cols, CvType.CV_32FC1);
            Imgproc.matchTemplate(imageCopy, templateCopy, result, match_method);

            Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
            if (mmr.maxVal > maximumMatchValue) {
                maximumMatchValue = mmr.maxVal;
                bestScale = (i + 1) * 0.05d;
            }

            Imgproc.resize(template, templateCopy, new Size(), i  * 0.05d, i* 0.05d, Imgproc.INTER_AREA);
            Imgproc.cvtColor(templateCopy, templateCopy, Imgproc.COLOR_BGR2GRAY);

        }
        if (bestScale == 0) throw new RuntimeException("Couldn't find a suitable template size.");

        return bestScale;

    }

    private Map<PuzzlePiece, Mat> getAllImagesFromFolder(String symbolFolder) throws IOException {
        Map<PuzzlePiece, Mat> result = new HashMap<>();
        ClassLoader currentClassLoader = getClass().getClassLoader();

        for (PuzzlePiece puzzlePiece : PuzzlePiece.values()) {
            // Ugly hack, but resource path separator is forward slash
            // Paths.get resolving on Windows will produce wrong resource path
            String imageResourcePath = symbolFolder + "/" + puzzlePiece.getFileName();
            InputStream resourceAsStream = currentClassLoader.getResourceAsStream(imageResourcePath);
            result.put(puzzlePiece, OpenCVUtil.readInputStreamIntoMat(resourceAsStream));
        }

        return result;
    }
}