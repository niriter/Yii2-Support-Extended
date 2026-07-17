package com.nvlad.yii2support.migrations.actions;

import junit.framework.TestCase;

public class MigrateBaseActionTest extends TestCase {
    public void testPrependsProjectRootToShortRelativePaths() {
        assertEquals("/project/", MigrateBaseAction.preparePath("", "/project/"));
        assertEquals("/project/m", MigrateBaseAction.preparePath("m", "/project/"));
    }

    public void testPreservesAbsolutePaths() {
        assertEquals("@app/migrations", MigrateBaseAction.preparePath("@app/migrations", "/project/"));
        assertEquals("/var/migrations", MigrateBaseAction.preparePath("/var/migrations", "/project/"));
        assertEquals("C:\\migrations", MigrateBaseAction.preparePath("C:\\migrations", "/project/"));
    }

    public void testPrependsProjectRootToRelativePath() {
        assertEquals(
                "/project/migrations",
                MigrateBaseAction.preparePath("migrations", "/project/")
        );
    }
}
