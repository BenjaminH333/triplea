package games.strategy.triplea.settings;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.prefs.Preferences;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import com.google.common.base.Strings;

import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.system.HttpProxy;
import games.strategy.ui.SwingComponents;
import swinglib.JButtonBuilder;
import swinglib.JPanelBuilder;

/**
 * Logic for building UI components that "bind" to ClientSettings.
 * For example, if we have a setting that needs a number, we could create an integer text field with this
 * class. This class takes care of the UI code to ensure we render the proper swing component with validation.
 */
final class SelectionComponentFactory {
  private SelectionComponentFactory() {}

  static Supplier<SelectionComponent> proxySettings() {
    return () -> new SelectionComponent() {
      final Preferences pref = Preferences.userNodeForPackage(GameRunner.class);
      final HttpProxy.ProxyChoice proxyChoice =
          HttpProxy.ProxyChoice.valueOf(pref.get(HttpProxy.PROXY_CHOICE, HttpProxy.ProxyChoice.NONE.toString()));
      final JRadioButton noneButton = new JRadioButton("None", proxyChoice == HttpProxy.ProxyChoice.NONE);
      final JRadioButton systemButton =
          new JRadioButton("Use System Settings", proxyChoice == HttpProxy.ProxyChoice.USE_SYSTEM_SETTINGS);


      final JRadioButton userButton =
          new JRadioButton("Use These Settings:", proxyChoice == HttpProxy.ProxyChoice.USE_USER_PREFERENCES);
      final JTextField hostText = new JTextField(ClientSetting.PROXY_HOST.value(), 20);
      final JTextField portText = new JTextField(ClientSetting.PROXY_PORT.value(), 6);
      final JPanel radioPanel = JPanelBuilder.builder()
          .verticalBoxLayout()
          .add(noneButton)
          .add(systemButton)
          .add(userButton)
          .add(new JLabel("Proxy Host: "))
          .add(hostText)
          .add(new JLabel("Proxy Port: "))
          .add(portText)
          .build();

      final ActionListener enableUserSettings = e -> {
        if (userButton.isSelected()) {
          hostText.setEnabled(true);
          hostText.setBackground(Color.WHITE);
          portText.setEnabled(true);
          portText.setBackground(Color.WHITE);
        } else {
          hostText.setEnabled(false);
          hostText.setBackground(Color.DARK_GRAY);
          portText.setEnabled(false);
          portText.setBackground(Color.DARK_GRAY);
        }
      };

      @Override
      public JComponent getJComponent() {
        SwingComponents.createButtonGroup(noneButton, systemButton, userButton);
        enableUserSettings.actionPerformed(null);
        userButton.addActionListener(enableUserSettings);
        noneButton.addActionListener(enableUserSettings);
        systemButton.addActionListener(enableUserSettings);

        return radioPanel;
      }

      @Override
      public boolean isValid() {
        return !userButton.isSelected() || (isHostTextValid() && isPortTextValid());
      }

      private boolean isHostTextValid() {
        return !Strings.nullToEmpty(hostText.getText()).trim().isEmpty();
      }

      private boolean isPortTextValid() {
        final String value = Strings.nullToEmpty(portText.getText()).trim();
        if (value.isEmpty()) {
          return false;
        }

        try {
          return Integer.parseInt(value) > 0;
        } catch (final NumberFormatException e) {
          return false;
        }
      }

      @Override
      public String validValueDescription() {
        return "Proxy host can be a network name or an IP address, port should be number, usually 4 to 5 digits.";
      }

      @Override
      public Map<GameSetting, String> readValues() {
        final Map<GameSetting, String> values = new HashMap<>();
        if (noneButton.isSelected()) {
          values.put(ClientSetting.PROXY_CHOICE, HttpProxy.ProxyChoice.NONE.toString());
        } else if (systemButton.isSelected()) {
          values.put(ClientSetting.PROXY_CHOICE, HttpProxy.ProxyChoice.USE_SYSTEM_SETTINGS.toString());
          HttpProxy.updateSystemProxy();
        } else {
          values.put(ClientSetting.PROXY_CHOICE, HttpProxy.ProxyChoice.USE_USER_PREFERENCES.toString());
          values.put(ClientSetting.PROXY_HOST, hostText.getText().trim());
          values.put(ClientSetting.PROXY_PORT, portText.getText().trim());
        }
        return values;
      }

      @Override
      public void indicateError() {
        if (!isHostTextValid()) {
          hostText.setBackground(Color.RED);
        }
        if (!isPortTextValid()) {
          portText.setBackground(Color.RED);
        }
      }

      @Override
      public void clearError() {
        hostText.setBackground(Color.WHITE);
        portText.setBackground(Color.WHITE);
      }

      @Override
      public void resetToDefault() {
        ClientSetting.flush();
        hostText.setText(ClientSetting.PROXY_HOST.defaultValue);
        portText.setText(ClientSetting.PROXY_PORT.defaultValue);
        noneButton.setSelected(Boolean.valueOf(ClientSetting.PROXY_CHOICE.defaultValue));
      }

      @Override
      public void reset() {
        ClientSetting.flush();
        hostText.setText(ClientSetting.PROXY_HOST.value());
        portText.setText(ClientSetting.PROXY_PORT.value());
        noneButton.setSelected(ClientSetting.PROXY_CHOICE.booleanValue());
      }
    };
  }

