package com.hifzsniper.app

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hifzsniper.app.data.local.AppDatabase
import java.io.IOException
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @After
    fun cleanUp() {
        context.deleteDatabase(TEST_DB)
    }

    @Test
    @Throws(IOException::class)
    fun migrate2To3AddsReviewContextColumnsWithoutDataLoss() {
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL(
                """INSERT INTO learning_card(
                    id,type,surahId,ayahStart,ayahEnd,wordStart,wordEnd,prompt,answer,
                    createdAt,lastReviewedAt,nextReviewAt,intervalDays,stability,difficulty,
                    lapseCount,successCount,hesitationCount,isWeak,suspended,sourceCardId,
                    userNote,estimatedDurationSeconds,lastResult,algorithmVersion
                ) VALUES(
                    'ayah-1-1','AYAH',1,1,1,NULL,NULL,'Récite','Texte',
                    1,NULL,1,0,1,0.5,0,0,0,0,0,NULL,NULL,30,NULL,1
                )""".trimIndent()
            )
            close()
        }

        val sqlite = helper.runMigrationsAndValidate(TEST_DB, 3, true)
        try {
            sqlite.query(
                "SELECT promptMode, contextBeforeCount, contextAfterCount FROM learning_card WHERE id='ayah-1-1'"
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertTrue(cursor.getString(0) == "RECITATION")
                assertTrue(cursor.getInt(1) == 0)
                assertTrue(cursor.getInt(2) == 0)
            }
        } finally {
            sqlite.close()
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
