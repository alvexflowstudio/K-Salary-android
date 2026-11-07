package com.koreasalary.calculator

import android.graphics.Bitmap
import android.view.WindowInsets
import android.view.inspector.WindowInspector
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb
import com.koreasalary.calculator.presentation.screens.calculator.CalendarCell
import com.koreasalary.calculator.presentation.theme.*
import java.time.DayOfWeek
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.filters.SdkSuppress
import com.koreasalary.calculator.data.local.entity.DayRecordEntity
import com.koreasalary.calculator.data.model.AppLanguage
import com.koreasalary.calculator.data.model.AppTheme
import com.koreasalary.calculator.data.model.*
import com.koreasalary.calculator.presentation.components.MonthlySummaryWidget
import com.koreasalary.calculator.presentation.components.SalarySummaryCard
import com.koreasalary.calculator.presentation.screens.settings.installedVersionLabel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.koreasalary.calculator.domain.i18n.LanguageManager
import com.koreasalary.calculator.presentation.components.DayEditDialog
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import androidx.test.core.app.ActivityScenario
import androidx.activity.compose.setContent
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.*
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.toSize
import org.junit.Test
import java.io.File
import java.time.YearMonth

@SdkSuppress(minSdkVersion = 30)
class DayEditDialogTest {
    private lateinit var scenario: ActivityScenario<ComponentActivity>
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    @Before fun launch() { scenario = ActivityScenario.launch(ComponentActivity::class.java) }
    @After fun close() { scenario.close() }

