package com.koreasalary.calculator.data.model

/**
 * 12 поддерживаемых языков приложения. Порядок отражает удобную для
 * пользователя группировку: базовые языки, затем Центральная Азия и затем
 * Юго-Восточная Азия.
 */
enum class AppLanguage(val code: String, val displayName: String, val flagEmoji: String) {
    RU("ru", "Русский", "🇷🇺"),
    KO("ko", "한국어", "🇰🇷"),
    EN("en", "English", "🇺🇸"),
    KY("ky", "Кыргызча", "🇰🇬"),
    UZ("uz", "O'zbekcha", "🇺🇿"),
    KK("kk", "Қазақша", "🇰🇿"),
    VI("vi", "Tiếng Việt", "🇻🇳"),
    TH("th", "ไทย", "🇹🇭"),
    FIL("fil", "Filipino", "🇵🇭"),
    ID("id", "Bahasa Indonesia", "🇮🇩"),
    MS("ms", "Bahasa Melayu", "🇲🇾"),
    MY("my", "မြန်မာဘာသာ", "🇲🇲")
}

/**
 * Тема оформления приложения
 */
enum class AppTheme {
    SYSTEM, // Системная (автопереключение)
    LIGHT,  // Светлая
    DARK    // Тёмная
}

/**
 * Быстрые пресеты для настроек налогов и вычетов
 */
enum class PresetType {
    BUSINESS_3_3,     // 3.3% Фрилансер / Подработка (3.3% 프리랜서 / 아르바이트)
    FOUR_INSURANCES,  // Страховые удержания по документам без подоходного налога
    FULL_EMPLOYEE,    // Полный штат: страховые удержания + ориентировочный подоходный налог
    NO_DEDUCTIONS     // Без вычетов / Чистая выплата (공제 없음)
}
