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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
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
    @FXML
    public Button nextStepButton;
    @FXML
    public Button startManualButton;

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

        parentGrid.setStyle("-fx-background-color: #ffffff;");

        if (symbolStyle == SymbolStyle.TOPCODES) {
            worldImageFolder = "topcodesPrograms/";
        } else {
            worldImageFolder = "customPrograms/";
        }

        currentLevel = level;
        hideLevelButtons();

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
                new KeyFrame(Duration.seconds(0.7d))
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
            handleProgramEnd(null);
            return;
        }

        worldImage.setImage(currentNode.getImageWithADot());
        if (currentNode instanceof Move) {
            try {
                handleMove((Move) currentNode);
                takenSteps++;
            } catch (CannotWalkIntoTrap cannotWalkIntoTrap) {
                handleProgramEnd(new CannotWalkIntoTrap());

            } catch (CannotWalkIntoWall cannotWalkIntoWall) {
                handleProgramEnd(new CannotWalkIntoWall());
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
                handleProgramEnd(new NoLandForJump());
            } else {
                currentNode = ((Jump) currentNode).getLand();
            }
        } else if (currentNode instanceof Land) {
            currentNode = ((Land) currentNode).getChild();
        } else if (currentNode instanceof Start) {
            currentNode = ((Start) currentNode).getChild();
        } else if (currentNode instanceof Stop) {
            handleProgramEnd(null);
        } else {
            throw new RuntimeException("Unknown ee.ut.program step.");
        }
    }

    private void handleProgramEnd(Exception exception) {
        if (exception == null) {
            if (table.get(rabbitY).get(rabbitX) == 'c') {
                // tase läbitud
                takenSteps = 0;
                enableDefaultButtons();
                resultLabel.setText("Tase edukalt läbitud!");
                if (currentTimeline == null) { // manual
                    startManualButton.setDisable(true);
                    nextStepButton.setDisable(true);
                } else { // automatic
                    currentTimeline.stop();
                    startButton.setDisable(true);
                    stopButton.setDisable(true);
                }
            } else {
                // jäi poolele maale
                enableDefaultButtons();
                takenSteps = 0;
                resultLabel.setText("Jänes ei jõudnud porgandini. Jätka soovi korral taseme läbimist praegusest asukohast või alusta uuesti.");
                if (currentTimeline == null) { // manual
                    startManualButton.setDisable(false);
                    nextStepButton.setVisible(false);
                    startButton.setVisible(true);
                    startButton.setDisable(false);
                } else { // automatic
                    currentTimeline.stop();
                    startButton.setDisable(false);
                    stopButton.setVisible(false);
                    startManualButton.setVisible(true);
                    startManualButton.setDisable(false);
                }
            }
        } else {
            if (exception instanceof ProgramInterruption) {
                // vajutati stopp nuppu
                takenSteps = 0;
                enableDefaultButtons();
                resultLabel.setText("Programm on peatatud. Alustage soovi korral taset uuesti või liikuge järgmise taseme juurde.");

                currentTimeline.stop();
                startButton.setDisable(true);
                stopButton.setDisable(true);
            } else if (exception instanceof CannotWalkIntoTrap) {
                takenSteps = 0;
                enableDefaultButtons();
                resultLabel.setText("Kahjuks sattus jänes lõksu. Alustage soovi korral taset uuesti või liikuge järgmise taseme juurde.");

                if (currentTimeline == null) {
                    nextStepButton.setDisable(true);
                    startManualButton.setDisable(true);
                } else {
                    currentTimeline.stop();
                    stopButton.setDisable(true);
                    startButton.setDisable(true);
                }
            } else if (exception instanceof CannotWalkIntoWall) {
                takenSteps = 0;
                enableDefaultButtons();
                resultLabel.setText("Kahjuks liikus jänes vastu seina. Alustage soovi korral taset uuesti või liikuge järgmise taseme juurde.");

                if (currentTimeline == null) {
                    nextStepButton.setDisable(true);
                    startManualButton.setDisable(true);
                } else {
                    currentTimeline.stop();
                    stopButton.setDisable(true);
                    startButton.setDisable(true);
                }
            } else if (exception instanceof NoLandForJump) {
                takenSteps = 0;
                resultLabel.setText("Hüppeklotsil puudub vastav maandumisklots. Alustage soovi korral taset uuesti või liikuge järgmise taseme juurde.");
                enableDefaultButtons();

                if (currentTimeline == null) {
                    nextStepButton.setDisable(true);
                    startManualButton.setDisable(true);
                } else {
                    currentTimeline.stop();
                    stopButton.setDisable(true);
                    startButton.setDisable(true);
                }
            } else if (exception instanceof NoConditionForIfStatement) {
                takenSteps = 0;
                resultLabel.setText("Hargnemistükil puudub eelnev tingimus. Alustage soovi korral taset uuesti või liikuge järgmise taseme juurde.");
                enableDefaultButtons();

                if (currentTimeline == null) {
                    nextStepButton.setDisable(true);
                    startManualButton.setDisable(true);
                } else {
                    currentTimeline.stop();
                    stopButton.setDisable(true);
                    startButton.setDisable(true);
                }
            }
        }
        currentTimeline = null;
    }

    private void handleIfStatement(IfStatement ifStatement) {
        if (ifStatement.getCondition() == null) {
            handleProgramEnd(new NoConditionForIfStatement());
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
            case ISCARROT:
                if (table.get(nextPos[0]).get(nextPos[1]) == 'c') {
                    currentNode = ifStatement.getTrueNode();
                } else {
                    currentNode = ifStatement.getFalseNode();
                }
                break;
            default:
                throw new RuntimeException("Unknown condition for IfStatement");
        }
        if (currentNode == null) {
            handleProgramEnd(null);
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

    private void disableDefaultButtons() {
        backToMainButton.setDisable(true);
        startAgain.setDisable(true);

        nextImageButton.setDisable(true);
        previousImageButton.setDisable(true);

        nextLevel.setDisable(true);
        lastLevel.setDisable(true);
    }

    private void enableDefaultButtons() {
        backToMainButton.setDisable(false);
        startAgain.setDisable(false);

        hideImageButtons();
        hideLevelButtons();
    }

    public void handleStartAgainButtonClick(ActionEvent event) throws IOException {
        takenSteps = 0;
        currentTimeline = null;

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("fxml/world.fxml"));
        Pane root = fxmlLoader.load();

        WorldController worldController = fxmlLoader.getController();

        worldController.setCurrentImageNr(currentImageNr);
        worldController.setPrimaryStage(primaryStage);
        worldController.setSymbolStyle(symbolStyle);

        worldController.initialize(currentLevel, primaryStage, root);
    }

    public void handleStartButtonClick(ActionEvent event) {
        String imageResource = worldImageFolder + currentImageNr + ".jpg";
        resultLabel.setText("Koostan programmi. Palun oodake.");
        ProgramGenerator programGenerator = new ProgramGenerator(imageResource, symbolStyle);

        startManualButton.setVisible(false);
        stopButton.setVisible(true);

        final String[] error = {""};
        Task<TreeNode> task = new Task<TreeNode>() {
            @Override
            public TreeNode call() {
                try {
                    disableDefaultButtons();
                    startButton.setDisable(true);
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
                    enableDefaultButtons();
                    startButton.setDisable(false);

                    stopButton.setVisible(false);
                    startManualButton.setVisible(true);
                    startManualButton.setDisable(false);
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
        // Initial main window sizes
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
            lastLevel.setDisable(false);
        else
            lastLevel.setDisable(true);

        String nextLevelNr = Integer.toString(Integer.parseInt(currentLevel) + 1);
        u = this.getClass().getClassLoader().getResource("worlds/" + nextLevelNr+ ".txt");
        if (u != null)
            nextLevel.setDisable(false);
        else
            nextLevel.setDisable(true);

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
            previousImageButton.setDisable(false);
        } catch (IllegalArgumentException e) {
            previousImageButton.setDisable(true);
        }

        try {
            String nextImageNr = Integer.toString(Integer.parseInt(currentImageNr) + 1);
            Image testImage = new Image(String.valueOf(getClass().getClassLoader().getResource(worldImageFolder + nextImageNr + ".jpg")));
            nextImageButton.setDisable(false);
        } catch (IllegalArgumentException e) {
            nextImageButton.setDisable(true);
        }
    }

    public void handleNextImageButtonClick(ActionEvent event) {
        currentImageNr = Integer.toString(Integer.parseInt(currentImageNr) + 1);
        Image image = new Image(String.valueOf(getClass().getClassLoader().getResource(worldImageFolder + currentImageNr + ".jpg")));
        worldImage.setImage(image);

        hideImageButtons();
    }

    public void handleStopButtonClick(ActionEvent event) throws IOException {
        handleProgramEnd(new ProgramInterruption());
    }

    public void handleNextStepButtonClick(ActionEvent event) {
        executeProgram();
    }

    public void handleStartManualButtonClick(ActionEvent event) {
        String imageResource = worldImageFolder + currentImageNr + ".jpg";
        resultLabel.setText("Koostan programmi. Palun oota.");
        ProgramGenerator programGenerator = new ProgramGenerator(imageResource, symbolStyle);

        startButton.setVisible(false);
        nextStepButton.setVisible(true);

        final String[] error = {""};
        Task<TreeNode> task = new Task<TreeNode>() {
            @Override
            public TreeNode call() {
                try {
                    disableDefaultButtons();
                    startManualButton.setDisable(true);
                    nextStepButton.setDisable(true);
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
                    enableDefaultButtons();
                    startManualButton.setDisable(false);

                    nextStepButton.setVisible(false);
                    startButton.setVisible(true);
                    startButton.setDisable(false);
                }
            } else {
                nextStepButton.setDisable(false);
                resultLabel.setText("Programm on valmis. Saate seda samm-sammult täitma hakata.");
                startAgain.setDisable(false);
            }
        });

        new Thread(task).start();
    }
}
