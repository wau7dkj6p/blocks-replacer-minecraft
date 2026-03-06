package com.blockreplace.gui;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

final class FxAutoComplete {
  private FxAutoComplete() {}

  static void enableContainsAutoComplete(ComboBox<String> combo, List<String> allItems) {
    Objects.requireNonNull(combo, "combo");
    Objects.requireNonNull(allItems, "allItems");

    combo.setEditable(true);
    ObservableList<String> base = FXCollections.observableArrayList(allItems);
    FilteredList<String> filtered = new FilteredList<>(base, s -> true);
    combo.setItems(filtered);

    TextField editor = combo.getEditor();

    Runnable applyFilter =
        () -> {
          String q = editor.getText() == null ? "" : editor.getText().trim();
          if (q.isEmpty()) {
            filtered.setPredicate(s -> true);
            combo.getSelectionModel().clearSelection();
            combo.hide();
            return;
          }
          String qLower = q.toLowerCase(Locale.ROOT);
          filtered.setPredicate(item -> item != null && item.toLowerCase(Locale.ROOT).contains(qLower));
          combo.getSelectionModel().clearSelection();
          if (!filtered.isEmpty()) {
            Platform.runLater(
                () -> {
                  if (!combo.isShowing()) combo.show();
                });
          } else {
            combo.hide();
          }
        };

    editor.addEventFilter(
        KeyEvent.KEY_RELEASED,
        e -> {
          KeyCode c = e.getCode();
          if (c == null) return;
          if (c.isArrowKey() || c == KeyCode.ENTER || c == KeyCode.TAB || c == KeyCode.SHIFT) {
            return;
          }
          if (c == KeyCode.ESCAPE) {
            combo.hide();
            return;
          }
          applyFilter.run();
        });

    combo.addEventFilter(
        KeyEvent.KEY_PRESSED,
        e -> {
          if (e.getCode() == KeyCode.ESCAPE) {
            combo.hide();
          }
          // Do not auto-fill on BACK_SPACE / DELETE (prevents erase issues).
        });

    Runnable commitEditorToValue =
        () -> {
          String t = editor.getText();
          if (t == null) return;
          String trimmed = t.trim();
          if (!trimmed.isEmpty()) {
            combo.setValue(trimmed);
          }
        };

    editor.setOnAction(e -> commitEditorToValue.run());
    editor
        .focusedProperty()
        .addListener(
            (obs, old, focused) -> {
              if (!focused) {
                commitEditorToValue.run();
              }
            });
  }
}

