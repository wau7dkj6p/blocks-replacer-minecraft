package com.blockreplace.gui;

import com.blockreplace.core.blockdb.BlockDatabase;
import com.blockreplace.core.blockdb.BlockDef;
import com.blockreplace.core.blockdb.PropertyDef;
import com.blockreplace.core.task.BlockStateSpec;
import com.blockreplace.core.task.ReplaceTask;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Separate window for creating and editing a single ReplaceTask.
 *
 * <p>Supports two modes:
 *
 * <ul>
 *   <li>Vanilla – strict validation against bundled block database.
 *   <li>Mods – allow unknown blocks and arbitrary properties.
 * </ul>
 */
final class TaskEditorDialog {
  private final Stage owner;
  private final ReplaceTask initial;

  TaskEditorDialog(Stage owner, ReplaceTask initial) {
    this.owner = owner;
    this.initial = initial;
  }

  ReplaceTask showAndWait() {
    Stage stage = new Stage();
    stage.initOwner(owner);
    stage.initModality(Modality.WINDOW_MODAL);
    stage.setTitle(initial == null ? "Добавить задачу" : "Редактировать задачу");

    BlockReplaceGuiApp.applyWindowIcons(stage);

    ToggleGroup modeGroup = new ToggleGroup();
    RadioButton vanillaBtn = new RadioButton("Ванила");
    vanillaBtn.setToggleGroup(modeGroup);
    vanillaBtn.setSelected(true);
    RadioButton modsBtn = new RadioButton("Моды");
    modsBtn.setToggleGroup(modeGroup);

    CheckBox matchByNameOnlyCheck = new CheckBox("Совпадение только по имени блока (игнорировать состояния)");

    VBox modeRow = new VBox(6, new HBox(10, new Label("Режим:"), vanillaBtn, modsBtn), matchByNameOnlyCheck);

    ComboBox<String> fromBlockCombo = new ComboBox<>();
    fromBlockCombo.setEditable(true);
    fromBlockCombo.setPromptText("Исходный блок (minecraft:snow)");
    fromBlockCombo.setMaxWidth(Double.MAX_VALUE);

    ComboBox<String> toBlockCombo = new ComboBox<>();
    toBlockCombo.setEditable(true);
    toBlockCombo.setPromptText("Новый блок (minecraft:air)");
    toBlockCombo.setMaxWidth(Double.MAX_VALUE);

    TextField titleField = new TextField();
    titleField.setPromptText("Название задачи (необязательно)");

    VBox fromPropsBox = new VBox(4);
    VBox toPropsBox = new VBox(4);

    final List<String>[] allBlocks = new List[] {List.of()};

    try {
      BlockDatabase db = BlockDatabase.loadBundled();
      allBlocks[0] = new ArrayList<>(db.blockNames());
      FxAutoComplete.enableContainsAutoComplete(fromBlockCombo, allBlocks[0]);
      FxAutoComplete.enableContainsAutoComplete(toBlockCombo, allBlocks[0]);

      // Rebuild properties on both value and typed text changes.
      fromBlockCombo.valueProperty().addListener((obs, old, val) -> rebuildProperties(db, val, fromPropsBox));
      toBlockCombo.valueProperty().addListener((obs, old, val) -> rebuildProperties(db, val, toPropsBox));
      fromBlockCombo.getEditor().textProperty().addListener((obs, old, val) -> rebuildProperties(db, val, fromPropsBox));
      toBlockCombo.getEditor().textProperty().addListener((obs, old, val) -> rebuildProperties(db, val, toPropsBox));

      if (initial != null) {
        fromBlockCombo.setValue(initial.from().name());
        toBlockCombo.setValue(initial.to().name());
        titleField.setText(initial.title());
        matchByNameOnlyCheck.setSelected(initial.matchByNameOnly());
        rebuildPropertiesFromSpec(db, initial.from(), fromPropsBox);
        rebuildPropertiesFromSpec(db, initial.to(), toPropsBox);
      }
    } catch (IOException ex) {
      new Alert(Alert.AlertType.ERROR, "Ошибка загрузки базы блоков: " + ex.getMessage())
          .showAndWait();
    }

    ScrollPane fromScroll = new ScrollPane(fromPropsBox);
    fromScroll.setFitToWidth(true);
    fromScroll.setPrefViewportHeight(160);
    ScrollPane toScroll = new ScrollPane(toPropsBox);
    toScroll.setFitToWidth(true);
    toScroll.setPrefViewportHeight(160);

    GridPane grid = new GridPane();
    grid.setHgap(10);
    grid.setVgap(10);
    grid.setPadding(new Insets(12));

    ColumnConstraints c0 = new ColumnConstraints();
    c0.setMinWidth(180);
    c0.setPrefWidth(180);
    ColumnConstraints c1 = new ColumnConstraints();
    c1.setHgrow(Priority.ALWAYS);
    grid.getColumnConstraints().setAll(c0, c1);

    int row = 0;
    grid.add(modeRow, 0, row, 2, 1);
    row++;
    Label fromLabel = new Label("Исходный блок:");
    grid.add(fromLabel, 0, row);
    grid.add(fromBlockCombo, 1, row);
    GridPane.setHgrow(fromBlockCombo, Priority.ALWAYS);
    row++;
    grid.add(new Label("Состояния исходного блока:"), 0, row);
    grid.add(fromScroll, 1, row);
    row++;
    Label toLabel = new Label("Новый блок:");
    grid.add(toLabel, 0, row);
    grid.add(toBlockCombo, 1, row);
    GridPane.setHgrow(toBlockCombo, Priority.ALWAYS);
    row++;
    grid.add(new Label("Состояния нового блока:"), 0, row);
    grid.add(toScroll, 1, row);
    row++;
    grid.add(new Label("Заголовок:"), 0, row);
    grid.add(titleField, 1, row);

    Button ok = new Button("Сохранить");
    Button cancel = new Button("Отмена");
    ok.setDefaultButton(true);
    cancel.setCancelButton(true);

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    HBox buttons = new HBox(10, spacer, ok, cancel);

    VBox root = new VBox(10, grid, buttons);
    VBox.setVgrow(fromScroll, Priority.ALWAYS);
    VBox.setVgrow(toScroll, Priority.ALWAYS);
    root.setPadding(new Insets(12));

    Scene scene = new Scene(root, 720, 520);
    stage.setScene(scene);

    final ReplaceTask[] result = {null};

    ok.setOnAction(
        e -> {
          boolean modsMode = modeGroup.getSelectedToggle() == modsBtn;
          String fromName = trimOrNull(fromBlockCombo.getEditor().getText());
          String toName = trimOrNull(toBlockCombo.getEditor().getText());
          if (fromName == null || toName == null) {
            showError("Укажите исходный и новый блок.");
            return;
          }

          try {
            BlockStateSpec fromSpec = buildSpec(fromName, fromPropsBox);
            BlockStateSpec toSpec = buildSpec(toName, toPropsBox);

            if (!modsMode) {
              BlockDatabase db = BlockDatabase.loadBundled();
              db.validate(fromSpec, false);
              db.validate(toSpec, true);
            }

            String title = trimOrNull(titleField.getText());
            if (title == null) {
              title = fromSpec + " -> " + toSpec;
            }

            String id = initial != null ? initial.id() : UUID.randomUUID().toString();
            boolean matchByNameOnly = matchByNameOnlyCheck.isSelected();
            result[0] = new ReplaceTask(id, title, fromSpec, toSpec, matchByNameOnly, true);
            stage.close();
          } catch (Exception ex) {
            showError("Ошибка: " + ex.getMessage());
          }
        });

    cancel.setOnAction(
        e -> {
          result[0] = null;
          stage.close();
        });

    stage.showAndWait();
    return result[0];
  }

