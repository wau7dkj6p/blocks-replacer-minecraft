package com.blockreplace.gui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Simple custom world selector that allows the user to choose a folder containing level.dat.
 */
final class WorldSelectorDialog {
  private final Stage owner;

  WorldSelectorDialog(Stage owner) {
    this.owner = owner;
  }

  Path showAndWait() {
    Stage stage = new Stage();
    stage.initOwner(owner);
    stage.initModality(Modality.WINDOW_MODAL);
    stage.setTitle("Выбор мира (папка с level.dat)");

    BlockReplaceGuiApp.applyWindowIcons(stage);

    TextField currentPathField = new TextField();
    currentPathField.setEditable(false);

    ListView<Path> dirList = new ListView<>();
    dirList.setCellFactory(
        lv ->
            new ListCell<>() {
              @Override
              protected void updateItem(Path item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getFileName().toString());
              }
            });

    Button upButton = new Button("Вверх");
    Button okButton = new Button("OK");
    Button cancelButton = new Button("Отмена");

    HBox top = new HBox(8, new Label("Текущий путь:"), currentPathField, upButton);
    top.setPadding(new Insets(8));

    HBox bottom = new HBox(8, okButton, cancelButton);
    bottom.setPadding(new Insets(8));

    BorderPane root = new BorderPane();
    root.setTop(top);
    root.setCenter(dirList);
    root.setBottom(bottom);

    Scene scene = new Scene(root, 600, 400);
    stage.setScene(scene);

    Path[] currentDir = {detectDefaultSavesDir()};
    if (currentDir[0] == null) {
      currentDir[0] = Paths.get(System.getProperty("user.home", "."));
    }

    Runnable refresh =
        () -> {
          Path dir = currentDir[0];
          currentPathField.setText(dir.toString());
          try (Stream<Path> stream = Files.list(dir)) {
            List<Path> children =
                stream
                    .filter(Files::isDirectory)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()))
                    .collect(Collectors.toList());
            dirList.setItems(FXCollections.observableArrayList(children));
          } catch (Exception e) {
            dirList.setItems(FXCollections.emptyObservableList());
          }
        };

    refresh.run();

    dirList.setOnMouseClicked(
        e -> {
          if (e.getClickCount() == 2) {
            Path sel = dirList.getSelectionModel().getSelectedItem();
            if (sel != null && Files.isDirectory(sel)) {
              currentDir[0] = sel;
              refresh.run();
            }
          }
        });

    upButton.setOnAction(
        e -> {
          Path parent = currentDir[0].getParent();
          if (parent != null) {
            currentDir[0] = parent;
            refresh.run();
          }
        });

    final Path[] result = {null};

    okButton.setOnAction(
        e -> {
          Path dir = currentDir[0];
          if (!Files.isDirectory(dir) || !Files.isRegularFile(dir.resolve("level.dat"))) {
            new Alert(
                    Alert.AlertType.ERROR,
                    "Выберите папку, в которой есть файл level.dat",
                    ButtonType.OK)
                .showAndWait();
            return;
          }
          result[0] = dir.resolve("level.dat");
          stage.close();
        });

    cancelButton.setOnAction(
        e -> {
          result[0] = null;
          stage.close();
        });

    stage.showAndWait();
    return result[0];
  }

  private static Path detectDefaultSavesDir() {
    String appData = System.getenv("APPDATA");
    if (appData != null && !appData.isBlank()) {
      Path p = Paths.get(appData, ".minecraft", "saves");
      if (Files.isDirectory(p)) {
        return p;
      }
    }
    return null;
  }
}

