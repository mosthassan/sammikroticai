package com.example.domain.ai

import org.json.JSONArray
import org.json.JSONObject

/**
 * Tool definitions and schemas for the Gemini API Function Calling mechanism.
 * All tools are strictly Read-Only and provide financial, accounting, and technical telemetry.
 */
object AiToolDefinitions {

    val SYSTEM_INSTRUCTION = """
        You are 'Sammi ERP Assistant', an integrated Business Intelligence Agent for the SamMikrotik ISP management system. Your sole purpose is to convert natural language queries into read-only tool calls and present the accurate accounting and technical metrics to the user in clean, professional Arabic.

        Rules:
        1. NEVER invent, fabricate, calculate, or hallucinate financial numbers, balances, or device counts manually. Strictly use the data returned by the available functions.
        2. All operations are strictly Read-Only.
        3. When the user asks a question, call the relevant function(s) first.
        4. Format output using clear Markdown tables for comparative data, bold text for key numbers, and bullet points for summaries.
        5. If dates are omitted, query the current active month by default.
        6. Always present monetary amounts clearly in Yemeni Rials (YER) or their original currencies as returned by the tools.
    """.trimIndent()

    fun getToolsJsonArray(): JSONArray {
        val functionDeclarations = JSONArray()

        // 1. get_monthly_profit_report
        functionDeclarations.put(
            JSONObject().apply {
                put("name", "get_monthly_profit_report")
                put(
                    "description",
                    "Returns total sales, expenses, gross margin, and net profit for a given month and fiscal year. If month or year are omitted or 0, defaults to the active current month."
                )
                put(
                    "parameters",
                    JSONObject().apply {
                        put("type", "OBJECT")
                        put(
                            "properties",
                            JSONObject().apply {
                                put(
                                    "month",
                                    JSONObject().apply {
                                        put("type", "INTEGER")
                                        put("description", "Month number from 1 to 12. If omitted, defaults to active current month.")
                                    }
                                )
                                put(
                                    "year",
                                    JSONObject().apply {
                                        put("type", "INTEGER")
                                        put("description", "Fiscal year (e.g. 2026). If omitted, defaults to current fiscal year.")
                                    }
                                )
                            }
                        )
                    }
                )
            }
        )

        // 2. get_cash_flow_statement
        functionDeclarations.put(
            JSONObject().apply {
                put("name", "get_cash_flow_statement")
                put(
                    "description",
                    "Returns cash receipts, payouts, and cash box balance for a date range (YYYY-MM-DD). If dates are omitted, defaults to the current month."
                )
                put(
                    "parameters",
                    JSONObject().apply {
                        put("type", "OBJECT")
                        put(
                            "properties",
                            JSONObject().apply {
                                put(
                                    "start_date",
                                    JSONObject().apply {
                                        put("type", "STRING")
                                        put("description", "Start date in format YYYY-MM-DD. If omitted, defaults to start of current month.")
                                    }
                                )
                                put(
                                    "end_date",
                                    JSONObject().apply {
                                        put("type", "STRING")
                                        put("description", "End date in format YYYY-MM-DD. If omitted, defaults to end of current month.")
                                    }
                                )
                            }
                        )
                    }
                )
            }
        )

        // 3. get_party_ledger
        functionDeclarations.put(
            JSONObject().apply {
                put("name", "get_party_ledger")
                put(
                    "description",
                    "Returns customer and supplier outstanding balances (آجل), outstanding receivables, payables, and ledger items. Can filter by party_id for a single entity or by status ('receivables' for customers, 'payables' for suppliers, or 'all')."
                )
                put(
                    "parameters",
                    JSONObject().apply {
                        put("type", "OBJECT")
                        put(
                            "properties",
                            JSONObject().apply {
                                put(
                                    "party_id",
                                    JSONObject().apply {
                                        put("type", "STRING")
                                        put("description", "Optional specific party ID to get detailed account statement.")
                                    }
                                )
                                put(
                                    "status",
                                    JSONObject().apply {
                                        put("type", "STRING")
                                        put("description", "Filter by category: 'receivables' (عملاء ووكلاء), 'payables' (موردين), or 'all'.")
                                    }
                                )
                            }
                        )
                    }
                )
            }
        )

        // 4. get_assets_summary
        functionDeclarations.put(
            JSONObject().apply {
                put("name", "get_assets_summary")
                put(
                    "description",
                    "Returns fixed assets summary including total asset counts, active asset valuations, accumulated depreciation, and net book value."
                )
                put(
                    "parameters",
                    JSONObject().apply {
                        put("type", "OBJECT")
                        put("properties", JSONObject())
                    }
                )
            }
        )

        // 5. get_network_devices_status
        functionDeclarations.put(
            JSONObject().apply {
                put("name", "get_network_devices_status")
                put(
                    "description",
                    "Returns MikroTik and network device counts, online/offline status, location breakdown, and warning/offline alerts."
                )
                put(
                    "parameters",
                    JSONObject().apply {
                        put("type", "OBJECT")
                        put("properties", JSONObject())
                    }
                )
            }
        )

        val tools = JSONArray()
        tools.put(JSONObject().apply {
            put("functionDeclarations", functionDeclarations)
        })
        return tools
    }

    /**
     * Converts tools to the standard OpenAI-compatible format:
     * [{ "type": "function", "function": { "name": "...", "description": "...", "parameters": { ... } } }]
     */
    fun getOpenAiToolsJsonArray(): JSONArray {
        val geminiTools = getToolsJsonArray().getJSONObject(0).getJSONArray("functionDeclarations")
        val openAiTools = JSONArray()
        for (i in 0 until geminiTools.length()) {
            val fn = geminiTools.getJSONObject(i)
            val name = fn.getString("name")
            val desc = fn.getString("description")
            val params = fn.getJSONObject("parameters")

            // Adjust parameter casing for OpenAI standard (object, string, integer)
            val paramsStr = params.toString()
                .replace("\"OBJECT\"", "\"object\"")
                .replace("\"STRING\"", "\"string\"")
                .replace("\"INTEGER\"", "\"integer\"")

            val openAiTool = JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", name)
                    put("description", desc)
                    put("parameters", JSONObject(paramsStr))
                })
            }
            openAiTools.put(openAiTool)
        }
        return openAiTools
    }
}