  private static void rebuildProperties(BlockDatabase db, String blockName, VBox container) {
    container.getChildren().clear();
    container.setUserData(null);
    String name = trimOrNull(blockName);
    if (name == null) return;
    BlockDef def = db.block(name).orElse(null);
    if (def == null) {
      Label lbl = new Label("Блок не найден в базе. Введите состояния вручную (для модов):");
      TextField raw = new TextField();
      raw.setPromptText("ключ=значение,ключ2=значение2 (например: powered=true,facing=north)");
      container.getChildren().addAll(lbl, raw);
      container.setUserData(raw);
      return;
    }

    GridPane props = new GridPane();
    props.setHgap(6);
    props.setVgap(8);

    ColumnConstraints k = new ColumnConstraints();
    k.setMinWidth(Region.USE_PREF_SIZE);
    k.setPrefWidth(Region.USE_COMPUTED_SIZE);
    ColumnConstraints v = new ColumnConstraints();
    v.setHgrow(Priority.ALWAYS);
    props.getColumnConstraints().setAll(k, v);

    if (def.properties().isEmpty()) {
      Label noProps = new Label("У этого блока нет настраиваемых состояний.");
      container.getChildren().add(noProps);
      container.setUserData(Map.of());
      return;
    }

    LinkedHashMap<String, ComboBox<String>> controls = new LinkedHashMap<>();

    int row = 0;
    for (PropertyDef pd : def.properties().values()) {
      Label l = new Label(pd.name());
      ComboBox<String> valueBox = new ComboBox<>();
      valueBox.setEditable(true);
      valueBox.getItems().add(BlockStateSpec.ALL);
      valueBox.getItems().addAll(pd.allowedValues());
      valueBox.setMaxWidth(Double.MAX_VALUE);
      props.add(l, 0, row);
      props.add(valueBox, 1, row);
      GridPane.setHgrow(valueBox, Priority.ALWAYS);
      controls.put(pd.name(), valueBox);
      row++;
    }

    container.getChildren().add(props);
    container.setUserData(controls);
  }

