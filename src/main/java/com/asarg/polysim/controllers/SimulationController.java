package com.asarg.polysim.controllers;

import com.asarg.polysim.Main;
import com.asarg.polysim.PolyTileCell;
import com.asarg.polysim.models.TwoHAM.TwoHAMAssembly;
import com.asarg.polysim.models.TwoHAM.TwoHAMSimulationNode;
import com.asarg.polysim.models.base.*;
import com.asarg.polysim.utils.Utilities;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by ericmartinez on 1/5/15.
 */
public class SimulationController implements Initializable {
    public static final DataFormat polyTileFormat = new DataFormat("PolyTile");
    private final SimpleBooleanProperty showHelp = new SimpleBooleanProperty(true);
    //FXML Components
    @FXML
    TabPane tabPane;
    @FXML
    StackPane inspector;
    @FXML
    AnchorPane assemblyInspectorPane;
    @FXML
    AnchorPane helpPane;
    @FXML
    BorderPane borderPane;
    @FXML
    Button btn_fb;
    @FXML
    Button btn_b;
    @FXML
    Button btn_play;
    @FXML
    Button btn_f;
    @FXML
    Button btn_ff;
    @FXML
    TextField field_temperature;
    @FXML
    ChoiceBox choice_weight;
    @FXML
    ToggleButton btn_settings;
    @FXML
    Button btn_help;
    @FXML
    MenuItem menu_save;
    @FXML
    MenuItem menu_save_as;
    @FXML
    MenuItem menu_import_tile_config;
    @FXML
    MenuItem menu_tile_editor;
    @FXML
    MenuItem menu_export_2ham_terminal_tileset;
    @FXML
    Label lbl_left_status;
    @FXML
    Label lbl_right_status;
    @FXML
    ListView<PolyTile> listview_polytiles;
    @FXML
    Hyperlink hyperlink_website;


    private Stage stage;
    private boolean inspecting = false;

    @Override // This method is called by the FXMLLoader when initialization is complete
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        btn_fb.setText(String.valueOf('\uf049'));
        btn_b.setText(String.valueOf('\uf048'));
        btn_play.setText(String.valueOf('\uf04b'));
        btn_f.setText(String.valueOf('\uf051'));
        btn_ff.setText(String.valueOf('\uf050'));
        btn_settings.setText(String.valueOf('\uf013'));
        btn_help.setText(String.valueOf('\uf059'));
        inspector.managedProperty().bind(inspector.visibleProperty());
        inspector.setVisible(false);
        helpPane.visibleProperty().bind(showHelp);
        btn_settings.setSelected(false);
        tabPane.setDisable(true);
        btn_settings.setDisable(true);
        choice_weight.setItems(FXCollections.observableArrayList("Uniform", "Concentration", "Count"));

        listview_polytiles.setCellFactory(list -> {
            final ListCell<PolyTile> ptCell = new PolyTileCell();
            ptCell.setOnDragDetected(event -> {
                Dragboard db = ptCell.startDragAndDrop(TransferMode.ANY);
                ClipboardContent content = new ClipboardContent();
                ImageView temp = (ImageView) ptCell.getGraphic();
                content.put(polyTileFormat, ptCell.getIndex());
                db.setContent(content);
                db.setDragView(temp.getImage());
                event.consume();
            });
            return ptCell;
        }
        );

        field_temperature.focusedProperty().addListener((arg0, oldPropertyValue, newPropertyValue) -> {
            if (!newPropertyValue) {
                try {
                    int temperature = (Integer.parseInt(field_temperature.getText()));
                    if (temperature > 0) {
                        currentSimulationNode().setTemperature(temperature);
                    }
                    return;
                } catch (Exception ignored) {

                }
                field_temperature.setText("" + currentSimulationNode().getTemperature());
            }
        });

