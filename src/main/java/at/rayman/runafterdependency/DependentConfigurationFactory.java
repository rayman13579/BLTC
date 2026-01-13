package at.rayman.runafterdependency;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.components.BaseState;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DependentConfigurationFactory extends ConfigurationFactory {

    protected DependentConfigurationFactory(ConfigurationType type) {
        super(type);
    }

    @Override
    public @NotNull String getId() {
        return DependentRunConfigurationType.ID;
    }

    @Override
    public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new DependentRunConfiguration(project, this, "Dependent");
    }

    @Override
    public @Nullable Class<? extends BaseState> getOptionsClass() {
        return DependentRunConfigurationOptions.class;
    }

}
