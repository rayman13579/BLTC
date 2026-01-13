package at.rayman.runafterdependency;

import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.NotNullLazyValue;

public class DependentRunConfigurationType extends ConfigurationTypeBase {

    protected static final String ID = "DependentRunConfiguration";

    public DependentRunConfigurationType() {
        super(ID, "Dependent", "Run configuration that can be triggered after another one",
                NotNullLazyValue.createValue(() -> AllIcons.Nodes.Console));
        addFactory(new DependentConfigurationFactory(this));
    }

}
