package com.example.ui.ai

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import com.example.domain.ai.AiProvider
import com.example.domain.ai.AiSettings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.BrandCyanPrimary
import com.example.ui.theme.SemanticExpenseRed
import com.example.ui.theme.SemanticIncomeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAssistantScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AIAssistantViewModel = viewModel()
) {
    BackHandler { onNavigateBack() }

    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val activeToolStatus by viewModel.activeToolStatus.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val connectionTestState by viewModel.connectionTestState.collectAsState()

    var inputPrompt by rememberSaveable { mutableStateOf("") }
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var showClearChatDialog by rememberSaveable { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Scroll to bottom when new messages arrive or loading begins
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Force Arabic RTL layout for natural reading
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = BrandCyanPrimary.copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = BrandCyanPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "مساعد سامي الذكي",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { showSettingsDialog = true }
                                ) {
                                    Text(
                                        text = "${settings.provider.displayName} • ${settings.model}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrandCyanPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("ai_assistant_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع"
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier.testTag("ai_assistant_settings_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "إعدادات محرك الذكاء الاصطناعي",
                                tint = if (settings.apiKey.isNotBlank()) BrandCyanPrimary else SemanticExpenseRed
                            )
                        }
                        IconButton(
                            onClick = { showClearChatDialog = true },
                            modifier = Modifier.testTag("ai_assistant_clear_chat_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "مسح المحادثة"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // Quick Suggestions Row
                    val suggestions = listOf(
                        "📊 أرباح هذا الشهر",
                        "💵 التدفقات النقدية للصندوق",
                        "👥 ديون العملاء والوكلاء",
                        "🏢 الأصول الثابتة والإهلاك",
                        "📡 حالة أجهزة الميكروتك",
                        "📉 مستحقات الموردين"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestions.forEach { suggestion ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val cleaned = suggestion.substringAfter(" ")
                                    val prompt = when {
                                        cleaned.contains("أرباح") -> "ما هي أرباح ومبيعات الشهر الحالي ونسبة الهامش؟"
                                        cleaned.contains("التدفقات") -> "ما هو موقف الصندوق وقائمة التدفقات النقدية للشهر الحالي؟"
                                        cleaned.contains("العملاء") -> "كم إجمالي ديون العملاء والوكلاء الآجلة ومن هم الأكثر مديونية؟"
                                        cleaned.contains("الأصول") -> "ما هو ملخص الأصول الثابتة وقيمتها ومجمع إهلاكها؟"
                                        cleaned.contains("الميكروتك") -> "ما هي حالة أجهزة وشبكة الميكروتك وهل توجد أجهزة متوقفة؟"
                                        cleaned.contains("الموردين") -> "ما هي مستحقات الموردين ومزودي الإنترنت الآجلة؟"
                                        else -> cleaned
                                    }
                                    viewModel.sendMessage(prompt)
                                },
                                label = { Text(suggestion, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }

                    // Input Text Field Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputPrompt,
                            onValueChange = { inputPrompt = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_assistant_input_field"),
                            placeholder = {
                                Text(
                                    text = "اسأل عن الأرباح، الصندوق، العملاء، أو الأجهزة...",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            },
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandCyanPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = if (inputPrompt.isNotBlank() && !isLoading) BrandCyanPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(48.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (inputPrompt.isNotBlank() && !isLoading) {
                                        val prompt = inputPrompt
                                        inputPrompt = ""
                                        viewModel.sendMessage(prompt)
                                    }
                                },
                                enabled = inputPrompt.isNotBlank() && !isLoading,
                                modifier = Modifier.testTag("ai_assistant_send_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "إرسال",
                                    tint = if (inputPrompt.isNotBlank() && !isLoading) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        ChatMessageItem(
                            message = message,
                            onRetry = { viewModel.retryLastMessage() }
                        )
                    }

                    if (isLoading) {
                        item {
                            ActiveLoadingIndicator(status = activeToolStatus)
                        }
                    }
                }
            }
        }

        if (showSettingsDialog) {
            AiSettingsDialog(
                currentSettings = settings,
                connectionTestState = connectionTestState,
                onTestConnection = { viewModel.testConnection(it) },
                onResetTestState = { viewModel.resetConnectionTestState() },
                onResetToDefaults = { viewModel.resetToDefaults() },
                onDismiss = {
                    viewModel.resetConnectionTestState()
                    showSettingsDialog = false
                },
                onSave = { newSettings ->
                    viewModel.saveSettings(newSettings)
                    viewModel.resetConnectionTestState()
                    showSettingsDialog = false
                }
            )
        }

        if (showClearChatDialog) {
            AlertDialog(
                onDismissRequest = { showClearChatDialog = false },
                title = { Text("مسح سجل المحادثة", fontWeight = FontWeight.Bold) },
                text = { Text("هل ترغب في مسح سجل المحادثة والبدء بجلسة جديدة مع المساعد الذكي؟") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearChat()
                            showClearChatDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SemanticExpenseRed)
                    ) {
                        Text("مسح", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearChatDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    onRetry: () -> Unit
) {
    val isUser = message.role == MessageRole.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.Start else Arrangement.End
    ) {
        if (!isUser) {
            Surface(
                shape = CircleShape,
                color = BrandCyanPrimary.copy(alpha = 0.12f),
                modifier = Modifier
                    .size(34.dp)
                    .padding(top = 4.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = BrandCyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 340.dp),
            horizontalAlignment = if (isUser) Alignment.Start else Alignment.End
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 4.dp else 16.dp,
                    bottomEnd = if (isUser) 16.dp else 4.dp
                ),
                color = if (isUser) {
                    BrandCyanPrimary
                } else if (message.status == MessageStatus.ERROR) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
                border = if (!isUser && message.status != MessageStatus.ERROR) {
                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                } else null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (message.toolExecutionNote != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BrandCyanPrimary.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = BrandCyanPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = message.toolExecutionNote,
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandCyanPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (isUser) {
                        Text(
                            text = message.content,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        MarkdownFormattedText(markdown = message.content)
                    }
                }
            }

            if (message.status == MessageStatus.ERROR) {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onRetry,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("ai_assistant_retry_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إعادة المحاولة", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ActiveLoadingIndicator(status: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            shape = CircleShape,
            color = BrandCyanPrimary.copy(alpha = 0.12f),
            modifier = Modifier.size(34.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = BrandCyanPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = BrandCyanPrimary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = status ?: "جاري التفكير وتحليل البيانات المحاسبية...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Parses and renders Markdown text into beautiful Compose components, supporting:
 * - Markdown Tables with headers and borders
 * - Bold text (**text**)
 * - Bullet lists (* or -)
 * - Numbered lists (1. 2.)
 * - Headers (# ## ###)
 */
@Composable
fun MarkdownFormattedText(markdown: String) {
    val lines = markdown.lines()
    var i = 0

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        while (i < lines.size) {
            val line = lines[i].trim()

            if (line.startsWith("|") && line.endsWith("|")) {
                // Table detection: collect all table lines
                val tableLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
                    tableLines.add(lines[i].trim())
                    i++
                }
                RenderMarkdownTable(tableLines)
                continue
            }

            when {
                line.startsWith("### ") -> {
                    Text(
                        text = line.removePrefix("### "),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = BrandCyanPrimary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        text = line.removePrefix("## "),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandCyanPrimary,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                line.startsWith("# ") -> {
                    Text(
                        text = line.removePrefix("# "),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = BrandCyanPrimary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                line.startsWith("* ") || line.startsWith("- ") -> {
                    val bulletContent = line.substring(2).trim()
                    Row(
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            color = BrandCyanPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = parseBoldMarkdown(bulletContent),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                line.matches(Regex("^\\d+\\.\\s+.*")) -> {
                    val number = line.substringBefore(".")
                    val content = line.substringAfter(".").trim()
                    Row(
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "$number.",
                            color = BrandCyanPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = parseBoldMarkdown(content),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                line.isNotBlank() -> {
                    Text(
                        text = parseBoldMarkdown(line),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                else -> {
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
            i++
        }
    }
}

@Composable
fun RenderMarkdownTable(tableLines: List<String>) {
    if (tableLines.size < 2) return

    val parsedRows = tableLines.map { row ->
        row.trim().removeSurrounding("|").split("|").map { it.trim() }
    }

    // Filter out separator rows like |---|---|
    val headerRow = parsedRows.firstOrNull() ?: return
    val contentRows = parsedRows.drop(1).filter { row ->
        !row.all { cell -> cell.matches(Regex("^:?-+:?$")) }
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrandCyanPrimary.copy(alpha = 0.15f))
                    .padding(vertical = 8.dp, horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                headerRow.forEachIndexed { index, title ->
                    Text(
                        text = parseBoldMarkdown(title),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandCyanPrimary,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Start
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Content Rows
            contentRows.forEachIndexed { index, rowCells ->
                val rowBg = if (index % 2 == 1) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                } else {
                    Color.Transparent
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(rowBg)
                        .padding(vertical = 7.dp, horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rowCells.forEachIndexed { colIndex, cellText ->
                        Text(
                            text = parseBoldMarkdown(cellText),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start
                        )
                    }
                }

                if (index < contentRows.size - 1) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

fun parseBoldMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val regex = Regex("\\*\\*(.*?)\\*\\*")
        val matches = regex.findAll(text)

        for (match in matches) {
            val start = match.range.first
            val end = match.range.last + 1
            if (start > cursor) {
                append(text.substring(cursor, start))
            }
            val boldContent = match.groupValues[1]
            val boldStart = length
            append(boldContent)
            addStyle(SpanStyle(fontWeight = FontWeight.Bold), boldStart, length)
            cursor = end
        }

        if (cursor < text.length) {
            append(text.substring(cursor))
        }
    }
}

@Composable
fun AiSettingsDialog(
    currentSettings: AiSettings,
    connectionTestState: ConnectionTestState,
    onTestConnection: (AiSettings) -> Unit,
    onResetTestState: () -> Unit,
    onResetToDefaults: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (AiSettings) -> Unit
) {
    var selectedProvider by rememberSaveable { mutableStateOf(currentSettings.provider) }
    var apiKeyInput by rememberSaveable { mutableStateOf(currentSettings.apiKey) }
    var modelInput by rememberSaveable { mutableStateOf(currentSettings.model) }
    var fallbackModelInput by rememberSaveable { mutableStateOf(currentSettings.fallbackModel) }
    var customBaseUrlInput by rememberSaveable { mutableStateOf(currentSettings.customBaseUrl) }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val currentWorkingSettings = remember(selectedProvider, apiKeyInput, modelInput, fallbackModelInput, customBaseUrlInput) {
        AiSettings(
            provider = selectedProvider,
            apiKey = apiKeyInput,
            model = modelInput,
            fallbackModel = fallbackModelInput,
            customBaseUrl = customBaseUrlInput
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = BrandCyanPrimary.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = BrandCyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "إعدادات محرك الذكاء الاصطناعي",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "تخصيص المزود والمفتاح والنماذج البديلة",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. API Provider Selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "مزود الخدمة (API Provider)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AiProvider.entries.forEach { prov ->
                            val isSelected = selectedProvider == prov
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (selectedProvider != prov) {
                                        selectedProvider = prov
                                        modelInput = prov.defaultModel
                                        fallbackModelInput = prov.defaultFallbackModel
                                        onResetTestState()
                                    }
                                },
                                label = {
                                    Text(
                                        text = prov.displayName,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = BrandCyanPrimary
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandCyanPrimary.copy(alpha = 0.15f),
                                    selectedLabelColor = BrandCyanPrimary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ai_provider_chip_${prov.id.lowercase()}")
                            )
                        }
                    }
                }

                // 2. Custom Base URL (shown when OpenAI-Compatible selected)
                if (selectedProvider == AiProvider.OPENAI_COMPATIBLE) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "عنوان الخادم الأساسي (Base URL)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = customBaseUrlInput,
                            onValueChange = {
                                customBaseUrlInput = it
                                onResetTestState()
                            },
                            placeholder = { Text(selectedProvider.defaultBaseUrl) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("api_custom_base_url_field"),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Text(
                            text = "يدعم OpenAI, Groq, OpenRouter, DeepSeek أو خوادم محلية (Ollama)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 3. API Key Field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "مفتاح الوصول (API Key)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            onResetTestState()
                        },
                        placeholder = {
                            Text(if (selectedProvider == AiProvider.GEMINI) "AIzaSy..." else "sk-...")
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "إخفاء" else "إظهار"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input_field"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // 4. Model Selection & Custom Input
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "النموذج الأساسي (Primary Model)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    // Quick-pick chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        selectedProvider.popularModels.forEach { popularModel ->
                            val isCurrent = modelInput == popularModel
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCurrent) BrandCyanPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        modelInput = popularModel
                                        onResetTestState()
                                    }
                            ) {
                                Text(
                                    text = popularModel,
                                    fontSize = 11.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = modelInput,
                        onValueChange = {
                            modelInput = it
                            onResetTestState()
                        },
                        placeholder = { Text(selectedProvider.defaultModel) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_model_input_field"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // 5. Fallback Model Input
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "النموذج البديل عند التوقف (Fallback Model on 429 / 404)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = fallbackModelInput,
                        onValueChange = {
                            fallbackModelInput = it
                            onResetTestState()
                        },
                        placeholder = { Text(selectedProvider.defaultFallbackModel) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_fallback_model_input_field"),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Text(
                        text = "يتحول المساعد إليه تلقائياً وبسلاسة في حال تعذر توفر النموذج الأساسي (HTTP 404) أو تجاوز حد الطلبات (HTTP 429).",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 6. Test Connection Button & Status Card
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onTestConnection(currentWorkingSettings) },
                        enabled = apiKeyInput.isNotBlank() && modelInput.isNotBlank() && connectionTestState !is ConnectionTestState.Loading,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_test_ai_connection")
                    ) {
                        if (connectionTestState is ConnectionTestState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = BrandCyanPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري اختبار الاتصال بالخادم...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("اختبار الاتصال بالخادم (Test Connection)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    when (connectionTestState) {
                        is ConnectionTestState.Success -> {
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = SemanticIncomeGreen.copy(alpha = 0.12f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = SemanticIncomeGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = connectionTestState.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SemanticIncomeGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        is ConnectionTestState.Error -> {
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = SemanticExpenseRed.copy(alpha = 0.12f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = SemanticExpenseRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = connectionTestState.errorMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SemanticExpenseRed,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        else -> Unit
                    }

                    // 7. Reset to Defaults Action
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(top = 4.dp))
                    OutlinedButton(
                        onClick = {
                            selectedProvider = AiProvider.GEMINI
                            modelInput = AiProvider.GEMINI.defaultModel
                            fallbackModelInput = AiProvider.GEMINI.defaultFallbackModel
                            customBaseUrlInput = ""
                            onResetTestState()
                            onResetToDefaults()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_reset_ai_defaults")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "إعادة الإعدادات الافتراضية (Reset to Defaults)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(currentWorkingSettings) },
                colors = ButtonDefaults.buttonColors(containerColor = BrandCyanPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_save_ai_settings")
            ) {
                Text("حفظ الإعدادات", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_cancel_ai_settings")
            ) {
                Text("إلغاء")
            }
        }
    )
}
