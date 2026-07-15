package com.nvlad.yii2support.common;

import junit.framework.TestCase;

/**
 * Fast regression tests for Yii identifier conversion.  Keeping these tests
 * independent from the IDE fixture makes them useful as a first CI smoke test.
 */
public class StringUtilsTest extends TestCase {
    public void testUsesUnderscoreByDefault() {
        assertEquals("active_record", StringUtils.CamelToId("ActiveRecord"));
    }

    public void testSupportsYiiRouteSeparator() {
        assertEquals("user-profile", StringUtils.CamelToId("UserProfile", "-"));
    }

    public void testTrimsInputAndSplitsAfterDigits() {
        assertEquals("oauth2_client", StringUtils.CamelToId("  OAuth2Client  "));
    }

    public void testDoesNotSplitAnAcronymIntoIndividualLetters() {
        assertEquals("urlvalue", StringUtils.CamelToId("URLValue"));
    }
}
