package at.rayman.runafterdependency;

import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.ui.SettingsEditorFragment;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class TextTriggerFragment<T extends RunConfigurationBase<?>> extends SettingsEditorFragment<T, JPanel> {

    private static final JTextField textField = new JTextField();

    public TextTriggerFragment() {
        super("BeforeLaunchTextTrigger", "Trigger Text", "Before Launch", createComponent(),
                (config, panel) -> {
                },
                (config, panel) -> {
                },
                (config) -> config.getBeforeRunTasks().stream()
                        .anyMatch(t -> t instanceof TextTriggerBeforeRunProvider.RunConfigurableBeforeRunTask)
        );
    }

    private static JPanel createComponent() {
        return FormBuilder.createFormBuilder()
                .addLabeledComponent("Trigger text:", textField)
                .getPanel();
    }

}
