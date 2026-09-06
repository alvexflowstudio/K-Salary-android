package com.koreasalary.calculator.data.repository

import com.koreasalary.calculator.data.local.entity.ShiftTemplateEntity
import com.koreasalary.calculator.data.model.AppLanguage
import com.koreasalary.calculator.domain.i18n.LanguageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InfoContentRepositoryTest {

    @Test
    fun everyLanguageHasTheCompleteGuideStructure() {
        val expectedTopicCounts = listOf(3, 3, 2, 2, 3)

        AppLanguage.values().forEach { language ->
            val sections = InfoContentRepository.getSections(language)

            assertEquals("Unexpected section count for $language", 5, sections.size)
            assertEquals(
                "Unexpected topic counts for $language",
                expectedTopicCounts,
                sections.map { it.topics.size }
            )
            assertTrue(sections.all { it.title.isNotBlank() && it.description.isNotBlank() })
            assertTrue(
                sections.flatMap { it.topics }.all { topic ->
                    topic.title.isNotBlank() &&
                        topic.summary.isNotBlank() &&
                        topic.content.isNotBlank() &&
                        topic.keyPoints.isNotEmpty()
                }
            )
        }
    }

    @Test
    fun nonKoreanLanguagesShowKoreanPayrollTermsWithoutDuplicatingKoreanUi() {
        AppLanguage.values()
            .filter { it != AppLanguage.KO }
            .forEach { language ->
                val strings = LanguageManager.getStringsFor(language)
                val guideTopics = InfoContentRepository.getSections(language).flatMap { it.topics }

                assertTrue("Missing hourly-rate glossary for $language", strings.ui.hourlyRateHelpItems.any { it.title.contains("시급") })
                assertTrue("Missing FAQ glossary for $language", guideTopics.any { it.title.contains("퇴직금") })
                assertTrue("Missing 3.3% payroll term for $language", strings.tax33Label.contains("사업소득 원천징수"))
                assertTrue("Missing pension payroll term for $language", strings.nationalPensionLabel.contains("국민연금"))
                assertTrue("Missing health payroll term for $language", strings.healthInsuranceLabel.contains("건강보험"))
                assertTrue("Missing long-term-care payroll term for $language", strings.longTermCareLabel.contains("장기요양보험"))
                assertTrue("Missing employment payroll term for $language", strings.employmentInsuranceLabel.contains("고용보험"))
                assertTrue("Missing fuel payroll term for $language", strings.ui.fuelAllowanceLabel.contains("유류비"))
                assertTrue("Missing annual-leave payroll term for $language", strings.ui.annualLeaveAllowanceLabel.contains("연차수당"))
                assertFalse("English language marker leaked into $language", strings.languageSectionTitle.contains("Language"))
            }

        val koreanHelp = LanguageManager.getStringsFor(AppLanguage.KO).ui.hourlyRateHelpItems
        assertTrue(koreanHelp.all { !it.title.contains("(") })
    }

    @Test
    fun kazakhIsIncludedInCentralAsianLanguageSet() {
        assertEquals("kk", AppLanguage.KK.code)
        assertTrue(LanguageManager.getStringsFor(AppLanguage.KK).appTitle.isNotBlank())
        assertTrue(InfoContentRepository.getSections(AppLanguage.KK).all { it.title.isNotBlank() })
    }

    @Test
    fun builtInShiftTemplateNameUsesSelectedLanguage() {
        AppLanguage.values().forEach { language ->
            val ui = LanguageManager.getStringsFor(language).ui
            val dayTemplateName = ui.defaultDayShiftTemplateName
            val nightTemplateName = ui.defaultNightShiftTemplateName

            assertTrue("Missing built-in day template name for $language", dayTemplateName.isNotBlank())
            assertTrue("Missing built-in night template name for $language", nightTemplateName.isNotBlank())
            assertFalse("Day and night template names are identical for $language", dayTemplateName == nightTemplateName)
            if (language != AppLanguage.RU) {
                assertFalse(
                    "Stored Russian template name leaked into $language",
                    dayTemplateName == ShiftTemplateEntity.STORED_DEFAULT_TITLE ||
                        nightTemplateName == ShiftTemplateEntity.STORED_DEFAULT_TITLE
                )
            }
        }
    }

    @Test
    fun newUsersReceiveDayAndNightEightHourTemplates() {
        val templates = ShiftTemplateEntity.builtInDefaults()

        assertEquals(2, templates.size)
        assertEquals(ShiftTemplateEntity.STORED_DEFAULT_DAY_TITLE, templates[0].title)
        assertEquals(ShiftTemplateEntity.STORED_DEFAULT_NIGHT_TITLE, templates[1].title)
        assertEquals(8.0, templates[0].regularHours, 0.0)
        assertEquals(8.0, templates[1].regularHours, 0.0)
        assertEquals(0.0, templates[0].nightHours, 0.0)
        assertEquals(8.0, templates[1].nightHours, 0.0)
        assertTrue(templates.all { it.isDefault })
    }
}
