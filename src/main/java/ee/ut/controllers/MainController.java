package ee.ut.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.*;

import static ee.ut.imageProcessing.SymbolStyle.CUSTOM;
import static ee.ut.imageProcessing.SymbolStyle.TOPCODES;

public class MainController {

    @FXML
    private RadioButton radioButtonTopCodes;
    @FXML
    private RadioButton radioButtonCustom;
    @FXML
    private ChoiceBox<String> choiceBox;

    private Stage primaryStage;

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void initWorld() throws IOException {
        String value = choiceBox.getValue();

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("fxml/world.fxml"));
        Pane root = fxmlLoader.load();
        WorldController worldController = fxmlLoader.getController();
        worldController.setPrimaryStage(primaryStage);
        if (radioButtonCustom.isSelected()) {
            worldController.setSymbolStyle(CUSTOM);
        } else {
            worldController.setSymbolStyle(TOPCODES);
        }

        worldController.initialize(value, primaryStage, root);

    }

    public void fillChoiceBox() throws IOException{
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("levels.txt");
        try(BufferedReader br = new BufferedReader(new InputStreamReader(resourceAsStream))) {
            String line = br.readLine();
            while (line != null) {
                choiceBox.getItems().add(line);
                line = br.readLine();
            }
        }
        choiceBox.setValue(choiceBox.getItems().get(0));
    }

    public void setupRadioButtons() {
        ToggleGroup group = new ToggleGroup();
        radioButtonCustom.setToggleGroup(group);
        radioButtonTopCodes.setToggleGroup(group);
        radioButtonCustom.setSelected(true);
    }
}
