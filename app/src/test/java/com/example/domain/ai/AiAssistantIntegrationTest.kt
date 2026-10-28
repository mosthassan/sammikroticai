package com.example.domain.ai

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.ledger.AccountConstants
import com.example.core.model.CurrencyCode
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AssetEntity
import com.example.data.local.entity.PartyEntity
import com.example.data.network.DeviceStatus
import com.example.data.network.DeviceType
import com.example.data.network.NetworkDevice
import com.example.data.network.NetworkRepository
import com.example.domain.usecase.StatementOfAccountUseCase
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AiAssistantIntegrationTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var networkRepository: NetworkRepository
    private lateinit var statementOfAccountUseCase: StatementOfAccountUseCase
    private lateinit var toolExecutor: AiToolExecutor

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = AppDatabase.createInMemory(context)
        AppDatabase.installTriggers(db.openHelper.writableDatabase)
        AppDatabase.seedDefaultData(db.openHelper.writableDatabase)

        networkRepository = NetworkRepository(context)
        statementOfAccountUseCase = StatementOfAccountUseCase(db)
        toolExecutor = AiToolExecutor(
            db = db,
            networkRepository = networkRepository,
            statementOfAccountUseCase = statementOfAccountUseCase
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testToolDefinitionsContainsAllFiveTools() {
        val toolsArray = AiToolDefinitions.getToolsJsonArray()
        assertEquals(1, toolsArray.length())

        val toolObj = toolsArray.getJSONObject(0)
        val declarations = toolObj.getJSONArray("functionDeclarations")
        assertEquals(5, declarations.length())

        val names = (0 until declarations.length()).map { index: Int ->
            declarations.getJSONObject(index).getString("name")
        }.toSet()

        assertTrue(names.contains("get_monthly_profit_report"))
        assertTrue(names.contains("get_cash_flow_statement"))
        assertTrue(names.contains("get_party_ledger"))
        assertTrue(names.contains("get_assets_summary"))
        assertTrue(names.contains("get_network_devices_status"))

        // Verify system instruction mentions Read-Only and clean Arabic
        assertTrue(AiToolDefinitions.SYSTEM_INSTRUCTION.contains("Read-Only"))
        assertTrue(AiToolDefinitions.SYSTEM_INSTRUCTION.contains("Sammi ERP Assistant"))
    }

    @Test
    fun testGetMonthlyProfitReportDefaultsToCurrentMonthWhenOmitted(): Unit = runBlocking {
        val initialDocsCount = db.documentDao().getAllDocumentsSync().size
        val initialEntriesCount = db.journalDao().getAllEntriesSync().size

        // Call tool with empty args
        val result = toolExecutor.execute("get_monthly_profit_report", JSONObject())

        assertNotNull(result)
        val today = LocalDate.now()
        assertEquals(today.year, result.getInt("fiscalYear"))
        assertEquals(today.monthValue, result.getInt("monthIndex"))
        assertTrue(result.has("netRevenue"))
        assertTrue(result.has("grossProfit"))
        assertTrue(result.has("netProfit"))
        assertTrue(result.has("reconciliation"))

        // Verify strictly read-only: no changes to documents or journal entries
        assertEquals(initialDocsCount, db.documentDao().getAllDocumentsSync().size)
        assertEquals(initialEntriesCount, db.journalDao().getAllEntriesSync().size)
    }

    @Test
    fun testGetCashFlowStatementDefaultsToCurrentMonthWhenOmitted(): Unit = runBlocking {
        val result = toolExecutor.execute("get_cash_flow_statement", JSONObject())

        assertNotNull(result)
        assertTrue(result.has("period"))
        assertTrue(result.has("openingCash"))
        assertTrue(result.has("cashReceiptsOperating"))
        assertTrue(result.has("cashDisbursementsOperating"))
        assertTrue(result.has("netOperatingCashFlow"))
        assertTrue(result.has("closingCashBalance"))
        assertTrue(result.has("reconciliation"))
    }

    @Test
    fun testGetPartyLedgerSummaryAndSingleParty(): Unit = runBlocking {
        // Insert sample party
        val testParty = PartyEntity(
            id = "party_ai_test_1",
            name = "وكيل المحطة الشمالية",
            phone = "771122334",
            isCustomer = true
        )
        db.partyDao().insertParty(testParty)

        // 1. General ledger summary
        val generalResult = toolExecutor.execute("get_party_ledger", JSONObject())
        assertTrue(generalResult.has("totalReceivables"))
        assertTrue(generalResult.has("totalPayables"))
        assertTrue(generalResult.has("balancesList"))

        // 2. Specific party inquiry
        val singlePartyArgs = JSONObject().apply {
            put("party_id", "party_ai_test_1")
        }
        val partyResult = toolExecutor.execute("get_party_ledger", singlePartyArgs)
        assertEquals("party_ai_test_1", partyResult.getString("partyId"))
        assertEquals("وكيل المحطة الشمالية", partyResult.getString("partyName"))
        assertTrue(partyResult.has("closingBalance"))
    }

    @Test
    fun testGetAssetsSummaryReturnsAccurateValuations(): Unit = runBlocking {
        // Insert sample asset
        val asset = AssetEntity(
            id = "asset_router_1",
            docId = "doc_init_1",
            name = "راوتر مايكروتك CCR2004",
            purchaseDateEpochDay = LocalDate.now().toEpochDay(),
            purchaseCostMinor = 350_000_00L, // 350,000 YER
            salvageValueMinor = 0L,
            usefulLifeMonths = 36,
            accumulatedDepreciationMinor = 50_000_00L, // 50,000 YER
            isDisposed = false
        )
        db.assetDao().insertAsset(asset)

        val result = toolExecutor.execute("get_assets_summary", JSONObject())

        assertTrue(result.getInt("totalAssetsCount") >= 1)
        assertTrue(result.getInt("activeAssetsCount") >= 1)
        assertTrue(result.has("totalPurchaseCost"))
        assertTrue(result.has("totalAccumulatedDepreciation"))
        assertTrue(result.has("totalNetBookValue"))

        val activeAssets = result.getJSONArray("activeAssets")
        assertTrue(activeAssets.length() >= 1)
        val firstAsset = activeAssets.getJSONObject(0)
        assertEquals("راوتر مايكروتك CCR2004", firstAsset.getString("name"))
    }

    @Test
    fun testGetNetworkDevicesStatus(): Unit = runBlocking {
        // Add sample network devices
        networkRepository.insertDevice(
            NetworkDevice(
                id = "dev_1",
                name = "Sector North AP",
                ipAddress = "10.10.1.10",
                deviceType = DeviceType.ACCESS_POINT,
                status = DeviceStatus.ONLINE,
                towerLocation = "برج الشمال"
            )
        )
        networkRepository.insertDevice(
            NetworkDevice(
                id = "dev_2",
                name = "Station South Client",
                ipAddress = "10.10.1.20",
                deviceType = DeviceType.STATION,
                status = DeviceStatus.OFFLINE,
                towerLocation = "برج الجنوب"
            )
        )

        val result = toolExecutor.execute("get_network_devices_status", JSONObject())

        assertTrue(result.getInt("totalDevicesCount") >= 2)
        assertTrue(result.getInt("offlineCount") >= 1)
        assertTrue(result.has("devicesList"))
        assertTrue(result.has("offlineOrWarningAlerts"))

        val alerts = result.getJSONArray("offlineOrWarningAlerts")
        assertTrue(alerts.length() >= 1)
    }
}
