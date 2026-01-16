package at.rayman.runafterdependency;

import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.ui.SettingsEditorFragment;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.ui.components.fields.ExpandableTextField;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class TextTriggerFragment<T extends RunConfigurationBase<?>> extends SettingsEditorFragment<T, JComponent> {

    public TextTriggerFragment() {
        super("BeforeLaunchTextTrigger", "Trigger text", "Before Launch",
                createComponent(),
                (config, panel) -> {
                },
                (config, panel) -> {
                },
                (config) -> config.getBeforeRunTasks().stream()
                        .anyMatch(t -> t instanceof TextTriggerBeforeRunProvider.RunConfigurableBeforeRunTask)
        );
        //text only wraps if html
        setHint("<html>Text listened for in output of Dependent Before Launch Task to execute this run configuration</html>");
        setActionHint("Automatically shown when Dependent Before Launch Task is added");
    }

    @Override
    public int getMenuPosition() {
        //BeforeRunFragment is 100. This makes it last entry within same group
        return 100;
    }

    @Override
    protected void resetEditorFrom(@NotNull T config) {
        config.getBeforeRunTasks().stream()
                .filter(t -> t instanceof TextTriggerBeforeRunProvider.RunConfigurableBeforeRunTask)
                .map(t -> (TextTriggerBeforeRunProvider.RunConfigurableBeforeRunTask) t)
                .forEach(task -> {
                    getComboBox().setSelectedItem(task.getTriggerCondition());
                    getTextField().setText(task.getTriggerText());
                });
    }

    @Override
    protected void applyEditorTo(@NotNull T config) {
        boolean shouldBeSelected = config.getBeforeRunTasks().stream().anyMatch(t -> t instanceof TextTriggerBeforeRunProvider.RunConfigurableBeforeRunTask);
        if (isSelected() != shouldBeSelected) {
            setSelected(shouldBeSelected);
        }
        config.getBeforeRunTasks().stream()
                .filter(t -> t instanceof TextTriggerBeforeRunProvider.RunConfigurableBeforeRunTask)
                .map(t -> (TextTriggerBeforeRunProvider.RunConfigurableBeforeRunTask) t)
                .forEach(task -> {
                    task.setTriggerCondition((String) getComboBox().getSelectedItem());
                    task.setTriggerText(getTextField().getText());
                });
    }

    private ComboBox<String> getComboBox() {
        return (ComboBox<String>) ((JPanel) component().getComponent(0)).getComponent(0);
    }

    private JTextField getTextField() {
        return (JTextField) ((JPanel) component().getComponent(0)).getComponent(1);
    }

    private static JComponent createComponent() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new ComboBox<>(List.of("exact", "contains", "startsWith", "endsWith").toArray(new String[0])), BorderLayout.WEST);
        panel.add(new ExpandableTextField(ParametersListUtil.COLON_LINE_PARSER, ParametersListUtil.COLON_LINE_JOINER));
        return LabeledComponent.create(panel, "Text trigger", BorderLayout.WEST);
    }

}
