package at.rayman.bltc;

import com.intellij.execution.*;
import com.intellij.execution.compound.ConfigurationSelectionUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.BeforeRunTaskAwareConfiguration;
import com.intellij.execution.impl.RunConfigurationBeforeRunProviderDelegate;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.icons.AllIcons;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Ref;
import com.intellij.util.concurrency.Semaphore;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;

import javax.swing.*;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;

public final class TextConditionBeforeRunProvider
        extends BeforeRunTaskProvider<TextConditionBeforeRunProvider.TextConditionBeforeRunTask>
        implements DumbAware {

    public static final Key<TextConditionBeforeRunTask> ID = Key.create("TextConditionBeforeRunTask");

    private static final String TRIGGER_CONDITION_ERROR = "Trigger Condition not fulfilled, cancelling execution";

    private static final Notification TEXT_CONDITION_NOTIFICATION = NotificationGroupManager.getInstance()
            .getNotificationGroup("TextConditionBeforeRun")
            .createNotification("Before launch condition", TRIGGER_CONDITION_ERROR, NotificationType.WARNING)
            .setIcon(AllIcons.RunConfigurations.TestState.Yellow2);

    private static final Notification BEFORE_LAUNCH_NOT_FOUND_NOTIFICATION = NotificationGroupManager.getInstance()
            .getNotificationGroup("TextConditionBeforeRun")
            .createNotification("Before launch condition", "Before launch configuration not found", NotificationType.WARNING)
            .setIcon(AllIcons.RunConfigurations.TestState.Yellow2);

    private final Project project;

    public TextConditionBeforeRunProvider(Project project) {
        this.project = project;
    }

    @Override
    public Key<TextConditionBeforeRunTask> getId() {
        return ID;
    }

    @Override
    public Icon getIcon() {
        return AllIcons.Actions.Execute;
    }

    @Override
    public Icon getTaskIcon(TextConditionBeforeRunTask task) {
        RunnerAndConfigurationSettings settings = task.getSettings();
        return settings == null ? AllIcons.General.Error : ProgramRunnerUtil.getConfigurationIcon(settings, false);
    }

    @Override
    public String getName() {
        return "Text Condition Configuration";
    }

    @Override
    public String getDescription(TextConditionBeforeRunTask task) {
        RunnerAndConfigurationSettings settings = task.getSettings();
        if (settings == null) {
            return "Configuration not found";
        }
        String text = ConfigurationSelectionUtil.getDisplayText(settings.getConfiguration(), task.getTarget());
        return "Run " + text;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public @NotNull TextConditionBeforeRunProvider.TextConditionBeforeRunTask createTask(@NotNull RunConfiguration runConfiguration) {
        return new TextConditionBeforeRunTask();
    }

    @Override
    public Promise<Boolean> configureTask(@NotNull DataContext context, @NotNull RunConfiguration configuration, @NotNull TextConditionBeforeRunProvider.TextConditionBeforeRunTask task) {
        Project project = configuration.getProject();
        RunManagerImpl runManager = RunManagerImpl.getInstanceImpl(project);

        List<RunConfiguration> configurations = runManager.getAllSettings().stream()
                .map(RunnerAndConfigurationSettings::getConfiguration)
                .filter(config -> configuration != config)
                .toList();

        AsyncPromise<Boolean> result = new AsyncPromise<>();
        ConfigurationSelectionUtil.createPopup(project, runManager, configurations, (selectedConfigs, selectedTarget) -> {
            RunnerAndConfigurationSettings selectedSettings = runManager.getSettings(selectedConfigs.get(0));
            task.setSettings(selectedSettings);
            task.setTarget(selectedTarget);
            result.setResult(true);
        }).showInBestPositionFor(context);

        return result;
    }

    @Override
    public boolean canExecuteTask(@NotNull RunConfiguration configuration, @NotNull TextConditionBeforeRunProvider.TextConditionBeforeRunTask task) {
        RunnerAndConfigurationSettings settings = task.getSettings();
        if (settings == null) {
            return false;
        }
        RunConfiguration runConfig = settings.getConfiguration();
        String executorId = DefaultRunExecutor.getRunExecutorInstance().getId();
        ProgramRunner<?> runner = ProgramRunner.getRunner(executorId, runConfig);
        return runner != null && runner.canRun(executorId, runConfig);
    }

    @Override
    public boolean executeTask(final @NotNull DataContext dataContext, @NotNull RunConfiguration configuration, final @NotNull ExecutionEnvironment env, @NotNull TextConditionBeforeRunProvider.TextConditionBeforeRunTask task) {
        RunnerAndConfigurationSettings settings = task.getSettings();
        if (settings == null) {
            BEFORE_LAUNCH_NOT_FOUND_NOTIFICATION.notify(project);
            return false;
        }
        RunConfiguration beforeRunConfiguration = settings.getConfiguration();
        Executor executor = beforeRunConfiguration instanceof BeforeRunTaskAwareConfiguration && ((BeforeRunTaskAwareConfiguration) beforeRunConfiguration).useRunExecutor() ? DefaultRunExecutor.getRunExecutorInstance() : env.getExecutor();
        final String executorId = executor.getId();
        ExecutionEnvironmentBuilder builder = ExecutionEnvironmentBuilder.createOrNull(executor, settings);
        if (builder == null) {
            return false;
        }

        ExecutionTarget effectiveTarget = getExecutionTarget(env, task.getTarget(), beforeRunConfiguration);
        if (effectiveTarget == null) {
            return false;
        }

        ExecutionEnvironment environment = builder.target(effectiveTarget).build();
        environment.setExecutionId(env.getExecutionId());
        env.copyUserDataTo(environment);

        if (!environment.getRunner().canRun(executorId, environment.getRunProfile())) {
            return false;
        } else {
            beforeRun(environment);
            return runTask(executorId, environment, environment.getRunner(), task);
        }
    }

    private ExecutionTarget getExecutionTarget(ExecutionEnvironment env, ExecutionTarget target, RunConfiguration configuration) {
        ExecutionTarget actualTarget = target;

        if (actualTarget == null && ExecutionTargetManager.canRun(configuration, env.getExecutionTarget())) {
            actualTarget = env.getExecutionTarget();
        }

        if (actualTarget == null) {
            List<ExecutionTarget> allTargets = ExecutionTargetManager.getInstance(env.getProject()).getTargetsFor(configuration);
            actualTarget = ContainerUtil.find(allTargets, ExecutionTarget::isReady);

            if (actualTarget == null) {
                actualTarget = ContainerUtil.getFirstItem(allTargets);
            }
        }
        return actualTarget;
    }

    private void beforeRun(ExecutionEnvironment environment) {
        for (RunConfigurationBeforeRunProviderDelegate delegate : RunConfigurationBeforeRunProviderDelegate.EP_NAME.getExtensionList()) {
            delegate.beforeRun(environment);
        }
    }

    public boolean runTask(String executorId, ExecutionEnvironment environment, ProgramRunner<?> runner, TextConditionBeforeRunTask task) {
        Semaphore conditionPassed = new Semaphore();
        Ref<Boolean> result = new Ref<>(false);
        Disposable disposable = Disposer.newDisposable();
        BiPredicate<String, String> triggerTextPredicate = textConditionPredicate(task.state.getTriggerCondition());

        ProcessListener outputListener = new ProcessListener() {
            @Override
            public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
                if (triggerTextPredicate.test(event.getText(), task.state.getTriggerText())) {
                    result.set(true);
                    conditionPassed.up();
                }
            }
        };

        environment.getProject().getMessageBus().connect(disposable).subscribe(ExecutionManager.EXECUTION_TOPIC, new ExecutionListener() {
            @Override
            public void processStartScheduled(final @NotNull String executorIdLocal, final @NotNull ExecutionEnvironment environmentLocal) {
                if (executorId.equals(executorIdLocal) && environment.equals(environmentLocal)) {
                    conditionPassed.down();
                }
            }

            @Override
            public void processStarting(@NotNull String executorIdLocal, @NotNull ExecutionEnvironment environmentLocal, @NotNull ProcessHandler handler) {
                if (executorId.equals(executorIdLocal) && environment.equals(environmentLocal)) {
                    handler.addProcessListener(outputListener);
                }
            }

            @Override
            public void processTerminating(@NotNull String executorIdLocal, @NotNull ExecutionEnvironment environmentLocal, @NotNull ProcessHandler handler) {
                if (executorId.equals(executorIdLocal) && environment.equals(environmentLocal)) {
                    result.set(false);
                    conditionPassed.up();

                    handler.notifyTextAvailable(TRIGGER_CONDITION_ERROR, ProcessOutputTypes.STDERR);
                    TEXT_CONDITION_NOTIFICATION.notify(environmentLocal.getProject());
                }
            }
        });

        try {
            ApplicationManager.getApplication().invokeAndWait(() -> {
                try {
                    runner.execute(environment);
                } catch (ExecutionException e) {
                    conditionPassed.up();
                }
            }, ModalityState.defaultModalityState());
        } catch (ProcessCanceledException e) {
            Disposer.dispose(disposable);
            throw e;
        } catch (Exception e) {
            Disposer.dispose(disposable);
            return false;
        }

        conditionPassed.waitFor();
        Disposer.dispose(disposable);
        return result.get();
    }

    private BiPredicate<String, String> textConditionPredicate(String triggerCondition) {
        return switch (triggerCondition) {
            case "exact" -> String::equals;
            case "contains" -> String::contains;
            case "startsWith" -> String::startsWith;
            case "endsWith" -> (text, triggerText) -> text.endsWith(triggerText) || text.trim().endsWith(triggerText);
            default -> (text, triggerText) -> false;
        };
    }

    public final class TextConditionBeforeRunTask extends BeforeRunTask<TextConditionBeforeRunTask> implements PersistentStateComponent<TextConditionState> {

        private TextConditionState state = new TextConditionState();
        private RunnerAndConfigurationSettings settings;
        private ExecutionTarget target;

        TextConditionBeforeRunTask() {
            super(ID);
        }

        @Override
        public TextConditionState getState() {
            return state;
        }

        @Override
        public void loadState(@NotNull TextConditionState state) {
            this.state = state;
        }

        public RunnerAndConfigurationSettings getSettings() {
            if (settings == null) {
                settings = RunManager.getInstance(project).findConfigurationByTypeAndName(state.getType(), state.getName());
            }
            return settings;
        }

        public void setSettings(RunnerAndConfigurationSettings settings) {
            this.settings = settings;
            state.setName(settings.getName());
            state.setType(settings.getType().getId());
        }

        public ExecutionTarget getTarget() {
            if (target == null) {
                target = ExecutionTargetManager.getInstance(project).findTarget(getSettings().getConfiguration());
            }
            return target;
        }

        public void setTarget(ExecutionTarget target) {
            this.target = target;
            if (target != null) {
                state.setTargetId(target.getId());
            }
        }

        @Override
        public BeforeRunTask clone() {
            TextConditionBeforeRunTask task = new TextConditionBeforeRunTask();
            task.state = this.state;
            task.settings = this.settings;
            task.target = this.target;
            return task;
        }

        @Override
        public String toString() {
            return "TriggerConditionBeforeRunTask{name = " + state.getName() + "}";
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            TextConditionBeforeRunTask that = (TextConditionBeforeRunTask) o;
            return Objects.equals(state, that.state)
                    && Objects.equals(settings, that.settings)
                    && Objects.equals(target, that.target);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), state, settings, target);
        }
    }

}