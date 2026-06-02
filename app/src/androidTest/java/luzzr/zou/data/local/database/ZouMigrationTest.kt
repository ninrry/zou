package luzzr.zou.data.local.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ZouMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ZouDatabase::class.java,
    )

    @Before
    fun setUp() {
        deleteTestDatabase()
    }

    @After
    fun tearDown() {
        deleteTestDatabase()
    }

    @Test
    fun migratesFrom4To5PreservingNotesAndAddingPinDefaults() {
        helper.createDatabase(TEST_DATABASE, 4).apply {
            execSQL(
                """
                INSERT INTO notes (
                    id,
                    title,
                    contentMarkdown,
                    previewText,
                    createdAt,
                    updatedAt,
                    lastOpenedAt,
                    isDeleted,
                    deletedAt,
                    tags,
                    archived
                ) VALUES (
                    'legacy-note',
                    '旧版笔记',
                    '旧版正文',
                    '旧版预览',
                    100,
                    200,
                    NULL,
                    0,
                    NULL,
                    NULL,
                    0
                )
                """.trimIndent(),
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(
            TEST_DATABASE,
            5,
            true,
            ZouMigrations.MIGRATION_4_5,
        )
        helper.closeWhenFinished(migratedDb)

        migratedDb.query(
            "SELECT title, contentMarkdown, isPinned, pinnedAt FROM notes WHERE id = 'legacy-note'",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("旧版笔记", cursor.getString(0))
            assertEquals("旧版正文", cursor.getString(1))
            assertEquals(0, cursor.getInt(2))
            assertTrue(cursor.isNull(3))
        }
    }

    private fun deleteTestDatabase() {
        InstrumentationRegistry.getInstrumentation()
            .targetContext
            .deleteDatabase(TEST_DATABASE)
    }

    private companion object {
        const val TEST_DATABASE = "zou-migration-test"
    }
}
