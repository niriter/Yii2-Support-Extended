package com.nvlad.yii2support;

import junit.framework.TestCase;

public class PluginGlobalSettingsTest extends TestCase {
    public void testVersionNotificationIsClaimedOnlyOnce() {
        PluginGlobalSettings settings = new PluginGlobalSettings();

        assertTrue(settings.markVersionNotified("1.0"));
        assertFalse(settings.markVersionNotified("1.0"));
        assertTrue(settings.markVersionNotified("1.1"));
        assertFalse(settings.markVersionNotified("1.1"));
    }
}