  /**
   * Text field that only accepts numbers between a certain range.
   */
  static Supplier<SelectionComponent> intValueRange(final ClientSetting clientSetting, final int lo, final int hi) {
    return () -> new SelectionComponent() {
      String value = clientSetting.value();
      final JTextField component = new JTextField(value, String.valueOf(hi).length());

      @Override
      public JComponent getJComponent() {
        component.setToolTipText(validValueDescription());

        SwingComponents.addTextFieldFocusLostListener(component, () -> {
          if (isValid()) {
            clearError();
          } else {
            indicateError();
          }
        });

        return component;
      }

      @Override
      public boolean isValid() {
        final String value = component.getText();

        if (value.trim().isEmpty()) {
          return true;
        }

        try {
          final int intValue = Integer.parseInt(value);
          return intValue >= lo && intValue <= hi;
        } catch (final NumberFormatException e) {
          return false;
        }
      }

      @Override
      public String validValueDescription() {
        return "Number between " + lo + " and " + hi;
      }

      @Override
      public void indicateError() {
        component.setBackground(Color.RED);
      }

      @Override
      public void clearError() {
        component.setBackground(Color.WHITE);
      }

      @Override
      public Map<GameSetting, String> readValues() {
        final Map<GameSetting, String> map = new HashMap<>();
        map.put(clientSetting, component.getText());
        return map;
      }

      @Override
      public void resetToDefault() {
        component.setText(clientSetting.defaultValue);
        clearError();
      }

      @Override
      public void reset() {
        component.setText(clientSetting.value());
        clearError();
      }
    };
  }

  /**
   * yes/no radio buttons.
   */
  static SelectionComponent booleanRadioButtons(final ClientSetting clientSetting) {
    return new AlwaysValidInputSelectionComponent() {
      final boolean initialSelection = clientSetting.booleanValue();
      final JRadioButton yesButton = new JRadioButton("True");
      final JRadioButton noButton = new JRadioButton("False");
      final JPanel buttonPanel = JPanelBuilder.builder()
          .horizontalBoxLayout()
          .add(yesButton)
          .add(noButton)
          .build();

      @Override
      public JComponent getJComponent() {
        yesButton.setSelected(initialSelection);
        noButton.setSelected(!initialSelection);
        SwingComponents.createButtonGroup(yesButton, noButton);
        return buttonPanel;
      }

      @Override
      public Map<GameSetting, String> readValues() {
        final String value = yesButton.isSelected() ? String.valueOf(true) : String.valueOf(false);
        final Map<GameSetting, String> settingMap = new HashMap<>();
        settingMap.put(clientSetting, value);
        return settingMap;
      }

      @Override
      public void resetToDefault() {
        yesButton.setSelected(Boolean.valueOf(clientSetting.defaultValue));
        noButton.setSelected(!Boolean.valueOf(clientSetting.defaultValue));
      }

      @Override
      public void reset() {
        yesButton.setSelected(clientSetting.booleanValue());
        noButton.setSelected(!clientSetting.booleanValue());
      }
    };
  }

