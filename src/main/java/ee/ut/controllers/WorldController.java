package ee.ut.controllers;

import ee.ut.exceptions.*;
import ee.ut.imageProcessing.SymbolStyle;
import ee.ut.program.ProgramGenerator;
import ee.ut.program.TreeNode;
import ee.ut.program.elements.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.RowConstraints;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WorldController {

    private double MAZE_TILE_SIZE = 60d;

    /**
     * Parent grid (root container)
     */
    @FXML
    public GridPane parentGrid;

    /**
     * Grid pane for bunny-maze-thing
     */
    @FXML
    private GridPane gridPane;

    @FXML
    public ImageView worldImage;
    @FXML
    public Button startButton;
    @FXML
    public Button lastLevel;
    @FXML
    public Button backToMainButton;
    @FXML
    public Button nextLevel;
    @FXML
    public Button startAgain;
    @FXML
    public Button previousImageButton;
    @FXML
    public Button nextImageButton;
    @FXML
    private Label resultLabel;
    @FXML
    public Button stopButton;

    private Timeline currentTimeline;
    private double halfScreenHeigth = Screen.getPrimary().getVisualBounds().getHeight() / 3.0d;
    private int rabbitY;
    private int rabbitX;
    private int carrotY;
    private int carrotX;
    private Direction rabbitDirection;
    private List<List<ImageView>> tableImageViews = new ArrayList<>();
    private List<List<Character>> table;
    private SymbolStyle symbolStyle;
    private TreeNode currentNode;
    private String currentImageNr;
    private Stage primaryStage;
    private String currentLevel;
    private String worldImageFolder;
    private int takenSteps = 0;

    public void setSymbolStyle(SymbolStyle symbolStyle) {
        this.symbolStyle = symbolStyle;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void setCurrentImageNr(String currentImageNr) {
        this.currentImageNr = currentImageNr;
    }

    public void initialize(String level, Stage primaryStage, Pane root) {
        if (symbolStyle == SymbolStyle.TOPCODES) {
            worldImageFolder = "topcodesPrograms/";
        } else {
            worldImageFolder = "customPrograms/";
        }

        currentLevel = level;
        hideLevelButtons();
        stopButton.setDisable(true);

        if (currentImageNr == null) currentImageNr = "1";
        Image image = new Image(String.valueOf(getClass().getClassLoader().getResource(worldImageFolder + currentImageNr + ".jpg")));

        worldImage.setFitHeight(halfScreenHeigth * 2);
        worldImage.setImage(image);

        hideImageButtons();

        try {
            fillWorld(level);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        primaryStage.setScene(new Scene(root));

        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        primaryStage.setHeight(visualBounds.getHeight());
        primaryStage.setWidth(visualBounds.getWidth());
        primaryStage.setResizable(false);
        primaryStage.setMaximized(true);
        primaryStage.show();

        primaryStage.getScene().getWindow().setX(0.0d);
        primaryStage.getScene().getWindow().setY(0.0d);
    }

    public void fillWorld(String level) throws IOException {

        table = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("worlds/" + level + ".txt")))) {
            String line = br.readLine();
            while (line != null) {
                List<Character> row = new ArrayList<>();
                for (char c : line.toCharArray()) {
                    row.add(c);
                }
                table.add(row);
                line = br.readLine();
            }
        }


        ColumnConstraints columnConstraints = new ColumnConstraints();
        columnConstraints.setPercentWidth(100 / (double) table.get(0).size());
        gridPane.getColumnConstraints().add(columnConstraints);

        RowConstraints rowConstraints = new RowConstraints();
        rowConstraints.setPercentHeight(100 / (double) table.size());
        gridPane.getRowConstraints().add(rowConstraints);

        MAZE_TILE_SIZE = halfScreenHeigth / table.size();
        gridPane.setMaxSize(table.get(0).size() * MAZE_TILE_SIZE, halfScreenHeigth);

        for (int row = 0; row < table.size(); row++) {
            List<ImageView> rowImages = new ArrayList<>();
            for (int column = 0; column < table.get(0).size(); column++) {
                char c = table.get(row).get(column);
                Image image;
                if (c == '#') {
                    image = new Image("images/brick.png");
                } else if (c == ' ') {
                    image = new Image("images/white.jpg");
                } else if (c == 'c') {
                    carrotY = row;
                    carrotX = column;
                    image = new Image("images/carrot.png");
                } else if (c == '>' || c == '<' || c == 'v' || c == '^') {
                    rabbitY = row;
                    rabbitX = column;
                    image = getRabbitImage(c);
                } else if (c == 'x') {
                    image = new Image("images/trap.png");
                } else {
                    throw new RuntimeException("Unknown char in the world file.");
                }

                ImageView pic = new ImageView();
                pic.setFitWidth(MAZE_TILE_SIZE);
                pic.setFitHeight(MAZE_TILE_SIZE);
                pic.setImage(image);
                rowImages.add(pic);
                gridPane.add(pic, column, row);
            }
            tableImageViews.add(rowImages);
        }
    }

    private Image getRabbitImage(char rabbitDirectionSymbol) {
        Image image;
        switch (rabbitDirectionSymbol) {
            case '<':
                image = new Image("images/rabbitLeft.jpg");
                rabbitDirection = Direction.LEFT;
                break;
            case '>':
                image = new Image("images/rabbitRight.jpg");
                rabbitDirection = Direction.RIGHT;
                break;
            case 'v':
                image = new Image("images/rabbitFront.jpg");
                rabbitDirection = Direction.FRONT;
                break;
            case '^':
                image = new Image("images/rabbitBack.jpg");
                rabbitDirection = Direction.BACK;
                break;
            default:
                throw new RuntimeException("Unknown direction symbol.");
        }
        return image;
    }


    public File getTheNewestFile(String filePath, String ext) {
        File theNewestFile = null;
        File dir = new File(filePath);
        FileFilter fileFilter = new WildcardFileFilter("*." + ext);
        File[] files = dir.listFiles(fileFilter);

        if (files.length > 0) {
            Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
            theNewestFile = files[0];
        }

        return theNewestFile;
    }

    private void play() {
        if (currentTimeline != null) {
            currentTimeline.stop();
            takenSteps = 0;
            stopButton.setDisable(true);
        }

        this.currentTimeline = new Timeline(
                new KeyFrame(
                        Duration.ZERO,
                        actionEvent -> {
                            executeProgram();
                        }
                ),
                new KeyFrame(Duration.seconds(1.0d))
        );
        currentTimeline.setCycleCount(Integer.MAX_VALUE);
        currentTimeline.play();
    }

    private int[] getNextPos() {
        int nextY, nextX;
        switch (rabbitDirection) {
            case FRONT:
                nextY = rabbitY + 1;
                nextX = rabbitX;
                break;
            case LEFT:
                nextY = rabbitY;
                nextX = rabbitX - 1;
                break;
            case RIGHT:
                nextY = rabbitY;
                nextX = rabbitX + 1;
                break;
            case BACK:
                nextY = rabbitY - 1;
                nextX = rabbitX;
                break;
            default:
                throw new RuntimeException("Unknown rabbit direction.");
        }
        return new int[]{nextY, nextX};
    }

    private void executeProgram() {
        if (currentNode == null) {
            currentTimeline.stop();
            takenSteps = 0;
            stopButton.setDisable(true);
            handleProgramEnd();
            return;
        }

        worldImage.setImage(currentNode.getImageWithADot());
        if (currentNode instanceof Move) {
            try {
                handleMove((Move) currentNode);
                takenSteps++;
            } catch (CannotWalkIntoTrap cannotWalkIntoTrap) {
                currentTimeline.stop();
                takenSteps = 0;
                stopButton.setDisable(true);
                resultLabel.setText("Kahjuks sattus jänes lõksu.");
                enableButtons();
            } catch (CannotWalkIntoWall cannotWalkIntoWall) {
                currentTimeline.stop();
                takenSteps = 0;
                stopButton.setDisable(true);
                resultLabel.setText("Kahjuks tuli jänesel sein ette.");
                enableButtons();
            }

            if (((Move) currentNode).getSteps() == takenSteps) {
                // All steps have been taken
                takenSteps = 0;
                currentNode = ((Move) currentNode).getChild();
            }
        } else if (currentNode instanceof Turn) {
            handleTurn((Turn) currentNode);
            currentNode = ((Turn) currentNode).getChild();
        } else if (currentNode instanceof IfStatement) {
            handleIfStatement((IfStatement) currentNode);
        } else if (currentNode instanceof Jump) {
            if (((Jump) currentNode).getLand() == null) {
                currentTimeline.stop();
                takenSteps = 0;
                stopButton.setDisable(true);
                enableButtons();
                resultLabel.setText("Hüppeklotsil puudub vastav maandumisklots.");
            } else {
                currentNode = ((Jump) currentNode).getLand();
            }
        } else if (currentNode instanceof Land) {
            currentNode = ((Land) currentNode).getChild();
        } else if (currentNode instanceof Start) {
            currentNode = ((Start) currentNode).getChild();
        } else if (currentNode instanceof Stop) {
            currentTimeline.stop();
            takenSteps = 0;
            stopButton.setDisable(true);
            handleProgramEnd();
        } else {
            throw new RuntimeException("Unknown ee.ut.program step.");
        }
    }

    private void handleProgramEnd() {
        if (table.get(rabbitY).get(rabbitX) == 'c') {
            resultLabel.setText("Tase edukalt läbitud!");
        } else {
            resultLabel.setText("Jänes ei jõudnud porgandini. Jätka soovi korral taseme läbimist praegusest asukohast või alusta uuesti.");
        }
        enableButtons();
    }

    private void handleIfStatement(IfStatement ifStatement) {
        if (ifStatement.getCondition() == null) {
            currentTimeline.stop();
            takenSteps = 0;
            stopButton.setDisable(true);
            enableButtons();
            resultLabel.setText("Hargnemistükil puudub eelnev tingimus.");
            return;
        }

        int[] nextPos = getNextPos();
        switch (ifStatement.getCondition()) {
            case ISTRAP:
                if (table.get(nextPos[0]).get(nextPos[1]) == 'x') {
                    currentNode = ifStatement.getTrueNode();
                } else {
                    currentNode = ifStatement.getFalseNode();
                }
                break;
            case ISWALL:
                if (table.get(nextPos[0]).get(nextPos[1]) == '#') {
                    currentNode = ifStatement.getTrueNode();
                } else {
                    currentNode = ifStatement.getFalseNode();
                }
                break;
            default:
                throw new RuntimeException("Unknown condition for IfStatement");
        }
    }

    private void handleTurn(Turn expression) {
        rabbitDirection = expression.getNextDirection(rabbitDirection);

        gridPane.getChildren().remove(tableImageViews.get(rabbitY).get(rabbitX));

        Image newDirectionImage = getDirectionImage(rabbitDirection);
        ImageView imageViewNewDirection = new ImageView();
        imageViewNewDirection.setFitWidth(MAZE_TILE_SIZE);
        imageViewNewDirection.setFitHeight(MAZE_TILE_SIZE);
        imageViewNewDirection.setImage(newDirectionImage);

        gridPane.add(imageViewNewDirection, rabbitX, rabbitY);
        tableImageViews.get(rabbitY).set(rabbitX, imageViewNewDirection);
    }

    private void handleMove(Move move) throws CannotWalkIntoTrap, CannotWalkIntoWall {
        int[] nextPos = getNextPos();
        int nextY = nextPos[0];
        int nextX = nextPos[1];

        if (table.get(nextY).get(nextX) == ' ' || table.get(nextY).get(nextX) == 'c') {

            gridPane.getChildren().remove(tableImageViews.get(rabbitY).get(rabbitX));
            Image imageWhite = new Image("images/white.jpg");
            ImageView imageViewWhite = new ImageView();
            imageViewWhite.setFitWidth(MAZE_TILE_SIZE);
            imageViewWhite.setFitHeight(MAZE_TILE_SIZE);
            imageViewWhite.setImage(imageWhite);

            gridPane.add(imageViewWhite, rabbitX, rabbitY);
            tableImageViews.get(rabbitY).set(rabbitX, imageViewWhite);

            gridPane.getChildren().remove(tableImageViews.get(nextY).get(nextX));
            Image imageRabbit = getDirectionImage(rabbitDirection);
            ImageView imageViewRabbit = new ImageView();
            imageViewRabbit.setFitWidth(MAZE_TILE_SIZE);
            imageViewRabbit.setFitHeight(MAZE_TILE_SIZE);
            imageViewRabbit.setImage(imageRabbit);

            gridPane.add(imageViewRabbit, nextX, nextY);
            tableImageViews.get(nextY).set(nextX, imageViewRabbit);

            rabbitY = nextY;
            rabbitX = nextX;
        } else if (table.get(nextY).get(nextX) == '#') {
            throw new CannotWalkIntoWall();
        } else if (table.get(nextY).get(nextX) == 'x') {
            gridPane.getChildren().remove(tableImageViews.get(rabbitY).get(rabbitX));
            Image imageWhite = new Image("images/white.jpg");
            ImageView imageViewWhite = new ImageView();
            imageViewWhite.setFitWidth(MAZE_TILE_SIZE);
            imageViewWhite.setFitHeight(MAZE_TILE_SIZE);
            imageViewWhite.setImage(imageWhite);

            gridPane.add(imageViewWhite, rabbitX, rabbitY);
            tableImageViews.get(rabbitY).set(rabbitX, imageViewWhite);
            throw new CannotWalkIntoTrap();
        }

        if (table.get(rabbitY).get(rabbitX) != 'c') {
            gridPane.getChildren().remove(tableImageViews.get(carrotY).get(carrotX));
            Image carrot = new Image("images/carrot.png");
            ImageView imageViewCarrot = new ImageView();
            imageViewCarrot.setFitWidth(MAZE_TILE_SIZE);
            imageViewCarrot.setFitHeight(MAZE_TILE_SIZE);
            imageViewCarrot.setImage(carrot);

            gridPane.add(imageViewCarrot, carrotX, carrotY);
            tableImageViews.get(carrotY).set(carrotX, imageViewCarrot);
        }
    }

    private Image getDirectionImage(Direction direction) {
        switch (direction) {
            case BACK:
                return new Image("images/rabbitBack.jpg");
            case FRONT:
                return new Image("images/rabbitFront.jpg");
            case RIGHT:
                return new Image("images/rabbitRight.jpg");
            case LEFT:
                return new Image("images/rabbitLeft.jpg");
            default:
                return null;
        }
    }

    private void disableButtons() {
        startButton.setDisable(true);
        nextImageButton.setDisable(true);
        nextLevel.setDisable(true);
        previousImageButton.setDisable(true);
        lastLevel.setDisable(true);
        backToMainButton.setDisable(true);
        startAgain.setDisable(true);
    }

    private void enableButtons() {
        startButton.setDisable(false);
        nextImageButton.setDisable(false);
        nextLevel.setDisable(false);
        previousImageButton.setDisable(false);
        lastLevel.setDisable(false);
        backToMainButton.setDisable(false);
        startAgain.setDisable(false);
    }

    public void handleStartAgainButtonClick(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("fxml/world.fxml"));
        Pane root = fxmlLoader.load();

        WorldController worldController = fxmlLoader.getController();
        worldController.setCurrentImageNr(currentImageNr);
        worldController.setPrimaryStage(primaryStage);
        worldController.setSymbolStyle(symbolStyle);

        hideLevelButtons();
        worldController.initialize(currentLevel, primaryStage, root);
    }

    public void handleStartButtonClick(ActionEvent event) {
        String imageResource = worldImageFolder + currentImageNr + ".jpg";
        resultLabel.setText("Koostan programmi. Palun oota.");
        ProgramGenerator programGenerator = new ProgramGenerator(imageResource, symbolStyle);

        final String[] error = {""};
        Task<TreeNode> task = new Task<TreeNode>() {
            @Override
            public TreeNode call() {
                try {
                    disableButtons();
                    stopButton.setDisable(true);
                    return programGenerator.generateProgram() ;
                } catch (NoStartPieceError noStartPieceError) {
                    error[0] = "start";
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            currentNode = task.getValue();
            if (currentNode == null) {
                if (error[0].equals("start")) {
                    resultLabel.setText("Programmi loomine ebaõnnestus. Kontrolli, kas programm sisaldab alustamisklotsi.");
                }
            } else {
                stopButton.setDisable(false);
                resultLabel.setText("Programmi täidetakse hetkel.");
                play();
            }
        });

        new Thread(task).start();
    }

    public void handleBackToMainButtonClick(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("fxml/main.fxml"));
        Pane root = fxmlLoader.load();
        MainController mainController = fxmlLoader.getController();

        mainController.setPrimaryStage(primaryStage);

        primaryStage.setMaximized(false);
        // Initial main window sizes // TODO: 27.04.17 consider using some constants
        primaryStage.setWidth(600d);
        primaryStage.setHeight(400d);

        primaryStage.setScene(new Scene(root));
        primaryStage.show();

        mainController.fillChoiceBox();
        mainController.setupRadioButtons();
    }

    public void hideLevelButtons() {

        String previousLevelNr = Integer.toString(Integer.parseInt(currentLevel) - 1);
        URL u = this.getClass().getClassLoader().getResource("worlds/" + previousLevelNr + ".txt");
        if (u != null)
            lastLevel.setVisible(true);
        else
            lastLevel.setVisible(false);


        String nextLevelNr = Integer.toString(Integer.parseInt(currentLevel) + 1);
        u = this.getClass().getClassLoader().getResource("worlds/" + nextLevelNr+ ".txt");
        if (u != null)
            nextLevel.setVisible(true);
        else
            nextLevel.setVisible(false);

    }

    public void handleLastLevelButtonClick(ActionEvent event) throws IOException {
        handleLevelButtonClick(false);
    }

    public void handleNextLevelButtonClick(ActionEvent event) throws IOException {
        handleLevelButtonClick(true);
    }

    private void handleLevelButtonClick(boolean increment) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("fxml/world.fxml"));
        Pane root = fxmlLoader.load();

        WorldController worldController = fxmlLoader.getController();
        worldController.setCurrentImageNr(currentImageNr);
        worldController.setPrimaryStage(primaryStage);
        worldController.setSymbolStyle(symbolStyle);

        if (increment)
            currentLevel = Integer.toString(Integer.parseInt(currentLevel) + 1);
        else
            currentLevel = Integer.toString(Integer.parseInt(currentLevel) - 1);

        hideLevelButtons();
        worldController.initialize(currentLevel, primaryStage, root);
    }

    public void handlePreviousImageButtonClick(ActionEvent event) {
        currentImageNr = Integer.toString(Integer.parseInt(currentImageNr) - 1);
        Image image = new Image(String.valueOf(getClass().getClassLoader().getResource(worldImageFolder + currentImageNr + ".jpg")));
        worldImage.setImage(image);

        hideImageButtons();
    }

    private void hideImageButtons() {
        try {
            String previousImageNr = Integer.toString(Integer.parseInt(currentImageNr) - 1);
            Image testImage = new Image(String.valueOf(getClass().getClassLoader().getResource(worldImageFolder + previousImageNr + ".jpg")));
            previousImageButton.setVisible(true);
        } catch (IllegalArgumentException e) {
            previousImageButton.setVisible(false);
        }

        try {
            String nextImageNr = Integer.toString(Integer.parseInt(currentImageNr) + 1);
            Image testImage = new Image(String.valueOf(getClass().getClassLoader().getResource(worldImageFolder + nextImageNr + ".jpg")));
            nextImageButton.setVisible(true);
        } catch (IllegalArgumentException e) {
            nextImageButton.setVisible(false);
        }
    }

    public void handleNextImageButtonClick(ActionEvent event) {
        currentImageNr = Integer.toString(Integer.parseInt(currentImageNr) + 1);
        Image image = new Image(String.valueOf(getClass().getClassLoader().getResource(worldImageFolder + currentImageNr + ".jpg")));
        worldImage.setImage(image);

        hideImageButtons();
    }

    public void handleStopButtonClick(ActionEvent event) throws IOException {
        currentTimeline.stop();
        takenSteps = 0;

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("fxml/world.fxml"));
        Pane root = fxmlLoader.load();

        WorldController worldController = fxmlLoader.getController();
        worldController.setCurrentImageNr(currentImageNr);
        worldController.setPrimaryStage(primaryStage);
        worldController.setSymbolStyle(symbolStyle);
        worldController.initialize(currentLevel, primaryStage, root);
    }
}
