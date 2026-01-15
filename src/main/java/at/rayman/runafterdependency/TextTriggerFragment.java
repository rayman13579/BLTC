package at.rayman.runafterdependency;

import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.ui.SettingsEditorFragment;
import com.intellij.execution.ui.SettingsEditorFragmentType;
import com.intellij.openapi.ui.LabeledComponent;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class TextTriggerFragment<T extends RunConfigurationBase<?>> extends SettingsEditorFragment<T, JPanel> {

    public TextTriggerFragment() {
        super("BeforeLaunchTextTrigger", "Trigger text", "Dependent Before Launch",
                createComponent(),
                -2,
                SettingsEditorFragmentType.BEFORE_RUN,
                (config, panel) -> {
                },
                (config, panel) -> {
                },
                (config) -> config.getBeforeRunTasks().stream()
                        .anyMatch(t -> t instanceof TextTriggerBeforeRunProvider.RunConfigurableBeforeRunTask)
        );
        setHint("Text listened for in output of Dependent Before Launch Task to execute this run configuration");
        setActionHint("Automatically shown when Dependent Before Launch Task is added");
    }

    @Override
    protected void resetEditorFrom(@NotNull T config) {
        config.getBeforeRunTasks().stream()
                .filter(t -> t instanceof TextTriggerBeforeRunProvider.RunConfigurableBeforeRunTask)
                .map(t -> (TextTriggerBeforeRunProvider.RunConfigurableBeforeRunTask) t)
                .forEach(task -> getTextField().setText(task.getTriggerText()));
    }

    @Override
    protected void applyEditorTo(@NotNull T config) {
        setSelected(config.getBeforeRunTasks().stream().anyMatch(t -> t instanceof TextTriggerBeforeRunProvider.RunConfigurableBeforeRunTask));
        config.getBeforeRunTasks().stream()
                .filter(t -> t instanceof TextTriggerBeforeRunProvider.RunConfigurableBeforeRunTask)
                .map(t -> (TextTriggerBeforeRunProvider.RunConfigurableBeforeRunTask) t)
                .forEach(task -> task.setTriggerText(getTextField().getText()));
    }

    private JTextField getTextField() {
        return (JTextField) component().getClientProperty("textField");
    }

    private static JPanel createComponent() {
        JTextField textField = new JTextField();
        JPanel panel = LabeledComponent.create(textField, "Trigger text:", BorderLayout.WEST);
        panel.putClientProperty("textField", textField);
        return panel;
    }

}
