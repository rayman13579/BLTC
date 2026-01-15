package at.rayman.runafterdependency;


import com.intellij.execution.RunConfigurationExtension;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.openapi.options.SettingsEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TextTriggerRunConfigExtension extends RunConfigurationExtension {

    @Override
    public <T extends RunConfigurationBase<?>> void updateJavaParameters(@NotNull T t, @NotNull JavaParameters javaParameters, @Nullable RunnerSettings runnerSettings) {
    }

    @Override
    protected <P extends RunConfigurationBase<?>> List<SettingsEditor<P>> createFragments(@NotNull P config) {
        return List.of(new TextTriggerFragment<>());
    }

    @Override
    public boolean isApplicableFor(@NotNull RunConfigurationBase<?> config) {
        return config instanceof ApplicationConfiguration;
    }

}
