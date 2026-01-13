package at.rayman.runafterdependency;

import com.intellij.execution.configurations.RunConfigurationOptions;
import com.intellij.openapi.components.StoredProperty;

public class DependentRunConfigurationOptions extends RunConfigurationOptions {

    private final StoredProperty<String> triggerText = string("").provideDelegate(this, "triggerText");

    public String getTriggerText() {
        return triggerText.getValue(this);
    }

    public void setTriggerText(String value) {
        triggerText.setValue(this, value);
    }

}
