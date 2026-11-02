package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppDatabaseSeedTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = AppDatabase.createInMemory(context)
        AppDatabase.installTriggers(db.openHelper.writableDatabase)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun seedCreatesDefaultOrganizationOnFreshDatabase() = runBlocking {
        val org = db.organizationDao().getOrganizationSync()
        assertNotNull("Default organization must be seeded", org)
        assertEquals("DEFAULT_ORG", org!!.id)
        assertEquals("SANAA", org.primaryRateZone)
        assertEquals("Asia/Aden", org.timeZone)
        assertEquals(false, org.allowNegativeStock)
        assertEquals(1, org.fiscalYearStartMonth)
    }
}
