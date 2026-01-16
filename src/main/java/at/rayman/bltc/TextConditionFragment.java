package at.rayman.bltc;

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

public class TextConditionFragment<T extends RunConfigurationBase<?>> extends SettingsEditorFragment<T, JComponent> {

    public TextConditionFragment() {
        super("TextCondition", "Text condition", "Before Launch",
                createComponent(),
                (config, panel) -> {},
                (config, panel) -> {},
                (config) -> config.getBeforeRunTasks().stream()
                        .anyMatch(t -> t instanceof TextConditionBeforeRunProvider.TextConditionBeforeRunTask)
        );
        //text only wraps if html
        setHint("<html>Text listened for in before launch task's output</html>");
        setActionHint("Automatically selected when Text Condition Before Launch Task is added");
    }

    @Override
    public int getMenuPosition() {
        //BeforeRunFragment is 100. This makes it last entry within same group
        return 100;
    }

    @Override
    protected void resetEditorFrom(@NotNull T config) {
        config.getBeforeRunTasks().stream()
                .filter(t -> t instanceof TextConditionBeforeRunProvider.TextConditionBeforeRunTask)
                .map(t -> (TextConditionBeforeRunProvider.TextConditionBeforeRunTask) t)
                .forEach(task -> {
                    getComboBox().setSelectedItem(task.getState().getTriggerCondition());
                    getTextField().setText(task.getState().getTriggerText());
                });
    }

    @Override
    protected void applyEditorTo(@NotNull T config) {
        boolean shouldBeSelected = config.getBeforeRunTasks().stream().anyMatch(t -> t instanceof TextConditionBeforeRunProvider.TextConditionBeforeRunTask);
        if (isSelected() != shouldBeSelected) {
            setSelected(shouldBeSelected);
        }
        config.getBeforeRunTasks().stream()
                .filter(t -> t instanceof TextConditionBeforeRunProvider.TextConditionBeforeRunTask)
                .map(t -> (TextConditionBeforeRunProvider.TextConditionBeforeRunTask) t)
                .forEach(task -> {
                    task.getState().setTriggerCondition((String) getComboBox().getSelectedItem());
                    task.getState().setTriggerText(getTextField().getText());
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
        return LabeledComponent.create(panel, "Condition", BorderLayout.WEST);
    }

}