  private static void rebuildPropertiesFromSpec(
      BlockDatabase db, BlockStateSpec spec, VBox container) {
    rebuildProperties(db, spec.name(), container);
    Object ud = container.getUserData();
    Map<String, String> props = spec.properties();
    if (ud instanceof TextField raw) {
      // Unknown block: keep raw properties string.
      if (!props.isEmpty()) {
        String joined =
            props.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).reduce((a, b) -> a + "," + b).orElse("");
        raw.setText(joined);
      }
      return;
    }
    if (!(ud instanceof Map<?, ?> map)) return;
    for (Map.Entry<String, String> e : props.entrySet()) {
      Object ctrl = map.get(e.getKey());
      if (ctrl instanceof ComboBox<?> cb) {
        @SuppressWarnings("unchecked")
        ComboBox<String> combo = (ComboBox<String>) cb;
        combo.getSelectionModel().select(e.getValue());
        combo.getEditor().setText(e.getValue());
      }
    }
  }

  private static BlockStateSpec buildSpec(String name, VBox container) {
    Object ud = container.getUserData();
    if (ud instanceof TextField raw) {
      String t = trimOrNull(raw.getText());
      if (t == null) return new BlockStateSpec(name, Map.of());
      return BlockStateSpec.parse(name + "[" + t + "]");
    }
    if (!(ud instanceof Map<?, ?> map)) {
      return new BlockStateSpec(name, Map.of());
    }

    Map<String, String> props = new LinkedHashMap<>();
    for (Map.Entry<?, ?> e : map.entrySet()) {
      if (!(e.getKey() instanceof String key)) continue;
      Object ctrl = e.getValue();
      if (ctrl instanceof ComboBox<?> cb) {
        @SuppressWarnings("unchecked")
        ComboBox<String> combo = (ComboBox<String>) cb;
        String value = trimOrNull(combo.getEditor().getText());
        if (value != null) props.put(key, value);
      }
    }

    return new BlockStateSpec(name, props);
  }

  private static String trimOrNull(String s) {
    if (s == null) return null;
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }

  private static void showError(String message) {
    new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).showAndWait();
  }
}

