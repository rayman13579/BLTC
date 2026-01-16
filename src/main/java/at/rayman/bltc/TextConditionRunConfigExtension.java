package at.rayman.bltc;


import com.intellij.execution.RunConfigurationExtension;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.openapi.options.SettingsEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TextConditionRunConfigExtension extends RunConfigurationExtension {

    @Override
    public <T extends RunConfigurationBase<?>> void updateJavaParameters(@NotNull T t, @NotNull JavaParameters javaParameters, @Nullable RunnerSettings runnerSettings) {
    }

    @Override
    protected <P extends RunConfigurationBase<?>> List<SettingsEditor<P>> createFragments(@NotNull P config) {
        return List.of(new TextConditionFragment<>());
    }

    @Override
    public boolean isApplicableFor(@NotNull RunConfigurationBase<?> config) {
        return true;
    }

}
