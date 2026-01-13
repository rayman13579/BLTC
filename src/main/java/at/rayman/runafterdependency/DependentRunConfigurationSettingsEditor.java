package at.rayman.runafterdependency;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.ui.TextFieldWithHistory;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class DependentRunConfigurationSettingsEditor extends SettingsEditor<DependentRunConfiguration> {

    private final JPanel panel;

    private final TextFieldWithHistory triggerTextField;

    public DependentRunConfigurationSettingsEditor() {
        triggerTextField = new TextFieldWithHistory();
        panel = FormBuilder.createFormBuilder()
                .addLabeledComponent("Trigger text:", triggerTextField)
                .getPanel();
    }

    @Override
    protected void resetEditorFrom(@NotNull DependentRunConfiguration runConfig) {
        triggerTextField.setText(runConfig.getTriggerText());
    }

    @Override
    protected void applyEditorTo(@NotNull DependentRunConfiguration runConfig) throws ConfigurationException {
        runConfig.setTriggerText(triggerTextField.getText());
    }

    @Override
    protected @NotNull JComponent createEditor() {
        return panel;
    }

}
