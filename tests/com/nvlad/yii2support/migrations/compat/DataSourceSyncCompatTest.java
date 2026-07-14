package com.nvlad.yii2support.migrations.compat;

import com.intellij.database.util.LoaderContext;
import junit.framework.TestCase;
import kotlin.coroutines.Continuation;

import java.lang.reflect.Method;

public class DataSourceSyncCompatTest extends TestCase {
    public void testPrefersCoroutineApiWhenBothVersionsArePresent() {
        Method method = DataSourceSyncCompat.findSyncMethod(BothApis.class.getMethods());

        assertNotNull(method);
        assertEquals("tryPerformSync", method.getName());
    }

    public void testFallsBackTo2024AsyncApi() {
        Method method = DataSourceSyncCompat.findSyncMethod(LegacyApi.class.getMethods());

        assertNotNull(method);
        assertEquals("tryPerform", method.getName());
    }

    public static class LegacyApi {
        public Object tryPerform(LoaderContext context, boolean stopRunning, boolean merge) {
            return null;
        }
    }

    public static class BothApis extends LegacyApi {
        public Object tryPerformSync(
                LoaderContext context,
                boolean stopRunning,
                boolean merge,
                Object executor,
                Continuation<Object> continuation
        ) {
            return null;
        }
    }
}
