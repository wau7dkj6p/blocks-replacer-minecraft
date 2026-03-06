package com.blockreplace.gui;

import com.blockreplace.core.blockdb.BlockDatabase;
import com.blockreplace.core.task.BlockStateSpec;
import com.blockreplace.core.task.ReplaceTask;
import com.blockreplace.core.world.WorldDimension;
import com.blockreplace.core.world.WorldProcessor;
import com.blockreplace.core.world.WorldProcessorOptions;
import com.blockreplace.core.world.WorldProgressListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class BlockReplaceGuiApp extends Application {
  public static void launchGui(String[] args) {
    Application.launch(BlockReplaceGuiApp.class, args == null ? new String[0] : args);
  }

  private final AtomicBoolean running = new AtomicBoolean(false);
  private final java.util.concurrent.ExecutorService executor =
      Executors.newSingleThreadExecutor();
  private volatile WorldProcessor currentProcessor;

  @Override
  public void start(Stage primaryStage) {
    primaryStage.setTitle("block-replace");
    primaryStage.setWidth(1100);
    primaryStage.setHeight(750);
    primaryStage.setResizable(false);

    applyMainWindowIcons(primaryStage);

    SplitPane split = new SplitPane();
    split.setOrientation(Orientation.VERTICAL);

    BorderPane top = buildTopPane(primaryStage);
    VBox bottom = buildBottomPane();

    split.getItems().addAll(top, bottom);
    split.setDividerPositions(0.6);

    Scene scene = new Scene(split);
    primaryStage.setScene(scene);
    primaryStage.show();
  }

  static void applyMainWindowIcons(Stage stage) {
    List<String> candidates = new ArrayList<>();
    // Для главного окна: сначала ICO, чтобы его использовала панель задач,
    // затем PNG‑иконки для качественного отображения в разных размерах.
    candidates.add("/icons/block-replace.ico");
    candidates.add("/icons/favicon-16x16.png");
    candidates.add("/icons/favicon-32x32.png");
    candidates.add("/icons/favicon-48x48.png");
    candidates.add("/icons/favicon-64x64.png");
    candidates.add("/icons/favicon-96x96.png");

    for (String path : candidates) {
      var stream = BlockReplaceGuiApp.class.getResourceAsStream(path);
      if (stream == null) {
        continue;
      }
      Image img = new Image(stream);
      if (!img.isError() && img.getWidth() > 0 && img.getHeight() > 0) {
        stage.getIcons().add(img);
      }
    }
  }

  static void applyDialogIcons(Stage stage) {
    List<String> candidates = new ArrayList<>();
    candidates.add("/icons/favicon-16x16.png");
    candidates.add("/icons/favicon-32x32.png");
    candidates.add("/icons/favicon-48x48.png");
    candidates.add("/icons/favicon-64x64.png");
    candidates.add("/icons/favicon-96x96.png");

    for (String path : candidates) {
      var stream = BlockReplaceGuiApp.class.getResourceAsStream(path);
      if (stream == null) {
        continue;
      }
      Image img = new Image(stream);
      if (!img.isError() && img.getWidth() > 0 && img.getHeight() > 0) {
        stage.getIcons().add(img);
      }
    }
  }

  @Override
  public void stop() {
    running.set(false);
    WorldProcessor processor = currentProcessor;
    if (processor != null) {
      processor.cancel();
    }
    executor.shutdownNow();
  }

  private BorderPane buildTopPane(Stage stage) {
    BorderPane root = new BorderPane();
    root.setPadding(new Insets(8));

    VBox controls = new VBox(8);

    // World selection
    HBox worldRow = new HBox(6);
    Label worldLabel = new Label("Мир (level.dat):");
    TextField worldField = new TextField();
    worldField.setEditable(false);
    Button worldBrowse = new Button("Выбрать...");
    worldRow.getChildren().addAll(worldLabel, worldField, worldBrowse);
    HBox.setHgrow(worldField, Priority.ALWAYS);

    Label worldInfo = new Label("Мир не выбран");

    // Dimension selection
    CheckBox overworldBox = new CheckBox("Overworld");
    overworldBox.setSelected(true);
    CheckBox netherBox = new CheckBox("Nether");
    CheckBox endBox = new CheckBox("End");
    HBox dimsRow = new HBox(8, new Label("Измерения:"), overworldBox, netherBox, endBox);

    // Tasks list and buttons
    ListView<ReplaceTask> taskList = new ListView<>();
    taskList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    taskList.setCellFactory(
        lv ->
            new ListCell<>() {
              @Override
              protected void updateItem(ReplaceTask item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                  setText(null);
                } else if (item.title() != null && !item.title().isBlank()) {
                  setText(item.title());
                } else {
                  setText(item.from() + " -> " + item.to());
                }
              }
            });

    HBox taskButtons = new HBox(6);
    Button exportBtn = new Button("Экспортировать");
    Button importBtn = new Button("Импортировать");
    Button addBtn = new Button("Добавить задачу");
    Button deleteBtn = new Button("Удалить выбранное");
    Button editBtn = new Button("Редактировать выбранное");
    deleteBtn.setDisable(true);
    editBtn.setDisable(true);
    taskButtons.getChildren().addAll(exportBtn, importBtn, addBtn, deleteBtn, editBtn);

    controls.getChildren().addAll(worldRow, worldInfo, dimsRow, taskButtons, taskList);
    VBox.setVgrow(taskList, Priority.ALWAYS);

    root.setCenter(controls);

    // World chooser (Windows-like dialog)
    worldBrowse.setOnAction(
        e -> {
          FileChooser fc = new FileChooser();
          fc.setTitle("Выберите level.dat");
          fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("level.dat", "level.dat"));

          String appData = System.getenv("APPDATA");
          if (appData != null && !appData.isBlank()) {
            Path saves = Paths.get(appData, ".minecraft", "saves");
            if (Files.isDirectory(saves)) {
              fc.setInitialDirectory(saves.toFile());
            }
          }

          File file = fc.showOpenDialog(stage);
          if (file != null) {
            worldField.setText(file.getAbsolutePath());
            String worldFolderName =
                file.getParentFile() == null ? file.getParent() : file.getParentFile().getName();
            worldInfo.setText("Мир: " + worldFolderName);
          }
        });

    // Add task via dedicated dialog
    addBtn.setOnAction(
        e -> {
          TaskEditorDialog dialog = new TaskEditorDialog(stage, null);
          ReplaceTask created = dialog.showAndWait();
          if (created != null) {
            taskList.getItems().add(created);
          }
        });

    deleteBtn.setOnAction(
        e -> {
          var sel = taskList.getSelectionModel().getSelectedItems();
          if (sel.isEmpty()) return;
          taskList.getItems().removeAll(sel);
        });

    editBtn.setOnAction(
        e -> {
          ReplaceTask existing = taskList.getSelectionModel().getSelectedItem();
          if (existing == null) return;
          int idx = taskList.getSelectionModel().getSelectedIndex();
          TaskEditorDialog dialog = new TaskEditorDialog(stage, existing);
          ReplaceTask updated = dialog.showAndWait();
          if (updated != null) {
            taskList.getItems().set(idx, updated);
          }
        });

    // Enable/disable buttons based on selection.
    var selectionModel = taskList.getSelectionModel();
    selectionModel
        .getSelectedIndices()
        .addListener(
            (ListChangeListener<Integer>)
                c -> {
                  int count = selectionModel.getSelectedIndices().size();
                  deleteBtn.setDisable(count == 0);
                  editBtn.setDisable(count != 1);
                });

    // Export tasks to JSON
    exportBtn.setOnAction(
        e -> {
          if (taskList.getItems().isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Нет задач для экспорта.", ButtonType.OK)
                .showAndWait();
            return;
          }
          FileChooser fc = new FileChooser();
          fc.setTitle("Экспорт задач");
          fc.getExtensionFilters()
              .add(new FileChooser.ExtensionFilter("JSON файлы (*.json)", "*.json"));
          fc.setInitialFileName("block-replace-tasks.json");
          File file = fc.showSaveDialog(stage);
          if (file == null) {
            return;
          }
          try {
            ObjectMapper mapper =
                new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(file, taskList.getItems());
          } catch (IOException ex) {
            new Alert(
                    Alert.AlertType.ERROR,
                    "Ошибка экспорта задач: " + ex.getMessage(),
                    ButtonType.OK)
                .showAndWait();
          }
        });

    // Import tasks from JSON
    importBtn.setOnAction(
        e -> {
          FileChooser fc = new FileChooser();
          fc.setTitle("Импорт задач");
          fc.getExtensionFilters()
              .add(new FileChooser.ExtensionFilter("JSON файлы (*.json)", "*.json"));
          File file = fc.showOpenDialog(stage);
          if (file == null) {
            return;
          }
          try {
            ObjectMapper mapper = new ObjectMapper();
            ReplaceTask[] imported = mapper.readValue(file, ReplaceTask[].class);
            taskList.getItems().setAll(imported);
          } catch (IOException ex) {
            new Alert(
                    Alert.AlertType.ERROR,
                    "Ошибка импорта задач: " + ex.getMessage(),
                    ButtonType.OK)
                .showAndWait();
          }
        });

    // Attach tasks list, world field and dimension checkboxes into root user data for bottom pane
    // usage.
    root.setUserData(
        new TopContext(worldField, taskList, overworldBox, netherBox, endBox));

    return root;
  }

  private VBox buildBottomPane() {
    VBox box = new VBox(6);
    box.setPadding(new Insets(6));

    ListView<String> console = new ListView<>();
    VBox.setVgrow(console, Priority.ALWAYS);

    HBox runRow = new HBox(6);
    Button runBtn = new Button("Выполнить задачи");
    ProgressBar progress = new ProgressBar(0);
    progress.setMaxWidth(Double.MAX_VALUE);
    progress.prefHeightProperty().bind(runBtn.heightProperty());
    Label filesLabel = new Label("0 / 0 файлов");
    HBox.setHgrow(progress, Priority.ALWAYS);
    HBox.setHgrow(filesLabel, Priority.ALWAYS);
    runRow.getChildren().addAll(runBtn, progress, filesLabel);

    box.getChildren().addAll(console, runRow);

    runBtn.setOnAction(
        e -> {
          if (running.get()) {
            WorldProcessor processor = currentProcessor;
            if (processor != null) {
              processor.cancel();
              console
                  .getItems()
                  .add("[INFO] Остановка обработки по запросу пользователя...");
              if (!console.getItems().isEmpty()) {
                console.scrollTo(console.getItems().size() - 1);
              }
              runBtn.setDisable(true);
            }
            return;
          }
          // Find top pane context
          if (box.getScene() == null || box.getScene().getRoot() == null) {
            console.getItems().add("[ERROR] Внутренняя ошибка UI (нет сцены)");
            return;
          }
          SplitPane split;
          try {
            split = (SplitPane) box.getScene().getRoot();
          } catch (ClassCastException ex) {
            console.getItems().add("[ERROR] Внутренняя ошибка UI (ожидался SplitPane)");
            return;
          }
          BorderPane top = (BorderPane) split.getItems().get(0);
          TopContext ctx = (TopContext) top.getUserData();
          if (ctx == null) return;
          String levelPath = ctx.levelDatField.getText();
          if (levelPath == null || levelPath.isBlank()) {
            console.getItems().add("[WARN] Выберите level.dat");
            return;
          }
          List<ReplaceTask> tasks = List.copyOf(ctx.taskList.getItems());
          if (tasks.isEmpty()) {
            console.getItems().add("[WARN] Добавьте хотя бы одну задачу");
            return;
          }

          EnumSet<WorldDimension> dimensions = EnumSet.noneOf(WorldDimension.class);
          if (ctx.overworldBox.isSelected()) {
            dimensions.add(WorldDimension.OVERWORLD);
          }
          if (ctx.netherBox.isSelected()) {
            dimensions.add(WorldDimension.NETHER);
          }
          if (ctx.endBox.isSelected()) {
            dimensions.add(WorldDimension.END);
          }
          if (dimensions.isEmpty()) {
            console.getItems().add("[WARN] Выберите хотя бы одно измерение");
            return;
          }

          Path worldRoot = Path.of(levelPath).toAbsolutePath().getParent();
          Path stateFile = worldRoot.resolve(".block-replace-state.json");

          boolean resumeStateFlag = false;
          if (Files.isRegularFile(stateFile)) {
            Alert alert =
                new Alert(
                    Alert.AlertType.CONFIRMATION,
                    "Найден сохранённый прогресс обработки мира.\n"
                        + "Вы хотите продолжить с места остановки или начать новую сессию?",
                    new ButtonType("Продолжить", ButtonBar.ButtonData.YES),
                    new ButtonType("Новая сессия", ButtonBar.ButtonData.NO),
                    ButtonType.CANCEL);
            alert.setHeaderText("Продолжить или начать заново?");
            var result = alert.showAndWait();
            if (result.isEmpty()
                || result.get().getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
              console.getItems().add("[INFO] Отменено пользователем");
              return;
            }
            if (result.get().getButtonData() == ButtonBar.ButtonData.YES) {
              resumeStateFlag = true;
            } else {
              try {
                Files.deleteIfExists(stateFile);
              } catch (IOException ex) {
                console
                    .getItems()
                    .add("[ERROR] Не удалось удалить файл состояния: " + ex.getMessage());
                return;
              }
            }
          }

          startProcessing(
              worldRoot,
              dimensions,
              tasks,
              resumeStateFlag,
              stateFile,
              console,
              filesLabel,
              progress,
              runBtn);
        });

    box.setUserData(new BottomContext(console, filesLabel, progress, runBtn));
    return box;
  }

  private void startProcessing(
      Path worldRoot,
      EnumSet<WorldDimension> dimensions,
      List<ReplaceTask> tasks,
      boolean resumeFromState,
      Path stateFile,
      ListView<String> console,
      Label filesLabel,
      ProgressBar progress,
      Button runBtn) {
    running.set(true);
    console.getItems().clear();
    progress.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
    runBtn.setText("Остановить");

    final boolean allowUnknownBlocks = true;
    final boolean saveStateFlag = true;

    executor.submit(
        () -> {
          try {
            BlockDatabase db = BlockDatabase.loadBundled();
            WorldProcessorOptions opts =
                new WorldProcessorOptions(
                    worldRoot,
                    dimensions,
                    false,
                    true,
                    allowUnknownBlocks,
                    saveStateFlag,
                    resumeFromState,
                    true);
            WorldProcessor processor =
                new WorldProcessor(
                    opts,
                    tasks,
                    db,
                    new GuiProgressListener(console, filesLabel, progress));
            currentProcessor = processor;
            processor.run();
          } catch (Exception ex) {
            ex.printStackTrace();
            Platform.runLater(
                () -> {
                  console.getItems().add("[ERROR] " + ex.getMessage());
                  if (!console.getItems().isEmpty()) {
                    console.scrollTo(console.getItems().size() - 1);
                  }
                });
          } finally {
            Platform.runLater(
                () -> {
                  running.set(false);
                  runBtn.setDisable(false);
                  boolean hasState = Files.isRegularFile(stateFile);
                  WorldProcessor processor = currentProcessor;
                  boolean completedNormally =
                      processor != null && processor.isCompletedNormally();
                  if (hasState) {
                    runBtn.setText("Продолжить");
                  } else {
                    runBtn.setText("Выполнить задачи");
                  }
                  if (progress.getProgress()
                      == ProgressIndicator.INDETERMINATE_PROGRESS) {
                    progress.setProgress(1.0);
                  }
                  if (completedNormally) {
                    Alert alert =
                        new Alert(
                            Alert.AlertType.INFORMATION,
                            "Обработка мира завершена.",
                            ButtonType.OK);
                    alert.setHeaderText("Готово");
                    alert.showAndWait();
                  }
                  currentProcessor = null;
                });
          }
        });
  }

  private record TopContext(
      TextField levelDatField,
      ListView<ReplaceTask> taskList,
      CheckBox overworldBox,
      CheckBox netherBox,
      CheckBox endBox) {}

  private record BottomContext(
      ListView<String> console,
      Label filesLabel,
      ProgressBar progress,
      Button runBtn) {}

  private static final class GuiProgressListener implements WorldProgressListener {
    private final ListView<String> console;
    private final Label filesLabel;
    private final ProgressBar progressBar;

    private int totalRegions;
    private int doneRegions;
    private long worldBlocksAffected;
    private int worldChunksVisited;
    private int worldChunksModified;

    GuiProgressListener(
        ListView<String> console, Label filesLabel, ProgressBar progressBar) {
      this.console = console;
      this.filesLabel = filesLabel;
      this.progressBar = progressBar;
    }

    @Override
    public void onInfo(String message) {
      Platform.runLater(
          () -> {
            console.getItems().add("[INFO] " + message);
            if (!console.getItems().isEmpty()) {
              console.scrollTo(console.getItems().size() - 1);
            }
          });
    }

    @Override
    public void onWarn(String message) {
      Platform.runLater(
          () -> {
            console.getItems().add("[WARN] " + message);
            if (!console.getItems().isEmpty()) {
              console.scrollTo(console.getItems().size() - 1);
            }
          });
    }

    @Override
    public void onError(String message, Throwable t) {
      Platform.runLater(
          () -> {
            console.getItems().add("[ERROR] " + message);
            if (!console.getItems().isEmpty()) {
              console.scrollTo(console.getItems().size() - 1);
            }
          });
    }

    @Override
    public void onWorldStart(int totalRegionFiles) {
      Platform.runLater(
          () -> {
            this.totalRegions = totalRegionFiles;
            this.doneRegions = 0;
            this.worldBlocksAffected = 0L;
            this.worldChunksVisited = 0;
            this.worldChunksModified = 0;
            filesLabel.setText("0 / " + totalRegions + " файлов");
            progressBar.setProgress(totalRegions == 0 ? 1.0 : 0.0);
          });
    }

    @Override
    public void onRegionStart(WorldDimension dim, Path regionFile, int presentChunks) {
      Platform.runLater(
          () -> {
            console
                .getItems()
                .add(
                    "Начало "
                        + dim
                        + " "
                        + regionFile.getFileName()
                        + " (чанков: "
                        + presentChunks
                        + ")");
            if (!console.getItems().isEmpty()) {
              console.scrollTo(console.getItems().size() - 1);
            }
          });
    }

    @Override
    public void onChunkDone(
        WorldDimension dim,
        Path regionFile,
        int chunkX,
        int chunkZ,
        long blocksAffected,
        int paletteChanges,
        int blockEntitiesRemoved) {
      if (blocksAffected == 0 && paletteChanges == 0 && blockEntitiesRemoved == 0) return;
      Platform.runLater(
          () -> {
            console
                .getItems()
                .add(
                    String.format(
                        "  %s %s chunk(%d,%d): blocks=%d palette=%d beRemoved=%d",
                        dim,
                        regionFile.getFileName(),
                        chunkX,
                        chunkZ,
                        blocksAffected,
                        paletteChanges,
                        blockEntitiesRemoved));
            if (!console.getItems().isEmpty()) {
              console.scrollTo(console.getItems().size() - 1);
            }
          });
    }

    @Override
    public void onRegionDone(
        WorldDimension dim,
        Path regionFile,
        boolean modified,
        int chunksModified,
        long blocksAffected,
        int paletteChanges,
        int blockEntitiesRemoved) {
      Platform.runLater(
          () -> {
            doneRegions++;
            console
                .getItems()
                .add(
                    String.format(
                        "Конец %s %s: изменённых чанков=%d, блоков=%d, palette=%d, удалено BE=%d",
                        dim,
                        regionFile.getFileName(),
                        chunksModified,
                        blocksAffected,
                        paletteChanges,
                        blockEntitiesRemoved));
            if (!console.getItems().isEmpty()) {
              console.scrollTo(console.getItems().size() - 1);
            }
            filesLabel.setText(doneRegions + " / " + totalRegions + " файлов");
            double p = totalRegions == 0 ? 1.0 : (double) doneRegions / totalRegions;
            progressBar.setProgress(p);
          });

      worldBlocksAffected += blocksAffected;
      worldChunksModified += chunksModified;
    }

    @Override
    public void onWorldDone(
        int totalRegionFiles,
        int totalChunksVisited,
        int totalChunksModified,
        long totalBlocksAffected) {
      Platform.runLater(
          () -> {
            worldChunksVisited = totalChunksVisited;
            worldChunksModified = totalChunksModified;
            worldBlocksAffected = totalBlocksAffected;

            console
                .getItems()
                .add(
                    String.format(
                        "Итого по миру: регионов=%d, посещённых чанков=%d, изменённых чанков=%d, затронуто блоков=%d",
                        totalRegionFiles, worldChunksVisited, worldChunksModified, worldBlocksAffected));
            if (!console.getItems().isEmpty()) {
              console.scrollTo(console.getItems().size() - 1);
            }
          });
    }
  }
}

