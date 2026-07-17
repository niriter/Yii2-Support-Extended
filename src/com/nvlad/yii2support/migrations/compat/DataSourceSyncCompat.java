package com.nvlad.yii2support.migrations.compat;

import com.intellij.database.dataSource.DataSourceSyncManager;
import com.intellij.database.dataSource.LocalDataSource;
import com.intellij.database.util.LoaderContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import kotlin.ResultKt;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class DataSourceSyncCompat {
    private static final Logger LOG = Logger.getInstance(DataSourceSyncCompat.class);

    private DataSourceSyncCompat() {
    }

    /**
     * Database Tools moved from the Java-friendly tryPerform API in 2024.1 to
     * the coroutine-based tryPerformSync API in 2025.1. Reflection is confined
     * here so the same plugin binary can use the current API on 2025.1+ while
     * retaining the supported 2024.1 fallback.
     */
    public static void synchronize(Project project, LocalDataSource dataSource) {
        LoaderContext context = LoaderContext.selectGeneralTask(project, dataSource);
        Method syncMethod = findSyncMethod(DataSourceSyncManager.class.getMethods());
        if (syncMethod == null) {
            LOG.error("No compatible Database Tools synchronization API is available");
            return;
        }

        try {
            DataSourceSyncManager manager = DataSourceSyncManager.getInstance();
            if (syncMethod.getParameterCount() == 5) {
                Object result = syncMethod.invoke(
                        manager,
                        context,
                        true,
                        false,
                        null,
                        new LoggingContinuation()
                );
                if (result != kotlin.coroutines.intrinsics.IntrinsicsKt.getCOROUTINE_SUSPENDED()) {
                    ResultKt.throwOnFailure(result);
                }
            } else {
                syncMethod.invoke(manager, context, true, false);
            }
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException exception) {
            Throwable cause = exception instanceof InvocationTargetException invocationException
                    && invocationException.getCause() != null
                    ? invocationException.getCause()
                    : exception;
            LOG.error("Unable to synchronize the database data source", cause);
        }
    }

    static Method findSyncMethod(Method[] methods) {
        Method legacyMethod = null;
        for (Method method : methods) {
            Class<?>[] parameters = method.getParameterTypes();
            if (method.getName().equals("tryPerformSync")
                    && parameters.length == 5
                    && parameters[0] == LoaderContext.class
                    && Continuation.class.isAssignableFrom(parameters[4])) {
                return method;
            }
            if (method.getName().equals("tryPerform")
                    && parameters.length == 3
                    && parameters[0] == LoaderContext.class
                    && parameters[1] == boolean.class
                    && parameters[2] == boolean.class) {
                legacyMethod = method;
            }
        }
        return legacyMethod;
    }

    private static final class LoggingContinuation implements Continuation<Object> {
        @NotNull
        @Override
        public CoroutineContext getContext() {
            return EmptyCoroutineContext.INSTANCE;
        }

        @Override
        public void resumeWith(@NotNull Object result) {
            try {
                ResultKt.throwOnFailure(result);
            } catch (Throwable throwable) {
                LOG.error("Database data source synchronization failed", throwable);
            }
        }
    }
}