    private fun <T> onMain(block: () -> T): T {
        var result: T? = null
        instrumentation.runOnMainSync { result = block() }
        @Suppress("UNCHECKED_CAST") return result as T
    }
    private fun awaitCondition(message: String, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 5000
        while (!onMain(condition)) {
            check(SystemClock.uptimeMillis() < deadline) { message }
            SystemClock.sleep(30)
        }
    }
    private fun roots(): List<ViewRootForTest> {
        fun collect(view: View): List<ViewRootForTest> = buildList {
            if (view is ViewRootForTest) add(view)
            if (view is ViewGroup) for (i in 0 until view.childCount) addAll(collect(view.getChildAt(i)))
        }
        return WindowInspector.getGlobalWindowViews().flatMap(::collect).filter { it.view.width > 0 }
    }
    private fun nodes() = roots().flatMap { it.semanticsOwner.getAllSemanticsNodes(false) }
    private fun node(tag: String) = nodes().single { it.config.getOrNull(SemanticsProperties.TestTag) == tag }
    private fun absent(tag: String) = nodes().none { it.config.getOrNull(SemanticsProperties.TestTag) == tag }
    private fun rect(node: SemanticsNode) = Rect(node.positionInWindow, node.size.toSize())
    private fun scrollParent(node: SemanticsNode): SemanticsNode? = generateSequence(node.parent) { it.parent }
        .firstOrNull { it.config.contains(SemanticsActions.ScrollBy) }
    private fun visible(tag: String): Boolean {
        val n = node(tag)
        val bounds = rect(n)
        val viewport = scrollParent(n)?.boundsInWindow ?: return bounds.height > 0
        return bounds.height > 0 && bounds.top >= viewport.top - 1 && bounds.bottom <= viewport.bottom + 1
    }
    private fun scrollTo(tag: String) {
        awaitCondition("Cannot scroll to $tag") {
            if (visible(tag)) true else {
                val n = node(tag); val parent = scrollParent(n)!!
                val bounds = rect(n); val viewport = parent.boundsInWindow
                val delta = if (bounds.top < viewport.top) bounds.top - viewport.top else bounds.bottom - viewport.bottom
                parent.config[SemanticsActions.ScrollBy].action!!.invoke(0f, delta)
                false
            }
        }
    }
    private fun click(tag: String) {
        scrollTo(tag)
        instrumentation.waitForIdleSync()
        SystemClock.sleep(350) // let the scroll animation settle before converting coordinates
        val point = onMain {
            val n = node(tag)
            val root = roots().single { root -> root.semanticsOwner.getAllSemanticsNodes(false).any { it.id == n.id } }
            val origin = IntArray(2).also { root.view.rootView.getLocationOnScreen(it) }
            rect(n).center.let { it.copy(x = it.x + origin[0], y = it.y + origin[1]) }
        }
        android.os.ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand("input touchscreen tap ${point.x.toInt()} ${point.y.toInt()}")
        ).use { it.readBytes() }
        instrumentation.waitForIdleSync()
    }
    private fun setText(tag: String, text: String) {
        onMain { check(node(tag).config[SemanticsActions.SetText].action!!.invoke(AnnotatedString(text))) }
        awaitCondition("Text not updated in $tag") { node(tag).config[SemanticsProperties.EditableText].text == text }
    }
    private fun next(tag: String, target: String) {
        onMain { check(node(tag).config[SemanticsActions.OnImeAction].action!!.invoke()) }
        awaitCondition("Focus did not reach $target") { node(target).config.getOrNull(SemanticsProperties.Focused) == true && visible(target) }
    }
    private fun content(expectedTag: String = "hours-base", block: @Composable () -> Unit) {
        scenario.onActivity { it.setContent(content = block) }
        awaitCondition("Dialog missing") { !absent(expectedTag) }
    }
    private val month = YearMonth.of(2026, 11)
    private val strings = LanguageManager.getStringsFor(AppLanguage.RU)

    private fun keyboardVisible(): Boolean = WindowInspector.getGlobalWindowViews().any {
        it.rootWindowInsets?.isVisible(WindowInsets.Type.ime()) == true
    }
    private fun awaitKeyboard() {
        awaitCondition("Keyboard did not open") { keyboardVisible() }
    }
    private fun screenshot(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.getExternalFilesDir(null), "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
    // Public Compose semantics plus Android input taps avoid Espresso 3.6's
    // removed InputManager API on the installed Android 37 emulator.
    private fun open(day: Int = 1, existing: DayRecordEntity? = null, onSaved: (List<Double>) -> Unit = {}) {
        content {
            MaterialTheme {
                DayEditDialog(day, month, existing, strings, {},
                    { reg, ot, night, hol, holOt, _, _, _, _ -> onSaved(listOf(reg, ot, night, hol, holOt)) }, {})
            }
        }
    }

    @Test fun sundayWarningIsOnlyVisibleWhileBaseIsFocusedAndBaseStaysEditable() {
        open(existing = DayRecordEntity("2026-11-01", "2026-11", isSundayPay = true))
        assertTrue(onMain { absent("sunday-warning") })
        click("hours-base"); setText("hours-base", "8"); awaitKeyboard()
        awaitCondition("Sunday warning missing") { !absent("sunday-warning") }
        assertEquals(strings.ui.sundayBaseHoursWarning, onMain { node("sunday-warning").config[SemanticsProperties.Text].single().text })
        assertFalse(onMain { node("hours-base").config.contains(SemanticsProperties.Error) })
        // Wait for the keyboard animation before capturing review evidence.
        SystemClock.sleep(350)
        screenshot("sunday-base-keyboard")
        next("hours-base", "hours-combined")
        assertTrue(onMain { absent("sunday-warning") && keyboardVisible() })
        next("hours-combined", "hours-night")
        assertTrue(onMain { absent("sunday-warning") && keyboardVisible() })
        next("hours-night", "hours-double")
        assertTrue(onMain { absent("sunday-warning") && keyboardVisible() })
    }

    @Test fun mondayThroughSaturdayHaveNoBaseWarningEvenOnPaidHolidays() {
        var day by mutableIntStateOf(2)
        content {
            key(day) {
                MaterialTheme {
                    DayEditDialog(day, month, DayRecordEntity("2026-11-0$day", "2026-11", isPaidHoliday = true),
                        strings, {}, { _, _, _, _, _, _, _, _, _ -> }, {})
                }
            }
        }
        for (value in 2..7) {
            onMain { day = value }
            instrumentation.waitForIdleSync()
            click("hours-base"); setText("hours-base", "8")
            assertTrue(onMain { absent("sunday-warning") && !node("hours-base").config.contains(SemanticsProperties.Disabled) })
        }
    }

    @Test fun compactKeyboardAllowsNextScrollingFlagsNotesAndAllActions() {
        var saved: List<Double>? = null
        open(existing = DayRecordEntity("2026-11-01", "2026-11")) { saved = it }
        val fields = listOf("hours-base", "hours-combined", "hours-night", "hours-double")
        click(fields[0]); awaitKeyboard()
        fields.forEachIndexed { index, tag ->
            awaitCondition("Focused field obscured: $tag") { node(tag).config[SemanticsProperties.Focused] && visible(tag) }
            setText(tag, if (index == 0) "8" else "2")
            assertTrue("Keyboard stayed visible at $tag", onMain { keyboardVisible() })
            if (index < 3) next(tag, fields[index + 1])
        }
        for (tag in listOf("paid-holiday", "weekly-rest")) {
            click(tag)
            awaitCondition("Checkbox was not toggled: $tag") { node(tag).config[SemanticsProperties.ToggleableState] == ToggleableState.On }
            assertTrue(onMain { keyboardVisible() })
        }
        click("day-note")
        awaitCondition("Note was not focused") { node("day-note").config[SemanticsProperties.Focused] }
        setText("day-note", "test note"); awaitKeyboard()
        for (tag in listOf("clear-day", "cancel-day", "save-day")) {
            scrollTo(tag)
            assertTrue(onMain { visible(tag) && keyboardVisible() })
        }
        screenshot("actions-keyboard")
        click("save-day")
        awaitCondition("Save callback missing") { saved != null }
        assertEquals(listOf(8.0, 2.0, 2.0, 0.0, 2.0), onMain { saved })
    }

    @Test fun storedSplitSurvivesSaveAndInvalidBaseBlocksSave() {
        var saved: List<Double>? = null
        open(existing = DayRecordEntity("2026-11-01", "2026-11", overtimeHours = 2.5, holidayHours = 8.0)) { saved = it }
        assertEquals("10.5", onMain { node("hours-combined").config[SemanticsProperties.EditableText].text })
        click("hours-base"); setText("hours-base", "9")
        awaitCondition("Invalid base must disable Save") { node("save-day").config.contains(SemanticsProperties.Disabled) }
        click("hours-base"); setText("hours-base", "8")
        click("save-day")
        awaitCondition("Save callback missing") { saved != null }
        assertEquals(listOf(8.0, 2.5, 0.0, 8.0, 0.0), onMain { saved })
    }

    @Test fun allLocalizedLabelsFitOneLineOnCompactScreen() {
        var language by mutableStateOf(AppLanguage.RU)
        content {
            key(language) {
                MaterialTheme {
                    DayEditDialog(1, month, null, LanguageManager.getStringsFor(language), {},
                        { _, _, _, _, _, _, _, _, _ -> }, {})
                }
            }
        }
        AppLanguage.entries.forEach { current ->
            onMain { language = current }
            val ui = LanguageManager.getStringsFor(current).ui
            awaitCondition("Locale $current not rendered") { nodes().any { it.config.getOrNull(SemanticsProperties.Text)?.any { it.text == ui.dayBaseHoursLabel } == true } }
            listOf(ui.dayBaseHoursLabel, ui.dayOvertimeHoursLabel, ui.dayNightHoursLabel, ui.dayHolidayOvertimeHoursLabel).forEach { label ->
                val results = mutableListOf<TextLayoutResult>()
                onMain {
                    val n = nodes().single { it.config.getOrNull(SemanticsProperties.Text)?.any { it.text == label } == true }
                    n.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(results)
                }
                assertEquals(label, 1, results.single().lineCount)
                val layout = results.single()
                // Text uses integer pixel bounds; allow only subpixel rounding, not truncation.
                assertTrue("Clipped label in $current: $label right=${layout.getLineRight(0)} width=${layout.size.width}",
                    layout.getLineRight(0) <= layout.size.width + 1f)
                assertFalse(label, layout.didOverflowHeight)
            }
        }
    }
    @Test fun calendarBordersAndWorkedLabelsRenderInBothThemes() {
        var dark by mutableStateOf(false)
        val record = DayRecordEntity("2026-11-01", "2026-11")
        content("calendar-date-1") {
            KoreaSalaryTheme(appTheme = if (dark) AppTheme.DARK else AppTheme.LIGHT) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                    Row(Modifier.width(320.dp).statusBarsPadding()) {
                        CalendarCell(1, record.copy(regularHours = 8.0, nightHours = 8.0, isSundayPay = true), DayOfWeek.SUNDAY, strings, {}, Modifier.weight(1f).testTag("calendar-outer-1"))
                        CalendarCell(2, record.copy(holidayHours = 8.0, holidayOvertimeHours = 2.0, isPaidHoliday = true), DayOfWeek.MONDAY, strings, {}, Modifier.weight(1f).testTag("calendar-outer-2"))
                        CalendarCell(3, record.copy(overtimeHours = 2.5), DayOfWeek.SATURDAY, strings, {}, Modifier.weight(1f).testTag("calendar-outer-3"))
                        CalendarCell(4, record.copy(isPaidHoliday = true), DayOfWeek.TUESDAY, strings, {}, Modifier.weight(1f).testTag("calendar-outer-4"))
                        CalendarCell(5, null, DayOfWeek.WEDNESDAY, strings, {}, Modifier.weight(1f))
                    }
                }
            }
        }
        for (isDark in listOf(false, true)) {
            onMain { dark = isDark }
            instrumentation.waitForIdleSync()
            SystemClock.sleep(150)
            val expected = if (isDark) DarkAppPalette else LightAppPalette
            val bitmap = instrumentation.uiAutomation.takeScreenshot()
            try {
                for (day in 1..4) {
                    val point = onMain {
                        val n = node("calendar-outer-$day")
                        val root = roots().single { it.semanticsOwner.getAllSemanticsNodes(false).any { candidate -> candidate.id == n.id } }
                        val origin = IntArray(2).also { root.view.rootView.getLocationOnScreen(it) }
                        val bounds = rect(n)
                        Pair((bounds.center.x + origin[0]).toInt(), (bounds.top + origin[1]).toInt())
                    }
                    val color = (if (day in listOf(1, 2, 4)) expected.danger else LightAppPalette.accent).toArgb()
                    assertTrue("Outer $day border in dark=$isDark", (0..3).any { bitmap.getPixel(point.first, point.second + it) == color })
                }
            } finally { bitmap.recycle() }
            for (label in listOf("8 ч", "10 ч", "2.5 ч", strings.calendarPaidHolidayBadge)) {
                assertTrue(label, onMain { nodes().any { it.config.getOrNull(SemanticsProperties.Text)?.any { text -> text.text == label } == true } })
            }
            screenshot(if (isDark) "calendar-dark" else "calendar-light")
        }
    }

    @Test fun octoberOuterBordersAndStatusColorsInBothThemesAndAllLanguages() {
        var dark by mutableStateOf(false)
        var language by mutableStateOf(AppLanguage.RU)
        val october = YearMonth.of(2026, 10)
        fun record(day: Int) = DayRecordEntity(october.atDay(day).toString(), october.toString())
        val records = linkedMapOf(
            5 to record(5).copy(isPaidHoliday = true),
            9 to record(9).copy(isPaidHoliday = true),
            11 to record(11).copy(isSundayPay = true),
            25 to record(25).copy(regularHours = 8.0, overtimeHours = 2.0, nightHours = 4.0, isSundayPay = true),
            6 to record(6).copy(regularHours = 8.0),
            10 to record(10).copy(regularHours = 4.0),
            18 to null
        )
        content("calendar-cell-5") {
            KoreaSalaryTheme(appTheme = if (dark) AppTheme.DARK else AppTheme.LIGHT) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                    Row(Modifier.width(320.dp).statusBarsPadding()) {
                        records.forEach { (day, record) ->
                            CalendarCell(day, record, october.atDay(day).dayOfWeek,
                                LanguageManager.getStringsFor(language), {},
                                Modifier.weight(1f).testTag("calendar-cell-$day"))
                        }
                    }
                }
            }
        }
        fun screenBounds(tag: String) = onMain {
            val n = node(tag)
            val root = roots().single { it.semanticsOwner.getAllSemanticsNodes(false).any { candidate -> candidate.id == n.id } }
            val origin = IntArray(2).also { root.view.rootView.getLocationOnScreen(it) }
            rect(n).translate(origin[0].toFloat(), origin[1].toFloat())
        }
        fun textLayout(tag: String): TextLayoutResult = onMain {
            val results = mutableListOf<TextLayoutResult>()
            node(tag).config[SemanticsActions.GetTextLayoutResult].action!!.invoke(results)
            results.single()
        }
        for (isDark in listOf(false, true)) {
            onMain { dark = isDark }
            for (locale in AppLanguage.entries) {
                onMain { language = locale }
                val localized = LanguageManager.getStringsFor(locale)
                awaitCondition("Language not rendered: $locale") {
                    node("calendar-badge-5").config[SemanticsProperties.Text].single().text == localized.calendarPaidHolidayBadge
                }
                instrumentation.waitForIdleSync()
                SystemClock.sleep(100)
                val palette = if (isDark) DarkAppPalette else LightAppPalette
                val bitmap = instrumentation.uiAutomation.takeScreenshot()
                try {
                    records.keys.forEach { day ->
                        val red = day in listOf(5, 9, 11, 25, 18)
                        val expected = (if (red) palette.danger else LightAppPalette.accent).toArgb()
                        for (tag in listOf("calendar-cell-$day")) {
                            val bounds = screenBounds(tag)
                            if (day == 18) {
                                assertEquals("Empty Sunday $tag has a border", palette.card.toArgb(),
                                    bitmap.getPixel(bounds.center.x.toInt(), bounds.top.toInt()))
                            } else {
                                assertTrue("$tag outer/inner border dark=$isDark locale=$locale",
                                    (0..3).any { offset -> bitmap.getPixel(bounds.center.x.toInt(), bounds.top.toInt() + offset) == expected })
                            }
                        }
                        if (red) assertEquals("Date color $day", palette.danger, textLayout("calendar-date-$day").layoutInput.style.color)
                    }
                    for (day in listOf(5, 9, 11)) {
                        assertEquals("Status color $day in $locale", palette.danger, textLayout("calendar-badge-$day").layoutInput.style.color)
                    }
                    assertEquals("10 ${localized.hoursUnit}", onMain { node("calendar-badge-25").config[SemanticsProperties.Text].single().text })
                    assertEquals("8 ${localized.hoursUnit}", onMain { node("calendar-badge-6").config[SemanticsProperties.Text].single().text })
                    assertTrue("Untouched Sunday must have no badge", onMain { absent("calendar-badge-18") })
                } finally { bitmap.recycle() }
                if (locale == AppLanguage.RU) screenshot(if (isDark) "october-cell-dark" else "october-cell-light")
            }
        }
    }

    @Test fun emptyDaysHaveOnlyDatesAndPopulatedDaysRetainBordersInBothThemes() {
        var dark by mutableStateOf(false)
        var surface by mutableStateOf(androidx.compose.ui.graphics.Color.Unspecified)
        var textColor by mutableStateOf(androidx.compose.ui.graphics.Color.Unspecified)
        val october = YearMonth.of(2026, 10)
        fun record(day: Int) = DayRecordEntity(october.atDay(day).toString(), october.toString())
        val records = linkedMapOf(
            7 to null, // empty Wednesday
            17 to null, // empty Saturday
            18 to null, // empty Sunday
            19 to record(19), // blank stored row is still visually empty
            6 to record(6).copy(regularHours = 8.0),
            11 to record(11).copy(isSundayPay = true),
            9 to record(9).copy(isPaidHoliday = true)
        )
        content("calendar-empty-case-7") {
            KoreaSalaryTheme(appTheme = if (dark) AppTheme.DARK else AppTheme.LIGHT) {
                val currentSurface = MaterialTheme.colorScheme.surface
                val currentText = MaterialTheme.colorScheme.onSurface
                SideEffect { surface = currentSurface; textColor = currentText }
                Box(Modifier.fillMaxSize().background(currentSurface)) {
                    Row(Modifier.width(320.dp).statusBarsPadding()) {
                        records.forEach { (day, record) ->
                            CalendarCell(day, record, october.atDay(day).dayOfWeek, strings, {},
                                Modifier.weight(1f).testTag("calendar-empty-case-$day"))
                        }
                    }
                }
            }
        }
        fun bounds(tag: String) = onMain {
            val n = node(tag)
            val root = roots().single { it.semanticsOwner.getAllSemanticsNodes(false).any { candidate -> candidate.id == n.id } }
            val origin = IntArray(2).also { root.view.rootView.getLocationOnScreen(it) }
            rect(n).translate(origin[0].toFloat(), origin[1].toFloat())
        }
        fun layout(tag: String): TextLayoutResult = onMain {
            val result = mutableListOf<TextLayoutResult>()
            node(tag).config[SemanticsActions.GetTextLayoutResult].action!!.invoke(result)
            result.single()
        }
        for (isDark in listOf(false, true)) {
            onMain { dark = isDark }
            instrumentation.waitForIdleSync()
            SystemClock.sleep(150)
            val palette = if (isDark) DarkAppPalette else LightAppPalette
            val bitmap = instrumentation.uiAutomation.takeScreenshot()
            try {
                val reference = bounds("calendar-empty-case-6")
                records.keys.forEach { day ->
                    val cell = bounds("calendar-empty-case-$day")
                    assertEquals("Cell width $day", reference.width, cell.width, 1f)
                    assertEquals("Cell height $day", reference.height, cell.height, 1f)
                    assertEquals("Cell aspect ratio $day", 0.85f, cell.width / cell.height, 0.02f)
                    val date = bounds("calendar-date-$day")
                    val populated = day in listOf(6, 11, 9)
                    if (!populated) {
                        for (rect in listOf(cell, date)) {
                            // Check the entire straight top edge, not just absence of red.
                            val inset = if (rect == cell) 16 else 4
                            for (x in rect.left.toInt() + inset until rect.right.toInt() - inset) {
                                assertEquals("Empty $day has a border in dark=$isDark", surface.toArgb(), bitmap.getPixel(x, rect.top.toInt()))
                            }
                        }
                        assertTrue("Empty $day has a badge", onMain { absent("calendar-badge-$day") })
                        assertEquals(androidx.compose.ui.text.font.FontWeight.Medium, layout("calendar-date-$day").layoutInput.style.fontWeight)
                        val expectedText = when (day) {
                            17 -> LightAppPalette.accent
                            18 -> palette.danger
                            else -> textColor
                        }
                        assertEquals("Date text color $day", expectedText, layout("calendar-date-$day").layoutInput.style.color)
                    } else {
                        val expected = (if (day == 6) LightAppPalette.accent else palette.danger).toArgb()
                        for (rect in listOf(cell)) {
                            assertTrue("Populated $day lost border", (0..3).any { bitmap.getPixel(rect.center.x.toInt(), rect.top.toInt() + it) == expected })
                        }
                        // A solid straight edge here would be the removed inner date frame.
                        val dateTopColors=(date.left.toInt() until date.right.toInt()).count {
                            bitmap.getPixel(it,date.top.toInt())==expected
                        }
                        assertTrue("Date $day still has an inner frame",dateTopColors < date.width * 0.8f)
                        assertEquals(androidx.compose.ui.text.font.FontWeight.ExtraBold, layout("calendar-date-$day").layoutInput.style.fontWeight)
                    }
                }
                for (day in listOf(11, 9)) {
                    assertEquals(palette.danger, layout("calendar-badge-$day").layoutInput.style.color)
                }
                assertEquals("8 ч", onMain { node("calendar-badge-6").config[SemanticsProperties.Text].single().text })
            } finally { bitmap.recycle() }
            screenshot(if (isDark) "empty-days-dark" else "empty-days-light")
        }
    }

    @Test fun productionCalendarScreenCapturesAndDateSelection() {
        val args=InstrumentationRegistry.getArguments()
        val phase=args.getString("calendarPhase") ?: "after"
        val languages=listOf(AppLanguage.TH,AppLanguage.KO,AppLanguage.EN)
        var language by mutableStateOf(languages.first())
        var dark by mutableStateOf(false)
        val scope=kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
        val db=com.koreasalary.calculator.data.local.AppDatabase.getDatabase(instrumentation.targetContext,scope)
        val dao=db.dayRecordDao()
        val october=YearMonth.of(2026,10)
        val original=kotlinx.coroutines.runBlocking {dao.getAllRecords().filter {it.yearMonth==october.toString()}}
        fun record(day:Int)=DayRecordEntity(october.atDay(day).toString(),october.toString())
        val fixtures=listOf(record(1).copy(regularHours=8.0),record(2).copy(regularHours=8.0,overtimeHours=2.5,nightHours=4.0),
            record(3).copy(regularHours=8.0,overtimeHours=7.75,holidayHours=8.0),record(4).copy(regularHours=0.5),
            record(5).copy(isPaidHoliday=true),record(6).copy(isSundayPay=true),record(9).copy(isPaidHoliday=true),
            record(11).copy(regularHours=8.0,isSundayPay=true),record(12).copy(regularHours=8.0,overtimeHours=16.25,holidayHours=8.0,holidayOvertimeHours=16.0),
            record(25).copy(holidayHours=8.0,holidayOvertimeHours=2.5,isPaidHoliday=true))
        try {
            kotlinx.coroutines.runBlocking {original.forEach {dao.deleteByDate(it.date)};dao.insertOrUpdateAll(fixtures)}
            lateinit var vm:com.koreasalary.calculator.presentation.viewmodel.SalaryViewModel
            scenario.onActivity {
                vm=androidx.lifecycle.ViewModelProvider(it)[com.koreasalary.calculator.presentation.viewmodel.SalaryViewModel::class.java]
                vm.setYearMonth(october)
            }
            content("calendar-date-1") {
                key(language,dark) {
                    KoreaSalaryTheme(appTheme=if(dark) AppTheme.DARK else AppTheme.LIGHT) {
                        com.koreasalary.calculator.presentation.screens.calculator.CalculatorScreen(vm,LanguageManager.getStringsFor(language))
                    }
                }
            }
            awaitCondition("Calendar records missing") {vm.monthDayRecords.value.size==fixtures.size && !absent("calendar-badge-12")}
            for(mode in listOf(false,true)) for(lang in languages) {
                onMain {language=lang;dark=mode}
                awaitCondition("Actual calendar locale missing") {node("calendar-badge-5").config[SemanticsProperties.Text].single().text==LanguageManager.getStringsFor(lang).calendarPaidHolidayBadge}
                instrumentation.waitForIdleSync()
                onMain {
                    val n=node("calendar-date-1");val parent=scrollParent(n)!!
                    val density=instrumentation.targetContext.resources.displayMetrics.density
                    parent.config[SemanticsActions.ScrollBy].action!!.invoke(0f,rect(n).top-parent.boundsInWindow.top-70f*density)
                }
                SystemClock.sleep(300)
                assertTrue("Last calendar row not visible",onMain {visible("calendar-date-31")})
                val width=onMain {instrumentation.targetContext.resources.configuration.screenWidthDp}
                screenshot("calendar-screen-$phase-${lang.name}-$mode-${width}dp")
            }
            // Exercise the unchanged real date-selection path, not just a fixture callback.
            onMain {
                val clickable=generateSequence(node("calendar-date-11")) {it.parent}.first {it.config.contains(SemanticsActions.OnClick)}
                clickable.config[SemanticsActions.OnClick].action!!.invoke()
            }
            awaitCondition("Date selection did not open editor") {!absent("hours-base")}
            assertEquals("8",onMain {node("hours-base").config[SemanticsProperties.EditableText].text})
        } finally {
            kotlinx.coroutines.runBlocking {fixtures.forEach {dao.deleteByDate(it.date)};dao.insertOrUpdateAll(original)}
            scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
        }
    }

    @Test fun calendarCellMinimumRepro() {
        val strings=LanguageManager.getStringsFor(AppLanguage.KO)
        content("minimum-cell") {
            KoreaSalaryTheme(appTheme=AppTheme.LIGHT) {
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal=30.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    CalendarCell(12,DayRecordEntity("2026-10-12","2026-10",regularHours=8.0,overtimeHours=16.25,
                        holidayHours=8.0,holidayOvertimeHours=16.0),DayOfWeek.MONDAY,strings,{},Modifier.weight(1f).testTag("minimum-cell"))
                    repeat(6) {Spacer(Modifier.weight(1f))}
                }
            }
        }
        instrumentation.waitForIdleSync()
        onMain {
            val layouts=mutableListOf<TextLayoutResult>()
            node("calendar-badge-12").config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts)
            val l=layouts.single()
            assertTrue("48.25 hours at 34dp: font=${l.layoutInput.style.fontSize}, bounds=${rect(node("calendar-badge-12"))}",
                l.layoutInput.style.fontSize.value>=8.5f)
        }
    }

    // The production cell is measured at the production card padding and seven-column
    // width. Text's own overflow flag cannot detect a child extending below its cell.
    @Test fun calendarContentStaysInsideCellsAtReadableSize() {
        val args = InstrumentationRegistry.getArguments()
        val phase = args.getString("calendarPhase") ?: "after"
        val requested = args.getString("calendarLocales")?.split(",")
        val languages = AppLanguage.entries.filter { requested == null || it.name in requested }
        var language by mutableStateOf(languages.first())
        var dark by mutableStateOf(false)
        val october = YearMonth.of(2026, 10)
        fun record(day: Int) = DayRecordEntity(october.atDay(day).toString(), october.toString())
        val cases = linkedMapOf(
            1 to record(1).copy(regularHours=8.0),
            2 to record(2).copy(regularHours=8.0,overtimeHours=2.5,nightHours=4.0),
            3 to record(3).copy(regularHours=8.0,overtimeHours=7.75,holidayHours=8.0),
            4 to record(4).copy(regularHours=0.5),
            5 to record(5).copy(isPaidHoliday=true),
            6 to record(6).copy(isSundayPay=true),
            11 to record(11).copy(regularHours=8.0,isSundayPay=true),
            7 to null, 10 to null, 18 to null,
            8 to record(8).copy(note="Saved note"),
            9 to record(9).copy(isPaidHoliday=true,isSundayPay=true),
            12 to record(12).copy(regularHours=8.0,overtimeHours=16.25,holidayHours=8.0,holidayOvertimeHours=16.0),
            25 to record(25).copy(holidayHours=8.0,holidayOvertimeHours=2.5,isPaidHoliday=true)
        )
        var width = 0
        content("cell-regression") {
            key(language,dark) {
                KoreaSalaryTheme(appTheme=if(dark) AppTheme.DARK else AppTheme.LIGHT) {
                    width=androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
                    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).statusBarsPadding()
                        .padding(horizontal=16.dp).testTag("cell-regression")) {
                        Text("${language.name}/$dark • ${width}dp",Modifier.testTag("cell-locale"))
                        Card(Modifier.fillMaxWidth(),shape=androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            colors=androidx.compose.material3.CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                                cases.entries.chunked(7).forEach { row ->
                                    Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                                        row.forEach { (day,record) -> CalendarCell(day,record,october.atDay(day).dayOfWeek,
                                            LanguageManager.getStringsFor(language),{},Modifier.weight(1f).testTag("cell-bound-$day")) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        val failures=mutableListOf<String>()
        for(mode in listOf(false,true)) for(lang in languages) {
            onMain {language=lang;dark=mode}
            awaitCondition("Calendar not ready") {node("cell-locale").config[SemanticsProperties.Text].single().text.startsWith("${lang.name}/$mode")}
            instrumentation.waitForIdleSync()
            val measurements=onMain {
                val density=instrumentation.targetContext.resources.displayMetrics.density
                val entries=org.json.JSONArray()
                for(day in cases.keys) {
                    val cell=rect(node("cell-bound-$day"))
                    val date=rect(node("calendar-date-$day"))
                    assertEquals("Unchanged aspect ratio",0.85f,cell.width/cell.height,0.025f)
                    for(tag in listOf("calendar-date-$day","calendar-badge-$day")) {
                        if(absent(tag)) continue
                        val n=node(tag);val bounds=rect(n)
                        val layouts=mutableListOf<TextLayoutResult>()
                        n.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts)
                        val l=layouts.single()
                        val contained=bounds.left>=cell.left+1*density-1 && bounds.right<=cell.right-1*density+1 &&
                            bounds.top>=cell.top && bounds.bottom<=cell.bottom-1*density+1
                        val overflow=l.multiParagraph.didExceedMaxLines || l.didOverflowHeight ||
                            (0 until l.lineCount).any {l.getLineRight(it)>l.size.width+1 || l.getLineLeft(it)< -1}
                        val readable=l.layoutInput.style.fontSize.value>=8.5f
                        val overlaps=tag.startsWith("calendar-badge") && bounds.top<date.bottom-1
                        if(lang==AppLanguage.KO && tag.startsWith("calendar-badge") && l.layoutInput.text.text.contains("시간")) {
                            assertTrue("Korean hours unit split across lines",(0 until l.lineCount).any {
                                l.layoutInput.text.text.substring(l.getLineStart(it),l.getLineEnd(it,true)).contains("시간")
                            })
                        }
                        entries.put(org.json.JSONObject().put("day",day).put("tag",tag).put("text",l.layoutInput.text.text)
                            .put("font_sp",l.layoutInput.style.fontSize.value).put("lines",l.lineCount)
                            .put("cell",cell.toString()).put("bounds",bounds.toString()).put("contained",contained)
                            .put("text_overflow",overflow).put("paragraph_exceeds",l.multiParagraph.didExceedMaxLines)
                            .put("height_overflow",l.didOverflowHeight).put("layout_width",l.size.width).put("layout_height",l.size.height)
                            .put("line_left",org.json.JSONArray((0 until l.lineCount).map {l.getLineLeft(it)}))
                            .put("line_right",org.json.JSONArray((0 until l.lineCount).map {l.getLineRight(it)})).put("overlap",overlaps).put("readable",readable))
                        if(!contained || overflow || !readable || overlaps) failures.add("$lang/$mode/$day: contained=$contained overflow=$overflow readable=$readable overlap=$overlaps")
                    }
                    if(cases[day]==null) assertTrue("Empty day has badge",absent("calendar-badge-$day"))
                }
                entries
            }
            val label="calendar-$phase-${lang.name}-$mode-${width}dp"
            File(instrumentation.targetContext.getExternalFilesDir(null),"$label.json").writeText(measurements.toString(2))
            screenshot(label)
        }
        assertTrue("Calendar cell clipping/readability: ${failures.joinToString("; ")}",failures.isEmpty())
    }

    private fun assertNoTextOverflow(case: String) {
        onMain {
            val measurements = org.json.JSONArray()
            val failures = mutableListOf<String>()
            for (n in nodes()) {
                val action = n.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action ?: continue
                val values = mutableListOf<TextLayoutResult>(); action(values)
                for (layout in values) {
                    val text = layout.layoutInput.text.text
                    if (text.isBlank()) continue
                    val wrappedMoney = Regex("^[+\\-]?[0-9][0-9, ]* ₩$").matches(text) && layout.lineCount > 1
                    val overflow = wrappedMoney || layout.multiParagraph.didExceedMaxLines || layout.didOverflowHeight ||
                        (0 until layout.lineCount).any { layout.getLineRight(it) > layout.size.width + 1f || layout.getLineLeft(it) < -1f }
                    measurements.put(org.json.JSONObject().put("text",text).put("font_sp",layout.layoutInput.style.fontSize.value)
                        .put("width_px",layout.size.width).put("height_px",layout.size.height).put("lines",layout.lineCount).put("wrapped_money",wrappedMoney).put("overflow",overflow))
                    if (overflow) failures.add(text)
                }
            }
            val target = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "localization-$case.json")
            target.writeText(measurements.toString(2))
            assertTrue("Clipped text in $case: $failures", failures.isEmpty())
        }
    }

    @Test fun populatedCalendarAtProductionWidthAllLocalesBothThemes() {
        var language by mutableStateOf(AppLanguage.RU)
        var dark by mutableStateOf(false)
        val october = YearMonth.of(2026, 10)
        val records = listOf(5, 9, 11, 25, 6, 10, 18).associateWith { day ->
            DayRecordEntity(october.atDay(day).toString(),october.toString(),
                regularHours = if (day in listOf(25,6,10)) 8.0 else 0.0,
                overtimeHours = if (day == 25) 2.5 else 0.0,
                nightHours = if (day == 25) 4.0 else 0.0,
                isPaidHoliday = day in listOf(5,9), isSundayPay = day == 11)
        }
        content("production-calendar") {
            key(language,dark) {
                KoreaSalaryTheme(appTheme = if (dark) AppTheme.DARK else AppTheme.LIGHT) {
                    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).statusBarsPadding().padding(horizontal=16.dp).testTag("production-calendar")) {
                        Text("${language.name}/$dark",Modifier.testTag("audit-locale"))
                        Card(Modifier.fillMaxWidth(),
                            shape=androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            colors=androidx.compose.material3.CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
                            Row(Modifier.padding(14.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                                records.forEach { (day,record) -> CalendarCell(day,record,october.atDay(day).dayOfWeek,
                                    LanguageManager.getStringsFor(language),{},Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }
        for (mode in listOf(false,true)) for (lang in AppLanguage.entries) {
            onMain { language=lang;dark=mode }
            awaitCondition("Calendar locale not ready") { node("audit-locale").config[SemanticsProperties.Text].single().text == "${lang.name}/$mode" }
            instrumentation.waitForIdleSync();SystemClock.sleep(100)
            val strings=LanguageManager.getStringsFor(lang)
            assertEquals("10.5 ${strings.hoursUnit}",onMain { node("calendar-badge-25").config[SemanticsProperties.Text].single().text })
            assertNoTextOverflow("calendar-${lang.name}-$mode")
            screenshot("production-calendar-${lang.name}-$mode")
        }
    }

    @Test fun populatedSalarySummariesWithLongAmountsAllLocalesBothThemes() {
        var language by mutableStateOf(AppLanguage.RU)
        var dark by mutableStateOf(false)
        val huge = Long.MAX_VALUE / 4
        val result = SalaryResult(
            SalaryBreakdown(regularPay=huge,sundayPay=1234567890L,paidHolidayPay=1234567890L,
                overtimePay=1234567890L,nightPay=1234567890L,holidayPay=1234567890L,totalAllowancesPay=1234567890L,
                grossSalary=huge,taxableAmount=huge),
            DeductionBreakdown(otherDeductions=1234567890L,totalDeductions=1234567890L),netSalary=huge-1234567890L)
        val input=SalaryInputState(regularHours=176.0,overtimeHours=32.5,nightHours=64.0,holidayHours=8.0,
            holidayOvertimeHours=4.0,paidHolidayHours=8.0,sundayPayHours=32.0,
            allowances=AllowancesState(fuelAllowance=1234567890.0))
        val records=mapOf(1 to DayRecordEntity("2026-11-01","2026-11",isSundayPay=true),
            2 to DayRecordEntity("2026-11-02","2026-11",isPaidHoliday=true))
        content("salary-audit") {
            key(language,dark) {
                KoreaSalaryTheme(appTheme=if(dark) AppTheme.DARK else AppTheme.LIGHT) {
                    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).statusBarsPadding()
                        .verticalScroll(rememberScrollState()).padding(horizontal=16.dp).testTag("salary-audit")) {
                        Text("${language.name}/$dark",Modifier.testTag("audit-locale"))
                        MonthlySummaryWidget(input,records,LanguageManager.getStringsFor(language))
                        SalarySummaryCard(result,LanguageManager.getStringsFor(language))
                    }
                }
            }
        }
        for(mode in listOf(false,true)) for(lang in AppLanguage.entries) {
            onMain {language=lang;dark=mode}
            awaitCondition("Salary locale not ready") {node("audit-locale").config[SemanticsProperties.Text].single().text=="${lang.name}/$mode"}
            instrumentation.waitForIdleSync();SystemClock.sleep(100)
            onMain {
                val title=LanguageManager.getStringsFor(lang).breakdownTitle
                val n=nodes().single {it.config.getOrNull(SemanticsProperties.Text)?.any {t->t.text==title}==true}
                val clickable=generateSequence(n) {it.parent}.first {it.config.contains(SemanticsActions.OnClick)}
                clickable.config[SemanticsActions.OnClick].action!!.invoke()
            }
            awaitCondition("Breakdown not expanded") {nodes().any {it.config.getOrNull(SemanticsProperties.Text)?.any {t->t.text==LanguageManager.getStringsFor(lang).ui.detailRegularPayTitle}==true}}
            instrumentation.waitForIdleSync();SystemClock.sleep(300)
            assertNoTextOverflow("salary-${lang.name}-$mode")
            screenshot("salary-monthly-${lang.name}-$mode")
            onMain {node("salary-audit").config[SemanticsActions.ScrollBy].action!!.invoke(0f,700f)}
            SystemClock.sleep(300);screenshot("salary-breakdown-${lang.name}-$mode")
        }
    }

    // Layout contract for the new action row: Save keeps one whole centered line,
    // Cancel stays centered, and "Очистить день" may wrap to two centered lines.
    @Test fun actionRowKeepsSaveOneLineAndCentersLabelsAllLocalesBothThemes() {
        var language by mutableStateOf(AppLanguage.RU)
        var dark by mutableStateOf(false)
        content {
            key(language, dark) {
                KoreaSalaryTheme(appTheme = if (dark) AppTheme.DARK else AppTheme.LIGHT) {
                    DayEditDialog(1, month, DayRecordEntity("2026-11-01", "2026-11"),
                        LanguageManager.getStringsFor(language), {},
                        { _, _, _, _, _, _, _, _, _ -> }, {})
                }
            }
        }
        fun layout(label: String): TextLayoutResult = onMain {
            val results = mutableListOf<TextLayoutResult>()
            nodes().single { it.config.getOrNull(SemanticsProperties.Text)?.any { it.text == label } == true }
                .config[SemanticsActions.GetTextLayoutResult].action!!.invoke(results)
            results.single()
        }
        fun assertCentered(case: String, result: TextLayoutResult, maxLines: Int) {
            assertTrue("$case exceeded maxLines", result.lineCount <= maxLines)
            assertFalse("$case overflowed height", result.didOverflowHeight)
            for (line in 0 until result.lineCount) {
                val center = (result.getLineLeft(line) + result.getLineRight(line)) / 2f
                assertEquals("$case line $line not centered", result.size.width / 2f, center, 2f)
            }
        }
        val compact = instrumentation.targetContext.resources.configuration.screenWidthDp <= 320
        for (mode in listOf(false, true)) for (lang in AppLanguage.entries) {
            onMain { language = lang; dark = mode }
            val strings = LanguageManager.getStringsFor(lang)
            awaitCondition("Action row not rendered: $lang/$mode") {
                nodes().any { it.config.getOrNull(SemanticsProperties.Text)?.any { it.text == strings.ui.saveAction } == true }
            }
            instrumentation.waitForIdleSync(); SystemClock.sleep(100)
            // Save must show the complete word on exactly one centered line at a readable size.
            val save = layout(strings.ui.saveAction)
            assertEquals("Save wrapped in $lang/$mode: ${save.size.width}px", 1, save.lineCount)
            assertCentered("Save $lang/$mode", save, 1)
            assertTrue("Save font too small in $lang/$mode", save.layoutInput.style.fontSize.value >= 12f)
            // Cancel stays centered; Russian never wraps it.
            assertCentered("Cancel $lang/$mode", layout(strings.ui.cancelAction), 2)
            // "Очистить день" may use two lines; both stay centered in its action area.
            assertCentered("Clear $lang/$mode", layout(strings.ui.clearDayAction), 2)
            // Save gets sufficient width for its whole word, wider than Cancel.
            val saveWidth = onMain { rect(node("save-day")).width }
            val cancelWidth = onMain { rect(node("cancel-day")).width }
            assertTrue("Save too narrow in $lang/$mode: $saveWidth <= $cancelWidth", saveWidth > cancelWidth)
            if (compact && lang == AppLanguage.RU) {
                assertEquals("Russian clear label must wrap on 320 dp", 2,
                    layout(strings.ui.clearDayAction).lineCount)
            }
            // Visual evidence: Russian (wrap case) plus the longest clear-label translations.
            if (lang == AppLanguage.RU || lang in listOf(AppLanguage.ID, AppLanguage.MY)) {
                scrollTo("save-day")
                instrumentation.waitForIdleSync(); SystemClock.sleep(300)
                if (lang == AppLanguage.RU) {
                    screenshot(if (mode) "day-actions-3-ru-dark" else "day-actions-3-ru-light")
                } else {
                    screenshot("day-actions-3-${lang.name}-${if (mode) "dark" else "light"}")
                }
            }
        }
    }

    @Test fun twoActionRowStaysBalancedWithOneLineSave() {
        content {
            KoreaSalaryTheme(appTheme = AppTheme.LIGHT) {
                DayEditDialog(1, month, null, strings, {}, { _, _, _, _, _, _, _, _, _ -> }, {})
            }
        }
        instrumentation.waitForIdleSync()
        assertTrue("Clear action must stay hidden for empty days", onMain { absent("clear-day") })
        val saveWidth = onMain { rect(node("save-day")).width }
        val cancelWidth = onMain { rect(node("cancel-day")).width }
        assertEquals("Two actions must stay balanced: $saveWidth vs $cancelWidth", saveWidth, cancelWidth)
        fun centered(label: String) = onMain {
            val results = mutableListOf<TextLayoutResult>()
            nodes().single { it.config.getOrNull(SemanticsProperties.Text)?.any { it.text == label } == true }
                .config[SemanticsActions.GetTextLayoutResult].action!!.invoke(results)
            results.single().let { layout ->
                assertEquals("$label wrapped: ${layout.size.width}px", 1, layout.lineCount)
                val center = (layout.getLineLeft(0) + layout.getLineRight(0)) / 2f
                assertEquals("$label not centered", layout.size.width / 2f, center, 2f)
            }
        }
        centered(strings.ui.saveAction)
        centered(strings.ui.cancelAction)
        scrollTo("save-day")
        instrumentation.waitForIdleSync(); SystemClock.sleep(300)
        screenshot("day-actions-2-ru-light")
    }

    @Test fun existingDayFooterValidationWarningAndInstalledVersionAllLocalesBothThemes() {
        var language by mutableStateOf(AppLanguage.RU)
        var dark by mutableStateOf(false)
        val existing = DayRecordEntity("2026-11-01","2026-11",regularHours=8.0,overtimeHours=2.5,
            nightHours=4.0,holidayOvertimeHours=2.0,isPaidHoliday=true,isSundayPay=true,note="Audit shift")
        content {
            key(language,dark) {
                KoreaSalaryTheme(appTheme=if(dark) AppTheme.DARK else AppTheme.LIGHT) {
                    val strings=LanguageManager.getStringsFor(language)
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                        Column {
                            Text("${language.name}/$dark",Modifier.testTag("audit-locale"))
                            Text(installedVersionLabel(LocalContext.current,strings),Modifier.testTag("installed-version"))
                        }
                        DayEditDialog(1,month,existing,strings,{}, {_,_,_,_,_,_,_,_,_->},{})
                    }
                }
            }
        }
        for(mode in listOf(false,true)) for(lang in AppLanguage.entries) {
            onMain {language=lang;dark=mode}
            awaitCondition("Dialog locale not ready") {node("audit-locale").config[SemanticsProperties.Text].single().text=="${lang.name}/$mode"}
            instrumentation.waitForIdleSync();SystemClock.sleep(100)
            val strings=LanguageManager.getStringsFor(lang)
            val version=onMain {node("installed-version").config[SemanticsProperties.Text].single().text}
            assertTrue(version,version.contains(instrumentation.targetContext.packageManager.getPackageInfo(instrumentation.targetContext.packageName,0).versionName!!))
            assertFalse(version,version.contains("%s"))
            onMain {node("hours-base").config[SemanticsActions.SetText].action!!.invoke(AnnotatedString("-1"))}
            awaitCondition("Localized validation missing") {nodes().any {it.config.getOrNull(SemanticsProperties.Text)?.any {t->t.text==strings.ui.baseHoursValidation}==true}}
            assertNoTextOverflow("validation-${lang.name}-$mode")
            onMain {node("hours-base").config[SemanticsActions.SetText].action!!.invoke(AnnotatedString("8"))}
            onMain {node("hours-base").config[SemanticsActions.RequestFocus].action!!.invoke()}
            awaitCondition("Sunday warning missing") {!absent("sunday-warning")}
            awaitKeyboard();SystemClock.sleep(350)
            assertEquals(strings.ui.sundayBaseHoursWarning,onMain {node("sunday-warning").config[SemanticsProperties.Text].single().text})
            assertNoTextOverflow("existing-dialog-${lang.name}-$mode")
            screenshot("existing-day-fields-${lang.name}-$mode")
            for(tag in listOf("clear-day","cancel-day","save-day")) scrollTo(tag)
            assertTrue(onMain {visible("save-day") && keyboardVisible()})
            screenshot("existing-day-footer-${lang.name}-$mode")
        }
    }

}