  /**
   * File selection prompt.
   */
  static Supplier<SelectionComponent> filePath(final ClientSetting clientSetting) {
    return selectFile(clientSetting, SwingComponents.FolderSelectionMode.FILES);
  }

  private static Supplier<SelectionComponent> selectFile(
      final ClientSetting clientSetting,
      final SwingComponents.FolderSelectionMode folderSelectionMode) {
    return () -> new AlwaysValidInputSelectionComponent() {
      final int expectedLength = 20;
      final JTextField field = new JTextField(clientSetting.value(), expectedLength);
      final JButton button = JButtonBuilder.builder()
          .title("Select")
          .actionListener(
              () -> SwingComponents.showJFileChooser(folderSelectionMode)
                  .ifPresent(file -> field.setText(file.getAbsolutePath())))
          .build();

      @Override
      public JComponent getJComponent() {
        field.setEditable(false);

        return JPanelBuilder.builder()
            .horizontalBoxLayout()
            .add(field)
            .add(Box.createHorizontalStrut(10))
            .add(button)
            .build();
      }

      @Override
      public Map<GameSetting, String> readValues() {
        final String value = field.getText();
        final Map<GameSetting, String> settingMap = new HashMap<>();
        settingMap.put(clientSetting, value);
        return settingMap;
      }

      @Override
      public void resetToDefault() {
        field.setText(clientSetting.defaultValue);
        clearError();
      }

      @Override
      public void reset() {
        field.setText(clientSetting.value());
        clearError();
      }
    };
  }

  /**
   * Folder selection prompt.
   */
  static Supplier<SelectionComponent> folderPath(final ClientSetting clientSetting) {
    return selectFile(clientSetting, SwingComponents.FolderSelectionMode.DIRECTORIES);
  }

  static Supplier<SelectionComponent> selectionBox(
      final ClientSetting clientSetting,
      final List<String> availableOptions) {
    return () -> new AlwaysValidInputSelectionComponent() {
      final JComboBox<String> comboBox = new JComboBox<>(availableOptions.toArray(new String[availableOptions.size()]));

      @Override
      public JComponent getJComponent() {
        comboBox.setSelectedItem(clientSetting.value());
        return comboBox;
      }

      @Override
      public Map<GameSetting, String> readValues() {
        final String value = String.valueOf(comboBox.getSelectedItem());
        final Map<GameSetting, String> settingMap = new HashMap<>();
        settingMap.put(clientSetting, value);
        return settingMap;
      }

      @Override
      public void resetToDefault() {
        comboBox.setSelectedItem(clientSetting.defaultValue);
        clearError();
      }

      @Override
      public void reset() {
        comboBox.setSelectedItem(clientSetting.value());
        clearError();
      }
    };
  }

  static Supplier<SelectionComponent> textField(final ClientSetting clientSetting) {
    return () -> new AlwaysValidInputSelectionComponent() {
      final JTextField textField = new JTextField(clientSetting.value(), 20);

      @Override
      public JComponent getJComponent() {
        return textField;
      }

      @Override
      public Map<GameSetting, String> readValues() {
        final Map<GameSetting, String> map = new HashMap<>();
        map.put(clientSetting, textField.getText());
        return map;
      }

      @Override
      public void reset() {
        textField.setText(clientSetting.value());
        clearError();
      }

      @Override
      public void resetToDefault() {
        textField.setText(clientSetting.defaultValue);
        clearError();
      }
    };
  }

  private abstract static class AlwaysValidInputSelectionComponent implements SelectionComponent {
    @Override
    public void indicateError() {
      // no-op, component only allows valid selections
    }

    @Override
    public void clearError() {
      // also a no-op
    }

    @Override
    public boolean isValid() {
      return true;
    }

    @Override
    public String validValueDescription() {
      return "";
    }
  }
}