        choice_weight.getSelectionModel().selectedIndexProperty().addListener(
                (observable, oldValue, newValue) -> {
                    currentSimulationNode().setWeightOption(newValue.intValue());
                    choice_weight.getSelectionModel().select(currentSimulationNode().getWeightOption());
                }
        );
        tabPane.getSelectionModel().selectedItemProperty().addListener(
                (ov, t, t1) -> {
                    if (t1 != null) {
                        showHelp.setValue(false);
                        tabPane.setDisable(false);

                        menu_import_tile_config.setDisable(false);
                        menu_save.setDisable(false);
                        menu_save_as.setDisable(false);
                        menu_tile_editor.setDisable(false);

                        btn_settings.setDisable(false);

                        SimulationNode current = (SimulationNode) t1.getContent();
                        if (current != null) {
                            lbl_left_status.textProperty().bind(current.left_status);
                            lbl_right_status.textProperty().bind(current.right_status);
                            field_temperature.setText("" + currentSimulationNode().getTemperature());
                            choice_weight.getSelectionModel().select(current.getWeightOption());
                            listview_polytiles.setItems(current.getTileSet());
                            listview_polytiles.getSelectionModel().selectedItemProperty().removeListener((observable, oldValue, newValue) -> {
                                if (newValue != null) {
                                    currentSimulationNode().clearAssemblyReseed(newValue);
                                }
                            });

                            if (current.getClass() == TwoHAMSimulationNode.class) {
                                menu_export_2ham_terminal_tileset.setDisable(false);

                                listview_polytiles.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                                    if (newValue != null) {
                                        currentSimulationNode().clearAssemblyReseed(newValue);
                                    }
                                });
                            }

                            if (current.getFile() == null) {
                                menu_save.setDisable(true);
                            } else menu_save.setDisable(false);
                        }
                    } else {
                        tabPane.setDisable(true);
                        showHelp.setValue(true);

                        btn_settings.setDisable(true);

                        menu_import_tile_config.setDisable(true);
                        menu_save.setDisable(true);
                        menu_save_as.setDisable(true);
                        menu_tile_editor.setDisable(true);
                        menu_export_2ham_terminal_tileset.setDisable(true);

                    }
                }
        );


        borderPane.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
            final KeyCombination enter = new KeyCodeCombination(KeyCode.ENTER);
            final KeyCombination pgup = new KeyCodeCombination(KeyCode.PAGE_UP);
            final KeyCombination pgdown = new KeyCodeCombination(KeyCode.PAGE_DOWN);
            final KeyCombination left = new KeyCodeCombination(KeyCode.LEFT);
            final KeyCombination right = new KeyCodeCombination(KeyCode.RIGHT);
            final KeyCombination escape = new KeyCodeCombination(KeyCode.ESCAPE);
            final KeyCombination delete = new KeyCodeCombination(KeyCode.DELETE);
            final KeyCombination backspace = new KeyCodeCombination(KeyCode.BACK_SPACE);

            @Override
            public void handle(KeyEvent t) {
                SimulationNode cn = currentSimulationNode(); //current simulationnode in tab
                if (cn != null) {
                    if (enter.match(t)) {
                        if (cn.currentFrontierAttachment != null) {
                            FrontierElement fe = new FrontierElement(cn.currentFrontierAttachment.getLocation(),
                                    cn.currentFrontierAttachment.getOffset(), cn.currentFrontierAttachment.getPolyTile(), 4);
                            cn.resetFrontier();
                            cn.assembly.attach(fe);
                            cn.frontier = cn.assembly.calculateFrontier();
                            cn.placeFrontierOnGrid();
                            cn.exitFrontierMode();
                            t.consume();
                        }
                    } else if (pgup.match(t)) {
                        cn.zoomInDraw();
                        t.consume();
                    } else if (pgdown.match(t)) {
                        cn.zoomOutDraw();
                        t.consume();
                    } else if (left.match(t)) {
                        backward();
                        t.consume();
                    } else if (right.match(t)) {
                        forward();
                        t.consume();
                    } else if (escape.match(t)) {
                        cn.exitFrontierMode();
                        cn.reset();
                        cn.frontier = cn.assembly.calculateFrontier();
                        cn.placeFrontierOnGrid();
                        t.consume();
                    } else if (backspace.match(t) || delete.match(t)) {
                        System.out.println("Deleting polytile");
                        cn.deleteSelection();
                        t.consume();
                    }
                }
            }
        });

        hyperlink_website.setOnAction(t -> {
            try {
                Desktop.getDesktop().browse(new URI(hyperlink_website.getText()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private SimulationNode currentSimulationNode() {
        try {
            return (SimulationNode) tabPane.getSelectionModel().getSelectedItem().getContent();
        } catch (NullPointerException npe) {
            return null;
        }
    }

    @FXML
    public void forward() {
        SimulationNode current = currentSimulationNode();
        if (current != null) current.forward();
    }

    @FXML
    public void fast_forward() {
        SimulationNode current = currentSimulationNode();
        if (current != null) current.fast_forward();
    }

    @FXML
    public void backward() {
        SimulationNode current = currentSimulationNode();
        if (current != null) current.backward();
    }

    @FXML
    public void fast_backward() {
        SimulationNode current = currentSimulationNode();
        if (current != null) current.fast_backward();
    }

    @FXML
    public void clear_seed() {
        SimulationNode current = currentSimulationNode();
        if (current != null) current.clear_seed();
    }

    @FXML
    public void play() {
        final SimulationNode current = currentSimulationNode();
        if (current != null) {
            if (current.stopped) {
                btn_play.setText(String.valueOf('\uf04c'));
                Service<Void> service = new Service<Void>() {
                    @Override
                    protected Task<Void> createTask() {
                        return new Task<Void>() {
                            @Override
                            protected Void call() throws Exception {
                                current.play();
                                return null;
                            }
                        };
                    }

                    @Override
                    protected void succeeded() {
                        //Called when finished without exception
                        current.stopped = true;
                        btn_play.setText(String.valueOf('\uf04b'));
                    }
                };
                service.start(); // starts Thread
            } else {
                current.stopped = true;
                btn_play.setText(String.valueOf('\uf04b'));
            }

        }
    }

    @FXML
    public void toggle_settings() {
        inspecting = !inspecting;
        inspector.setVisible(inspecting);
        btn_settings.setSelected(inspecting);
    }


    @FXML
    public void newMenuItem() {
        Assembly asm = new Assembly();
        loadAssembly(asm);
    }

    @FXML
    public void new2HAMMenuItem() {
        TwoHAMAssembly asm = new TwoHAMAssembly();
        loadTwoHAMAssembly(asm);
        currentSimulationNode().removeFrontierFromGrid();
        currentSimulationNode().frontier.clear();
    }

    @FXML
    public void saveAsMenuItem() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save As...");
        fileChooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("XML", "*.xml"));

        File selectedFile = fileChooser.showSaveDialog(stage);
        if (selectedFile != null) {
            if (currentSimulationNode().saveAs(selectedFile)) {
                tabPane.getSelectionModel().getSelectedItem().setText(selectedFile.getName());
                menu_save.setDisable(false);
            }
        }
    }

    //TODO: Add functionality here
    @FXML
    void exportTileConfigMenuItem() {

    }

    @FXML
    void export2HAMTerminalTileSetMenuItem() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save As...");
        fileChooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("XML", "*.xml"));

        File selectedFile = fileChooser.showSaveDialog(stage);
        if (selectedFile != null) {
            if (((TwoHAMSimulationNode) currentSimulationNode()).exportTerminalTileSet(selectedFile)) {
                tabPane.getSelectionModel().getSelectedItem().setText(selectedFile.getName());
                menu_save.setDisable(false);
            }
        }

    }

    @FXML
    public void saveMenuItem() {
        currentSimulationNode().save();
    }

    @FXML
    public void quitMenuItem() {
        Platform.exit();
    }

    @FXML
    public void importTileConfigMenuItem() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Tile Configuration...");
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {

            try {
                JAXBContext jaxbContext = JAXBContext.newInstance(TileConfiguration.class);
                Unmarshaller unmarshaller;
                unmarshaller = jaxbContext.createUnmarshaller();
                TileConfiguration tc = (TileConfiguration) unmarshaller.unmarshal(selectedFile);

                currentSimulationNode().updateTileConfiguration(tc);

            } catch (javax.xml.bind.JAXBException jaxbe) {
                javax.swing.JOptionPane.showMessageDialog(null, "Failed to tile configuration");
            }
        }
    }

    private void launchTileEditor() {
        launchTileEditor(-1);
    }

    private void launchTileEditor(int i) {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/tileeditorwindow.fxml"));

        try {
            Parent root1 = loader.load();
            TileEditorController editorController = loader.getController();
            editorController.setTileConfiguration(currentSimulationNode().assembly.getTileSystem().getTileConfiguration());
            if (i != -1) editorController.selectPolyTileIndex(i);
            editorController.setSimulationNode(currentSimulationNode());
            Stage stage = new Stage();
            stage.setTitle("Tile Editor");
            stage.setScene(new Scene(root1));
            stage.show();
        } catch (IOException ex) {
            Logger.getLogger(TileEditorController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @FXML
    public void tileEditorMenuItem() {
        launchTileEditor();
    }

    public void loadAssembly(Assembly assembly) {
        loadAssembly(assembly, null);
    }

    public void loadAssembly(Assembly assembly, File f) {
        Tab tab = new Tab();
        if (f != null) tab.setText(f.getName());
        else tab.setText("Untitled");
        final SimulationNode simulationNode = new SimulationNode(assembly, f);
        simulationNode.widthProperty().bind(tabPane.widthProperty());
        simulationNode.heightProperty().bind(tabPane.heightProperty());
        tab.setContent(simulationNode);
        tabPane.getTabs().add(tab);
    }

    public void loadTwoHAMAssembly(TwoHAMAssembly assembly) {
        loadTwoHAMAssembly(assembly, null);
    }

    @FXML
    public void loadTwoHAMAssembly(TwoHAMAssembly assembly, File f) {
        Tab tab = new Tab();
        if (f != null) tab.setText(f.getName());
        else tab.setText("Untitled");
        final TwoHAMSimulationNode simulationNode = new TwoHAMSimulationNode(assembly, f);
        simulationNode.widthProperty().bind(tabPane.widthProperty());
        simulationNode.heightProperty().bind(tabPane.heightProperty());
        tab.setContent(simulationNode);
        tabPane.getTabs().add(tab);
    }

    @FXML
    public void loadMenuItem() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Assembly...");
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            try {
                Assembly assembly = Utilities.LoadAssembly(selectedFile);
                loadAssembly(assembly, selectedFile);
            } catch (javax.xml.bind.JAXBException jaxbe) {
                javax.swing.JOptionPane.showMessageDialog(null, "Failed to load assembly");
            } catch (FileNotFoundException fnfe) {
                javax.swing.JOptionPane.showMessageDialog(null, "Failed to load assembly");
            }
        }
    }

    @FXML
    public void loadTwoHamMenuItem() {
//        FileChooser fileChooser = new FileChooser();
//        fileChooser.setTitle("Open 2HAM Assembly...");
//        File selectedFile = fileChooser.showOpenDialog(stage);
//        if (selectedFile != null) {
//            try {
//                Assembly assembly = Assembly.LoadAssembly(selectedFile);
//                loadAssembly(assembly, selectedFile);
//            } catch (javax.xml.bind.JAXBException jaxbe) {
//                javax.swing.JOptionPane.showMessageDialog(null, "Failed to load assembly");
//            } catch (FileNotFoundException fnfe){
//                javax.swing.JOptionPane.showMessageDialog(null, "Failed to load assembly");
//            }
//        }
    }

    private void deleteSelectedPolyTile() {
        int index = listview_polytiles.getSelectionModel().getSelectedIndex();
        if (index >= 0) listview_polytiles.getItems().remove(index);
    }


    @FXML
    public void editPolyTileMenuItem() {
        int i = listview_polytiles.getSelectionModel().getSelectedIndex();
        launchTileEditor(i);
    }

    @FXML
    public void deletePolyTileMenuItem() {
        deleteSelectedPolyTile();
    }
}
