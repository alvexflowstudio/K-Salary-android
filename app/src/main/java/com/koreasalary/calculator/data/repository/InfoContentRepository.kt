package com.koreasalary.calculator.data.repository

import com.koreasalary.calculator.data.model.AppLanguage
import com.koreasalary.calculator.data.model.GuideSection
import com.koreasalary.calculator.data.model.GuideTopic

/**
 * Полный текст справочника хранится отдельными языковыми пакетами.
 * Переключение языка поэтому меняет весь FAQ, включая описания и пункты проверки.
 */
object InfoContentRepository {

    private val sectionIds = listOf("sec_severance", "sec_taxes", "sec_hours", "sec_leave", "sec_visas")
    private val topicIds = listOf(
        listOf("sev_conditions", "sev_formula", "sev_taxes_and_e9"),
        listOf("tax_33", "tax_insurance_deductions", "allowances"),
        listOf("hours_209", "hours_multipliers"),
        listOf("leave_annual", "unpaid_wages"),
        listOf("visa_e9", "visa_h2", "visa_f_series")
    )

    fun getSections(language: AppLanguage): List<GuideSection> {
        val sections = when (language) {
            AppLanguage.RU -> russianSections()
            AppLanguage.KO -> koreanSections()
            AppLanguage.EN -> englishSections()
            AppLanguage.VI -> vietnameseSections()
            AppLanguage.UZ -> uzbekSections()
            AppLanguage.KY -> kyrgyzSections()
            AppLanguage.KK -> kazakhSections()
            AppLanguage.TH -> thaiSections()
            AppLanguage.FIL -> filipinoSections()
            AppLanguage.ID -> indonesianSections()
            AppLanguage.MS -> malaySections()
            AppLanguage.MY -> burmeseSections()
        }
        return if (language == AppLanguage.KO) sections else addKoreanGlossary(sections)
    }

    /**
     * Иностранцу полезно видеть корейское слово из расчётного листка рядом с
     * переводом. Корейский интерфейс уже содержит эти термины и не получает
     * вторую копию в скобках.
     */
    private fun addKoreanGlossary(sections: List<GuideSection>): List<GuideSection> {
        val glossary = mapOf(
            "sev_conditions" to "퇴직금",
            "sev_formula" to "평균임금",
            "sev_taxes_and_e9" to "출국만기보험",
            "tax_33" to "사업소득 원천징수",
            "tax_insurance_deductions" to "4대보험",
            "allowances" to "교통비 · 연차수당",
            "hours_209" to "주휴일 · 주휴수당",
            "hours_multipliers" to "연장근로 · 야간근로 · 휴일근로",
            "leave_annual" to "연차휴가",
            "unpaid_wages" to "임금체불 · 고용노동부",
            "visa_e9" to "출국만기보험"
        )
        return sections.map { section ->
            section.copy(
                topics = section.topics.map { topic ->
                    glossary[topic.id]?.let { koreanTerm ->
                        topic.copy(title = "${topic.title} ($koreanTerm)")
                    } ?: topic
                }
            )
        }
    }

    private data class TopicCopy(
        val title: String,
        val summary: String,
        val content: String,
        val keyPoints: List<String>
    )

    private data class SectionCopy(
        val title: String,
        val description: String,
        val topics: List<TopicCopy>
    )

    private fun topic(
        title: String,
        summary: String,
        content: String,
        keyPoints: List<String>
    ) = TopicCopy(title, summary, content.trimIndent(), keyPoints)

    private fun section(
        title: String,
        description: String,
        topics: List<TopicCopy>
    ) = SectionCopy(title, description, topics)

    private fun buildSections(copies: List<SectionCopy>): List<GuideSection> {
        return copies.mapIndexed { sectionIndex, copy ->
            GuideSection(
                id = sectionIds[sectionIndex],
                title = copy.title,
                koreanTitle = copy.title,
                description = copy.description,
                topics = copy.topics.mapIndexed { topicIndex, topic ->
                    GuideTopic(
                        id = topicIds[sectionIndex][topicIndex],
                        title = topic.title,
                        summary = topic.summary,
                        content = topic.content,
                        keyPoints = topic.keyPoints
                    )
                }
            )
        }
    }

    private fun russianSections(): List<GuideSection> = buildSections(listOf(
        section("1. Выходное пособие", "Условия права, формула среднего заработка и сравнение с обычной дневной зарплатой.", listOf(
            topic("Когда возникает право", "Право проверяют по непрерывному стажу и средней продолжительности работы.", """
                Законное право на выходное пособие возникает при непрерывной работе у одного работодателя не менее 1 года и средней продолжительности работы от 15 часов в неделю за 4 недели. Условия нужно проверять по трудовым документам и фактическому графику.

            """, listOf("Непрерывная работа от 365 дней", "Средняя занятость от 15 часов в неделю", "Сверка по трудовым документам")),
            topic("Как считается сумма", "Сначала рассчитывается дневной показатель, затем он умножается на стаж.", """
                Среднедневной заработок = заработок за последние 3 месяца / число календарных дней в расчётном периоде. В расчётную сумму добавляют 3/12 годовой премии и 3/12 компенсации за неиспользованный отпуск, если эти выплаты относятся к расчётному периоду.

                Для законного расчёта сравнивают среднедневной заработок с обычной дневной зарплатой и применяют большее значение. Итоговая формула: применяемая дневная сумма × 30 × общее число дней непрерывной работы / 365.
            """, listOf("Фактические календарные дни", "Сравнение средней и обычной дневной зарплаты", "Проверка по расчётному листку")),
            topic("Страховка выезда и окончательный расчёт", "Для E-9 и H-2 приложение показывает разницу между страховкой и расчётным пособием.", """
                В поле страховки выезда внесите сумму, указанную страховой компанией или в документах работодателя. Приложение вычитает её из начисленного пособия и показывает возможную разницу для дополнительной проверки.

                Выходное пособие облагается отдельным налогом на доход от увольнения. Приведённый в приложении налог носит оценочный характер и не заменяет расчёт по официальной налоговой таблице. При увольнении окончательные выплаты должны быть произведены в течение 14 дней, если стороны письменно не согласовали иной срок в предусмотренных законом случаях.
            """, listOf("Сумма из официального документа", "Разница предварительная", "Налог считается отдельно от ежемесячных удержаний"))
        )),
        section("2. Налоги и оформление работы", "Разница между удержанием 3,3%, страховыми удержаниями и договорными доплатами.", listOf(
            topic("Удержание 3,3%", "Ставка связана с доходом от самостоятельной деятельности и сама по себе не меняет фактический статус работы.", """
                Удержание 3,3% состоит из 3,0% налога на доход и 0,3% местного налога. Такая схема встречается у подрядчиков и фрилансеров, но запись в договоре не отменяет проверку фактических отношений: графика, подчинения и характера работы.

                В мае следующего года доход обычно декларируют отдельно. Возможность возврата зависит от общей декларации и подтверждённых расходов.
            """, listOf("3,3% не доказывает статус фрилансера", "Проверьте реальный график", "Возврат зависит от годовой декларации")),
            topic("Основные страховые удержания", "Пенсия, медицина, долгосрочный уход и занятость по документам.", """
                Страховые удержания зависят от визы, гражданства, статуса работника и расчётного листка. В калькуляторе долгосрочный уход считается как дополнительный платёж, связанный с медицинской страховкой. Страхование от несчастных случаев на производстве обычно оплачивает работодатель, поэтому мы не включаем его в удержания работника.

                Для режима 3,3% оставляйте страховые пункты выключенными, если их нет в расчётном листке. Подоходный налог рассчитывается отдельно по действующей таблице, поэтому проценты в калькуляторе служат для предварительной оценки.
            """, listOf("Проверьте статус и визу", "Работник и работодатель платят разные доли", "Расчётный листок важнее оценки")),
            topic("Договорные доплаты", "Дополнительные выплаты вводят в разделе надбавок с названием из вашего расчётного листка.", """
                Премии, транспорт и другая доплата входят в начисление, если работодатель указал их в расчётном листке. В разделе надбавок можно добавить собственное название и сумму, поэтому отдельные специальные поля не требуются.

                Сверяйте каждую доплату с договором и расчётным листком. Если сумма зависит от фактически отработанных часов, сначала внесите часы в календарь и только затем добавьте подтверждённую выплату.
            """, listOf("Используйте название из расчётного листка", "Добавляйте сумму вручную", "Сверяйте начисление с документами"))
        )),
        section("3. Рабочее время и доплаты", "Базовые часы, еженедельный оплачиваемый отдых и повышающие коэффициенты.", listOf(
            topic("Месячная норма и еженедельный отдых", "Показатель 209 часов используют как расчётный ориентир для полной рабочей недели.", """
                Ориентир 209 часов получается из 40 рабочих часов и оплачиваемого еженедельного отдыха, пересчитанных на среднее число недель в месяце. Фактическая месячная норма зависит от календаря, графика и условий договора.

                Оплата еженедельного отдыха связана с продолжительностью работы от 15 часов в неделю и выполнением назначенных рабочих дней. Приложение показывает её отдельной строкой.
            """, listOf("209 часов служат ориентиром", "Проверьте недельный график", "Оплачиваемый отдых выводится отдельно")),
            topic("Сверхурочные, ночь и праздник", "Для расчёта применяются повышающие коэффициенты.", """
                Приложение использует ориентиры: сверхурочная работа 1,5×, ночная доплата 0,5× с 22:00 до 06:00, праздничная работа до 8 часов 1,5× и часы сверх 8 часов 2,0×.

                Один и тот же час нельзя одновременно записывать в несколько категорий без основания.
            """, listOf("Сверхурочные 1,5×", "Ночная доплата 0,5×", "Праздник 1,5× и 2,0×"))
        )),
        section("4. Отпуск и невыплата зарплаты", "Ежегодный отпуск, компенсация и действия при задержке окончательного расчёта.", listOf(
            topic("Ежегодный оплачиваемый отпуск", "Количество дней зависит от стажа, посещаемости и правил предоставления отпуска.", """
                В первый год при выполнении условий за каждый полностью отработанный месяц может начисляться один день отпуска, всего до 11 дней. Со второго года применяется базовое количество дней, а дальнейшее увеличение связано со стажем и установленным максимумом.

                Неиспользованные дни и компенсацию нужно сверять с кадровыми документами. Не каждая выплата с названием отпускных входит в расчёт выходного пособия одинаково.
            """, listOf("Первый год: до 11 дней", "Далее количество зависит от стажа", "Компенсация проверяется по документам")),
            topic("Задержка зарплаты и обращение за помощью", "Сохраняйте доказательства работы и обращайтесь в трудовую службу при невыплате.", """
                Сохраните договор, табель, расчётные листки, банковские переводы и переписку с работодателем. При увольнении зарплата и выходное пособие должны быть выплачены в течение 14 дней, если законное соглашение не устанавливает другой срок.

                При задержке можно обратиться в Министерство занятости и труда Республики Корея по номеру 1350 или подать жалобу через его сервисы.
            """, listOf("Окончательный расчёт: 14 дней", "Горячая линия труда: 1350", "Договор и переводы подтверждают долг"))
        )),
        section("5. Правила для разных виз", "Особенности E-9, H-2 и виз серии F при работе в Республике Корея.", listOf(
            topic("Виза E-9", "Работа по системе EPS связана с отдельными правилами смены места работы и страхования.", """
                Для E-9 действуют правила системы разрешений на трудоустройство иностранцев. Страхование выезда обычно оформляется отдельно работодателем, поэтому его сумму нужно проверять по документам страховой компании.

                Количество смен работодателя, срок пребывания и разрешённая отрасль зависят от актуальных правил EPS. Перед сменой работы проверьте информацию в службе занятости или миграционной службе.
            """, listOf("Проверяйте сумму страховки", "Уточняйте правила смены работы", "Используйте актуальные правила EPS")),
            topic("Виза H-2", "Виза посещения и трудоустройства требует проверки разрешённых процедур и отрасли.", """
                Для H-2 могут требоваться обучение трудоустройству, регистрация поиска работы и работа в разрешённых отраслях. Перед началом работы проверьте, что место и вид деятельности соответствуют разрешению.

                Трудовые выплаты и выходное пособие проверяются по общим условиям закона, а страховые процедуры зависят от статуса и документов работника.
            """, listOf("Проверьте разрешённую отрасль", "Храните документы о работе", "Страхование и пособие сверяйте отдельно")),
            topic("Визы серии F", "Условия работы зависят от конкретного статуса F-2, F-4, F-5 или F-6.", """
                Визы серии F различаются по праву на работу и ограничениям для отдельных видов деятельности. Нельзя переносить правила F-4 на F-2, F-5 или F-6 без проверки конкретного статуса.

                Налоги, страховки, отпуск и выходное пособие рассчитываются по фактическим трудовым отношениям. При споре сохраните копию визы, регистрационной карты и договора.
            """, listOf("Проверяйте конкретный статус", "Уточняйте ограничения работы", "Храните копии документов"))
        ))
    ))

    private fun koreanSections(): List<GuideSection> = buildSections(listOf(
        section("1. 퇴직금", "퇴직금 지급 요건, 평균임금 계산과 통상임금 비교", listOf(
            topic("퇴직금 지급 요건", "계속근로기간과 주당 평균 근로시간으로 지급 요건을 확인합니다.", """
                같은 사용자에게 계속 근로한 기간이 1년 이상이고 4주간 평균 주 소정근로시간이 15시간 이상이면 법정 퇴직금 지급 대상이 될 수 있습니다. 근로계약서와 근무기록으로 조건을 확인해야 합니다.

            """, listOf("계속근로 365일 이상", "주 15시간 이상", "고용·급여 서류로 조건 확인")),
            topic("퇴직금 계산 공식", "먼저 1일 금액을 계산한 뒤 계속근로일수에 따라 산정합니다.", """
                1일 평균임금 = 퇴직 전 3개월 임금 / 그 기간의 실제 달력 일수입니다. 산정 대상에 포함되는 연간 상여금과 연차수당은 각각 3/12를 반영할 수 있습니다.

                평균임금과 1일 통상임금을 비교하여 큰 금액을 적용합니다. 최종 공식은 적용 1일 금액 × 30 × 계속근로일수 / 365입니다.
            """, listOf("실제 달력 일수 입력", "평균임금과 통상임금 비교", "급여명세서 확인")),
            topic("출국만기보험과 정산", "E-9·H-2 근로자는 보험금과 법정 퇴직금의 차이를 확인할 수 있습니다.", """
                보험회사 또는 사업주 서류에 표시된 출국만기보험 금액을 입력하면 앱이 법정 퇴직금과의 차이를 계산합니다. 표시된 차이는 서류 확인을 위한 예상값입니다.

                퇴직금에는 퇴직소득세가 별도로 적용됩니다. 앱의 세금은 예상치이며 공식 세율표를 대신하지 않습니다. 법정 예외나 서면 합의가 없으면 퇴직 시 최종 금액은 14일 이내 지급 대상입니다.
            """, listOf("보험 서류의 금액 입력", "차액은 예상값", "퇴직소득세는 별도"))
        )),
        section("2. 세금과 고용 형태", "3.3% 원천징수, 보험 공제와 계약상 추가 지급", listOf(
            topic("3.3% 원천징수", "3.3% 공제만으로 프리랜서 여부가 결정되지는 않습니다.", """
                3.3%는 소득세 3.0%와 지방소득세 0.3%로 구성됩니다. 도급이나 프리랜서에게 사용되지만 계약서 표현만으로 근로자성이 사라지지 않으므로 실제 지휘·감독과 근무시간을 확인해야 합니다.

                다음 해 5월 종합소득세 신고에서 최종 세액과 환급 여부가 결정됩니다.
            """, listOf("3.3%만으로 고용 형태 판단 금지", "실제 근무 형태 확인", "5월 신고 확인")),
            topic("주요 보험 공제", "국민연금, 건강보험, 장기요양과 고용보험은 서류에 따라 적용됩니다.", """
                보험 공제는 비자, 국적, 근로자 지위와 급여명세서에 따라 달라집니다. 장기요양보험은 건강보험료에 연동되는 추가 보험료로 계산됩니다. 산재보험은 일반적으로 사업주가 부담하므로 근로자 공제로 입력하지 않습니다.

                3.3% 방식이면 급여명세서에 실제로 표시된 경우에만 보험 항목을 켜십시오. 근로소득세는 보험료와 별도로 계산되며 앱의 비율은 예상 계산입니다.
            """, listOf("비자와 서류 확인", "사업주와 근로자 부담분 구분", "급여명세서 확인")),
            topic("계약상 추가 지급", "급여명세서에 표시된 이름과 금액을 추가 지급 항목에 직접 입력합니다.", """
                보너스, 교통비 등 추가 지급은 급여명세서에 표시된 금액을 기준으로 계산합니다. 앱의 추가 지급 항목에서 이름과 금액을 직접 입력할 수 있으므로 별도의 특수 입력란은 필요하지 않습니다.

                계약서와 급여명세서의 금액을 대조하고 실제 지급된 항목만 입력하십시오.
            """, listOf("급여명세서 이름 사용", "추가 지급을 직접 입력", "계약서와 대조"))
        )),
        section("3. 근로시간과 가산수당", "기본근로, 주휴수당과 연장·야간·휴일근로 가산", listOf(
            topic("월 209시간과 주휴수당", "209시간은 주 40시간과 주휴시간을 월 평균으로 환산한 기준입니다.", """
                월 209시간은 주 40시간과 유급 주휴시간을 1개월 평균으로 환산한 기준입니다. 실제 월 근로시간은 달력, 근무표와 계약 조건에 따라 달라집니다.

                주 15시간 이상 근무하고 정해진 근무일을 개근한 경우 주휴수당 요건을 확인할 수 있습니다. 앱은 주휴수당을 별도 표시합니다.
            """, listOf("209시간은 월 평균 기준", "실제 근무표 확인", "주휴수당 별도 표시")),
            topic("연장·야간·휴일근로", "앱은 법정 가산 기준을 참고하여 계산합니다.", """
                앱은 연장근로 1.5배, 22시부터 06시까지 야간근로 가산 0.5배, 휴일근로 8시간 이내 1.5배와 8시간 초과 2.0배를 기준으로 계산합니다.

                같은 시간을 여러 항목에 중복 입력하면 금액이 부풀려집니다. 출퇴근 기록과 휴일 여부를 먼저 확인하십시오.
            """, listOf("연장근로 1.5배", "야간 가산 0.5배", "휴일근로 1.5배·2.0배"))
        )),
        section("4. 연차휴가와 임금체불", "연차유급휴가, 보상과 임금 미지급 시 대응", listOf(
            topic("연차유급휴가", "연차 일수는 계속근로기간과 출근율에 따라 달라집니다.", """
                1년 미만 근로자는 요건을 충족한 월마다 1일의 연차가 발생할 수 있으며 첫해 최대 11일까지입니다. 2년 차부터는 기본 연차와 계속근로기간에 따른 가산을 확인합니다.

                미사용 연차수당은 인사기록과 급여명세서로 확인해야 합니다.
            """, listOf("첫해 최대 11일", "근속과 출근율 확인", "미사용 수당 확인")),
            topic("임금체불 대응", "근로계약서와 근무·지급 기록을 보관하고 1350에 상담합니다.", """
                근로계약서, 근무표, 급여명세서, 계좌 입금내역과 사업주와의 대화를 보관합니다. 퇴직 시 임금과 퇴직금은 법정 예외나 서면 합의가 없으면 14일 이내 지급 대상입니다.

                임금이 지급되지 않으면 고용노동부 1350 또는 관련 민원 서비스를 이용할 수 있습니다.
            """, listOf("정산 기한 14일", "고용노동부 1350", "계약서와 입금내역 보관"))
        )),
        section("5. 비자별 근로 규정", "E-9, H-2와 F 계열 비자의 근로 확인사항", listOf(
            topic("E-9 비자", "고용허가제와 출국만기보험 관련 절차를 함께 확인해야 합니다.", """
                E-9는 외국인 고용허가제(EPS)의 적용을 받습니다. 출국만기보험은 별도 서류로 관리되므로 보험회사 또는 사업주가 제공한 금액을 확인해야 합니다.

                사업장 변경 횟수, 체류기간과 허용 업종은 최신 EPS 기준과 변경 사유에 따라 달라집니다.
            """, listOf("보험 서류 확인", "사업장 변경 사유 확인", "최신 EPS 기준 확인")),
            topic("H-2 비자", "취업교육과 허용 업종 등 현재 절차를 확인해야 합니다.", """
                H-2는 취업교육, 구직신청과 허용 업종 확인이 필요할 수 있습니다. 일을 시작하기 전에 사업장과 업무가 허용 범위인지 확인하십시오.

                임금과 퇴직금은 실제 근로조건에 따라 확인하고 보험 절차는 체류자격과 서류에 따라 따로 확인합니다.
            """, listOf("허용 업종 확인", "취업 서류 보관", "보험과 퇴직금 별도 확인")),
            topic("F 계열 비자", "F-2, F-4, F-5, F-6는 각각 근로 제한과 조건이 다릅니다.", """
                F 계열 비자는 체류자격별로 취업 가능 범위와 제한이 다릅니다. 본인의 체류자격을 기준으로 취업 제한을 확인해야 합니다.

                세금, 보험, 휴가와 퇴직금은 실제 근로관계에 따라 계산합니다. 분쟁에 대비해 외국인등록증과 근로계약서 사본을 보관하십시오.
            """, listOf("본인 체류자격 확인", "취업 제한 확인", "계약서와 등록증 보관"))
        ))
    ))

    private fun englishSections(): List<GuideSection> = buildSections(listOf(
        section("1. Severance pay", "Eligibility, average wage and the comparison with ordinary daily wage.", listOf(
            topic("When the right arises", "Check continuous service and average weekly hours.", """
                Statutory severance generally applies when continuous service with one employer is at least one year and average weekly scheduled hours over four weeks are at least 15. Confirm both conditions against the contract and work records.

                This calculator helps foreign workers compare their own hours, payslips and settlement documents. For a legal dispute, confirm the facts against the employment contract and official records.
            """, listOf("At least 365 days of continuous service", "At least 15 hours per week", "Check the employment documents")),
            topic("How the amount is calculated", "The daily amount is calculated first and then multiplied by service.", """
                Average daily wage = wages for the last three months / the calendar days in that period. Eligible annual bonuses and unused-leave compensation can be reflected at 3/12 each.

                Compare the average daily wage with the ordinary daily wage and apply the higher amount. The total is: applied daily amount × 30 × continuous service days / 365.
            """, listOf("Use actual calendar days", "Compare average and ordinary daily wage", "Check payroll records")),
            topic("Departure insurance and settlement", "For E-9 and H-2, the app shows a possible difference between insurance and severance.", """
                Enter the departure-insurance amount shown by the insurer or employer. The app subtracts it from the calculated severance and shows a possible difference for document review.

                Severance is subject to separate retirement-income tax. The tax in the app is an estimate and does not replace the official tax-table calculation. Final payments are generally due within 14 days unless a lawful exception or written agreement applies.
            """, listOf("Use an official document", "The difference is an estimate", "Retirement-income tax is separate"))
        )),
        section("2. Taxes and employment status", "3.3% withholding, insurance deductions and contract-based extras.", listOf(
            topic("3.3% withholding", "A 3.3% deduction alone does not decide whether someone is a freelancer.", """
                The 3.3% withholding consists of 3.0% income tax and 0.3% local income tax. It is often used for contractors and freelancers, but the real working relationship, supervision and schedule still matter.

                The annual income-tax return in May determines the final tax and any refund.
            """, listOf("3.3% is not proof of freelancer status", "Check actual work conditions", "Review the annual return")),
            topic("Insurance deductions by documents", "Pension, health, long-term care and employment insurance depend on your documents.", """
                Insurance deductions depend on your visa, nationality, worker status and payslip. Long-term care is calculated as an additional charge linked to health insurance. Industrial accident insurance is normally paid by the employer, so it is not entered as a worker deduction.

                Under the 3.3% mode, enable insurance items only when they appear on your payslip. Wage income tax is calculated separately, and the app's percentages are estimates.
            """, listOf("Check visa and documents", "Separate employer and worker shares", "Use the payslip")),
            topic("Contract-based extras", "Enter additional payments through the allowance section using the payslip name and amount.", """
                Bonuses, transport payments and other extras are included in gross pay when they appear on the payslip. The allowance section accepts a custom name and amount, so the app does not need separate special fields.

                Compare each extra with the contract and payslip, then enter only the amount actually paid.
            """, listOf("Use the payslip name", "Enter extras manually", "Compare with documents"))
        )),
        section("3. Working hours and premiums", "Base hours, weekly holiday pay and enhanced rates.", listOf(
            topic("Monthly 209-hour reference", "209 hours is a monthly reference based on a 40-hour week and weekly holiday pay.", """
                The 209-hour reference converts a 40-hour week and paid weekly holiday time into a monthly average. Actual hours depend on the calendar, schedule and contract.

                Weekly holiday pay is checked when the worker averages at least 15 hours per week and completes the scheduled workdays. The app displays it separately.
            """, listOf("209 hours is a reference", "Check the actual schedule", "Weekly holiday pay is separate")),
            topic("Overtime, night and holiday work", "The app applies statutory reference multipliers.", """
                The app uses 1.5× for overtime, an additional 0.5× for night work from 22:00 to 06:00, 1.5× for the first eight holiday hours and 2.0× beyond eight.

                Do not enter the same hour in several categories. Check attendance records and the holiday status first.
            """, listOf("Overtime 1.5×", "Night premium 0.5×", "Holiday 1.5× and 2.0×"))
        )),
        section("4. Leave and unpaid wages", "Annual leave, compensation and steps after a payment delay.", listOf(
            topic("Annual paid leave", "Leave days depend on service and attendance.", """
                During the first year, an eligible worker may receive one leave day for each fully worked month, up to 11 days. Later leave depends on service and the applicable rules. Verify unused-leave compensation with HR records.
            """, listOf("Up to 11 days in the first year", "Check service and attendance", "Verify unused leave")),
            topic("Wage delays and assistance", "Keep work evidence and contact the labor service when wages are unpaid.", """
                Keep the employment contract, schedule, payslips, bank transfers and messages with the employer. Final payments are generally due within 14 days. For a delay, contact the Ministry of Employment and Labor at 1350.
            """, listOf("14-day final settlement", "Labor hotline 1350", "Keep payment evidence"))
        )),
        section("5. Visa-specific work rules", "Key checks for E-9, H-2 and F-series visas in Korea.", listOf(
            topic("E-9 visa", "EPS includes separate rules for job changes and insurance.", """
                E-9 work is governed by the Employment Permit System. Verify departure-insurance documents and check current EPS rules before changing workplaces.
            """, listOf("Check insurance documents", "Confirm job-change rules", "Use current EPS guidance")),
            topic("H-2 visa", "Employment requires checking the current training and permitted-industry procedures.", """
                H-2 workers may need employment training, job-seeker registration and work in permitted industries. Confirm the workplace and duties before starting and keep employment documents.
            """, listOf("Check the permitted industry", "Keep work documents", "Review insurance separately")),
            topic("F-series visas", "F-2, F-4, F-5 and F-6 have different work conditions.", """
                F-series visas differ in work rights and restrictions. Check the exact residence status instead of applying one F-series rule to every visa. Taxes, insurance, leave and severance follow the actual employment relationship.
            """, listOf("Check the exact visa", "Review work restrictions", "Keep the contract and residence card copy"))
        ))
    ))

    private fun vietnameseSections(): List<GuideSection> = buildSections(listOf(
        section("1. Trợ cấp thôi việc", "Điều kiện, tiền lương bình quân và tiền lương ngày thông thường.", listOf(
            topic("Khi nào phát sinh quyền", "Kiểm tra thời gian làm việc liên tục và số giờ trung bình mỗi tuần.", """
                Quyền nhận trợ cấp được kiểm tra khi làm việc liên tục cho một người sử dụng lao động từ 1 năm và trung bình ít nhất 15 giờ mỗi tuần trong 4 tuần. Hãy đối chiếu hợp đồng và hồ sơ chấm công.

            """, listOf("Ít nhất 365 ngày liên tục", "Ít nhất 15 giờ mỗi tuần", "Đối chiếu hồ sơ lao động")),
            topic("Cách tính số tiền", "Tính số tiền theo ngày trước rồi nhân với thời gian làm việc.", """
                Tiền lương bình quân ngày = tiền lương của 3 tháng cuối / số ngày theo lịch trong kỳ tính. Tiền thưởng năm và tiền bồi thường ngày phép đủ điều kiện có thể được tính theo tỷ lệ 3/12.

                So sánh tiền lương bình quân ngày với tiền lương ngày thông thường và dùng mức cao hơn.
            """, listOf("Dùng ngày theo lịch thực tế", "So sánh hai mức lương ngày", "Đối chiếu bảng lương")),
            topic("Bảo hiểm xuất cảnh và quyết toán", "Ứng dụng hiển thị khoản chênh lệch dự kiến cho E-9 và H-2.", """
                Nhập số tiền bảo hiểm xuất cảnh trên giấy tờ của công ty bảo hiểm hoặc người sử dụng lao động. Ứng dụng trừ số tiền này khỏi trợ cấp đã tính và hiển thị khoản chênh lệch dự kiến.

                Thuế khi nghỉ việc được tính riêng; số tiền trong ứng dụng chỉ là ước tính. Khoản cuối cùng thường phải trả trong 14 ngày, trừ ngoại lệ hợp pháp hoặc thỏa thuận bằng văn bản.
            """, listOf("Dùng giấy tờ chính thức", "Chênh lệch chỉ là ước tính", "Thuế khi nghỉ việc tính riêng"))
        )),
        section("2. Thuế và hình thức làm việc", "Khấu trừ 3,3%, các khoản khấu trừ bảo hiểm và khoản bổ sung theo hợp đồng.", listOf(
            topic("Khấu trừ 3,3%", "Chỉ khấu trừ 3,3% không đủ để xác định tư cách freelancer.", "3,3% gồm 3,0% thuế thu nhập và 0,3% thuế địa phương. Hãy xem quan hệ làm việc, sự giám sát và lịch làm việc thực tế. Tờ khai thuế hằng năm vào tháng 5 quyết định số thuế cuối cùng.", listOf("3,3% không chứng minh freelancer", "Kiểm tra quan hệ thực tế", "Xem tờ khai tháng 5")),
            topic("Các khoản khấu trừ bảo hiểm chính", "Hưu trí, y tế, chăm sóc dài hạn và bảo hiểm việc làm phụ thuộc vào giấy tờ.", "Khoản khấu trừ bảo hiểm phụ thuộc vào visa, quốc tịch, tư cách người lao động và bảng lương. Bảo hiểm chăm sóc dài hạn được tính như khoản phí bổ sung gắn với bảo hiểm y tế. Bảo hiểm tai nạn lao động thường do doanh nghiệp đóng, vì vậy không nhập khoản này vào phần khấu trừ của người lao động. Với chế độ 3,3%, chỉ bật các khoản bảo hiểm khi chúng thực sự xuất hiện trên bảng lương. Thuế thu nhập từ tiền lương được tính riêng; tỷ lệ trong ứng dụng chỉ dùng để ước tính.", listOf("Kiểm tra visa và giấy tờ", "Phân biệt phần người lao động và doanh nghiệp", "Đối chiếu bảng lương")),
            topic("Khoản bổ sung theo hợp đồng", "Nhập khoản bổ sung trong mục phụ cấp bằng đúng tên và số tiền trên bảng lương.", "Tiền thưởng, tiền đi lại và khoản bổ sung khác được cộng vào tổng thu nhập khi có trên bảng lương. Mục phụ cấp cho phép tự đặt tên và số tiền nên không cần ô riêng cho từng loại. Đối chiếu từng khoản với hợp đồng và bảng lương.", listOf("Dùng tên trên bảng lương", "Nhập khoản bổ sung thủ công", "Đối chiếu giấy tờ"))
        )),
        section("3. Thời giờ làm việc và phụ cấp", "Giờ cơ bản, tiền nghỉ hằng tuần và hệ số tăng thêm.", listOf(
            topic("Mốc 209 giờ mỗi tháng", "209 giờ là mốc tháng dựa trên tuần 40 giờ và tiền nghỉ hằng tuần.", "Mốc 209 giờ là mức trung bình tháng. Số giờ thực tế phụ thuộc lịch, ca và hợp đồng. Tiền nghỉ hằng tuần được kiểm tra theo giờ làm việc và ngày công thực tế.", listOf("209 giờ chỉ là mốc tham khảo", "Kiểm tra lịch thực tế", "Xem tiền nghỉ riêng")),
            topic("Làm thêm, ban đêm và ngày lễ", "Ứng dụng dùng các hệ số tăng thêm theo luật.", "Ứng dụng dùng 1,5 lần cho làm thêm, thêm 0,5 lần cho giờ ban đêm từ 22:00 đến 06:00, 1,5 lần cho 8 giờ đầu ngày lễ và 2,0 lần sau 8 giờ. Không nhập cùng một giờ vào nhiều nhóm.", listOf("Làm thêm 1,5 lần", "Ban đêm thêm 0,5 lần", "Ngày lễ 1,5 lần và 2,0 lần"))
        )),
        section("4. Nghỉ phép và nợ lương", "Nghỉ phép năm, tiền bồi thường và cách xử lý khi chậm trả lương.", listOf(
            topic("Nghỉ phép năm có lương", "Số ngày nghỉ phụ thuộc thời gian làm việc và tỷ lệ đi làm.", "Trong năm đầu, nếu đủ điều kiện, người lao động có thể nhận 1 ngày nghỉ cho mỗi tháng làm đủ, tối đa 11 ngày. Sau đó cần kiểm tra thâm niên và hồ sơ nhân sự.", listOf("Năm đầu tối đa 11 ngày", "Kiểm tra thâm niên", "Đối chiếu tiền phép")),
            topic("Chậm lương và yêu cầu hỗ trợ", "Lưu bằng chứng làm việc và liên hệ cơ quan lao động.", "Lưu hợp đồng, lịch làm việc, bảng lương, giao dịch ngân hàng và tin nhắn. Khi nghỉ việc, khoản cuối cùng thường phải trả trong 14 ngày. Có thể gọi đường dây lao động 1350.", listOf("Thời hạn 14 ngày", "Đường dây 1350", "Giữ bằng chứng thanh toán"))
        )),
        section("5. Quy định theo visa", "Điểm cần kiểm tra với visa E-9, H-2 và nhóm visa F tại Hàn Quốc.", listOf(
            topic("Visa E-9", "Hệ thống EPS có quy định riêng về đổi nơi làm việc và bảo hiểm.", "Công việc theo E-9 chịu sự quản lý của Hệ thống Giấy phép Việc làm. Kiểm tra giấy tờ bảo hiểm và hướng dẫn EPS hiện hành trước khi đổi nơi làm việc.", listOf("Kiểm tra bảo hiểm", "Xác nhận quy định đổi việc", "Dùng hướng dẫn EPS hiện hành")),
            topic("Visa H-2", "Cần kiểm tra thủ tục đào tạo và ngành nghề được phép.", "Người có H-2 có thể phải học đào tạo việc làm, đăng ký tìm việc và làm trong ngành được phép. Xác nhận nơi làm việc và nhiệm vụ trước khi bắt đầu.", listOf("Kiểm tra ngành được phép", "Giữ giấy tờ việc làm", "Xem bảo hiểm riêng")),
            topic("Visa nhóm F", "F-2, F-4, F-5 và F-6 có điều kiện làm việc khác nhau.", "Các visa nhóm F khác nhau về quyền làm việc và giới hạn ngành nghề. Kiểm tra đúng tư cách cư trú; thuế, bảo hiểm, nghỉ phép và trợ cấp dựa trên quan hệ lao động thực tế.", listOf("Kiểm tra đúng visa", "Xem giới hạn công việc", "Giữ hợp đồng và thẻ cư trú"))
        ))
    ))

    private fun uzbekSections(): List<GuideSection> = buildSections(listOf(
        section("1. Ishdan bo‘shash nafaqasi", "Huquq shartlari, o‘rtacha ish haqi va oddiy kunlik ish haqini solishtirish.", listOf(
            topic("Huquq qachon paydo bo‘ladi", "Uzluksiz mehnat muddati va haftalik o‘rtacha ish soatini tekshiring.", """
                Bir ish beruvchida kamida 1 yil uzluksiz ishlash va 4 hafta davomida haftasiga o‘rtacha kamida 15 soat ishlash shartlari tekshiriladi. Shartnoma va ish vaqtini tasdiqlovchi hujjatlarni solishtiring.

            """, listOf("Kamida 365 kun", "Haftasiga kamida 15 soat", "Mehnat hujjatlari bilan solishtiring")),
            topic("Summa qanday hisoblanadi", "Avval kunlik summa, keyin ish stajiga ko‘ra umumiy summa hisoblanadi.", "O‘rtacha kunlik ish haqi = oxirgi 3 oydagi ish haqi / shu davrdagi kalendar kunlari. Yillik bonus va ishlatilmagan ta’til kompensatsiyasi 3/12 ulushida qo‘shilishi mumkin. O‘rtacha va oddiy kunlik ish haqining kattasi qo‘llanadi.", listOf("Haqiqiy kalendar kunlari", "Ikki kunlik ko‘rsatkichni solishtirish", "Hisob varaqasini tekshirish")),
            topic("Chet elga chiqish sug‘urtasi", "E-9 va H-2 vizalarida sug‘urta bilan nafaqa o‘rtasidagi taxminiy farq ko‘rsatiladi.", "Sug‘urta kompaniyasi yoki ish beruvchi hujjatidagi summani kiriting. Ilova uni nafaqadan ayirib, taxminiy farqni ko‘rsatadi. Ishdan bo‘shash solig‘i alohida hisoblanadi va yakuniy to‘lov odatda 14 kun ichida beriladi.", listOf("Rasmiy hujjatdagi summa", "Farq taxminiy", "Soliq alohida hisoblanadi"))
        )),
        section("2. Soliqlar va ish shakli", "3,3 foiz ushlab qolish, sug‘urta ushlanmalari va shartnomadagi qo‘shimcha to‘lovlar.", listOf(
            topic("3,3 foiz ushlab qolish", "Bu ushlab qolishning o‘zi frilanser maqomini belgilamaydi.", "3,3 foiz 3,0 foiz daromad solig‘i va 0,3 foiz mahalliy soliqdan iborat. Shartnoma nomiga emas, haqiqiy nazorat, jadval va ish munosabatlariga qarang. Yakuniy soliq yillik deklaratsiyada belgilanadi.", listOf("3,3 foiz frilanserlik isboti emas", "Haqiqiy ishni tekshirish", "Yillik deklaratsiyani ko‘rish")),
            topic("Asosiy sug‘urta ushlanmalari", "Pensiya, tibbiy, uzoq muddatli parvarish va bandlik sug‘urtasi hujjatlarga bog‘liq.", "Sug‘urta ushlanmalari viza, fuqarolik, xodim maqomi va hisob varaqasiga bog‘liq. Uzoq muddatli parvarish sug‘urtasi tibbiy sug‘urta bilan bog‘liq qo‘shimcha to‘lov sifatida hisoblanadi. Ishlab chiqarishdagi baxtsiz hodisalar sug‘urtasini odatda ish beruvchi to‘laydi, shuning uchun uni xodim ushlanmasi sifatida kiritmang. 3,3 foiz rejimida sug‘urta bandlarini faqat hisob varaqasida ko‘rsatilgan bo‘lsa yoqing. Ish haqi solig‘i alohida hisoblanadi; ilovadagi stavkalar taxminiy.", listOf("Viza va hujjatlarni tekshirish", "Ish beruvchi va xodim ulushlarini ajratish", "Hisob varaqasi bilan solishtirish")),
            topic("Shartnomadagi qo‘shimcha to‘lovlar", "Qo‘shimcha summani hisob varaqasidagi nomi va miqdori bilan nafaqalar bo‘limiga kiriting.", "Bonus, yo‘l puli va boshqa qo‘shimcha to‘lovlar hisob varaqasida ko‘rsatilsa, umumiy hisobga qo‘shiladi. Nafaqalar bo‘limida nom va summani o‘zingiz kiritishingiz mumkin, har bir tur uchun alohida maydon kerak emas. Har bir summani shartnoma va hisob varaqasi bilan solishtiring.", listOf("Hisob varaqasidagi nomdan foydalanish", "Qo‘shimcha summani qo‘lda kiritish", "Hujjatlar bilan solishtirish"))
        )),
        section("3. Ish vaqti va ustamalar", "Asosiy soatlar, haftalik dam olish haqi va oshirilgan koeffitsiyentlar.", listOf(
            topic("Oylik 209 soat mezoni", "209 soat 40 soatlik hafta va haftalik dam olish haqiga asoslangan oylik mezondir.", "209 soat o‘rtacha oylik mezondir. Haqiqiy soatlar taqvim, smena va shartnomaga bog‘liq. Haftalik dam olish haqi ish soati va davomat shartlari asosida tekshiriladi.", listOf("209 soat mezon xolos", "Haqiqiy jadvalni tekshirish", "Dam olish haqini alohida ko‘rish")),
            topic("Qo‘shimcha, tungi va bayram ishi", "Ilova oshirilgan koeffitsiyentlarni qo‘llaydi.", "Ilova qo‘shimcha ishga 1,5 baravar, 22:00–06:00 tungi ishga qo‘shimcha 0,5 baravar, bayramning dastlabki 8 soatiga 1,5 va undan keyingi soatlarga 2,0 baravarni qo‘llaydi. Bir soatni bir necha guruhga kiritmang.", listOf("Qo‘shimcha ish 1,5×", "Tungi qo‘shimcha 0,5×", "Bayram 1,5× va 2,0×"))
        )),
        section("4. Ta’til va to‘lanmagan ish haqi", "Yillik ta’til, kompensatsiya va ish haqi kechiksa bajariladigan ishlar.", listOf(
            topic("Yillik haq to‘lanadigan ta’til", "Ta’til kunlari staj va davomatga bog‘liq.", "Birinchi yilda shartlar bajarilsa, to‘liq ishlangan har oy uchun 1 kun, ko‘pi bilan 11 kun berilishi mumkin. Keyingi kunlar staj va amaldagi qoidalarga bog‘liq. Foydalanilmagan ta’tilni kadrlar hujjatlari bilan tekshiring.", listOf("Birinchi yil 11 kungacha", "Staj va davomatni tekshirish", "Foydalanilmagan ta’tilni solishtirish")),
            topic("Ish haqi kechikishi", "Dalillarni saqlang va ish haqi to‘lanmasa mehnat xizmatiga murojaat qiling.", "Shartnoma, jadval, hisob varaqasi, bank o‘tkazmalari va yozishmalarni saqlang. Yakuniy to‘lovlar odatda 14 kun ichida beriladi. Koreyada mehnat bo‘yicha 1350 raqamiga murojaat qilish mumkin.", listOf("14 kunlik yakuniy hisob", "Mehnat liniyasi 1350", "To‘lov dalillarini saqlash"))
        )),
        section("5. Vizaga oid mehnat qoidalari", "Koreyada E-9, H-2 va F vizalari bo‘yicha tekshiriladigan masalalar.", listOf(
            topic("E-9 vizasi", "EPS tizimida ish joyini almashtirish va sug‘urta bo‘yicha alohida qoidalar bor.", "E-9 bo‘yicha ish Mehnat ruxsatnomasi tizimi bilan boshqariladi. Ish joyini almashtirishdan oldin sug‘urta hujjatlari va amaldagi EPS qoidalarini tekshiring.", listOf("Sug‘urta hujjatini tekshirish", "Ish joyini almashtirish shartlari", "Amaldagi EPS ma’lumoti")),
            topic("H-2 vizasi", "Ta’lim, ish qidiruvchi ro‘yxati va ruxsat etilgan sohani tekshiring.", "H-2 egalariga ishga tayyorgarlik va ish qidiruvchi sifatida ro‘yxatdan o‘tish talab qilinishi mumkin. Ish boshlashdan oldin joy va vazifa ruxsat etilganini tekshiring.", listOf("Ruxsat etilgan sohani tekshirish", "Ish hujjatlarini saqlash", "Sug‘urtani alohida ko‘rish")),
            topic("F turidagi vizalar", "F-2, F-4, F-5 va F-6 shartlari bir xil emas.", "F vizalari ish huquqi va cheklovlari bo‘yicha farq qiladi. Aniq yashash maqomini tekshiring; soliq, sug‘urta, ta’til va nafaqa haqiqiy mehnat munosabatlariga bog‘liq.", listOf("Aniq viza maqomi", "Ish cheklovlari", "Shartnoma nusxasi"))
        ))
    ))

    private fun kyrgyzSections(): List<GuideSection> = buildSections(listOf(
        section("1. Жумуштан чыгуу жөлөкпулу", "Укук шарттары, орточо эмгек акы жана күнүмдүк кадимки акы.", listOf(
            topic("Укук качан пайда болот", "Үзгүлтүксүз иш стажын жана жумалык орточо саатты текшериңиз.", """
                Бир иш берүүчүдө кеминде 1 жыл үзгүлтүксүз иштөө жана 4 жумада жумасына орточо кеминде 15 саат иштөө шарттары текшерилет. Келишимди жана иш убактысынын документтерин салыштырыңыз.

            """, listOf("Кеминде 365 күн", "Жумасына кеминде 15 саат", "Эмгек документтери менен салыштыруу")),
            topic("Сумма кантип эсептелет", "Алгач күнүмдүк сумма, андан кийин иш стажына жараша жалпы сумма эсептелет.", "Орточо күнүмдүк эмгек акы = акыркы 3 айдагы эмгек акы / ошол мезгилдеги календардык күндөр. Жылдык бонус жана пайдаланылбаган өргүү компенсациясы 3/12 үлүшү менен кошулушу мүмкүн. Орточо жана кадимки күнүмдүк акыны салыштырып, чоңу колдонулат.", listOf("Чыныгы календардык күндөр", "Күнүмдүк эки көрсөткүчтү салыштыруу", "Эсеп барагын текшерүү")),
            topic("Чыгып кетүү камсыздандыруусу", "E-9 жана H-2 визаларында камсыздандыруу менен жөлөкпулдун болжолдуу айырмасы көрсөтүлөт.", "Камсыздандыруу компаниясынын же иш берүүчүнүн документиндеги сумманы киргизиңиз. Колдонмо аны жөлөкпулдан алып, болжолдуу айырманы көрсөтөт. Жумуштан чыгуу салыгы өзүнчө эсептелет, акыркы төлөм адатта 14 күндө берилет.", listOf("Расмий документтеги сумма", "Айырма болжолдуу", "Салык өзүнчө эсептелет"))
        )),
        section("2. Салыктар жана иш формасы", "3,3 пайыздык кармоо, камсыздандыруу кармоолору жана келишимдеги кошумча төлөмдөр.", listOf(
            topic("3,3 пайыздык кармоо", "Бул кармоо өзү фрилансер макамын аныктабайт.", "3,3 пайыз 3,0 пайыз киреше салыгынан жана 0,3 пайыз жергиликтүү салыктан турат. Келишимдин аталышына гана эмес, чыныгы көзөмөлгө жана иш графигине көңүл буруңуз. Акыркы салык жылдык декларацияда аныкталат.", listOf("3,3 пайыз фрилансерлик далили эмес", "Чыныгы ишти текшерүү", "Жылдык декларацияны көрүү")),
            topic("Негизги камсыздандыруу кармоолору", "Пенсия, медициналык, узак мөөнөттүү кам көрүү жана жумуш камсыздандыруусу документтерге жараша болот.", "Камсыздандыруу кармоолору визага, жарандыкка, кызматкер макамына жана эсеп барагына жараша болот. Узак мөөнөттүү кам көрүү камсыздандыруусу медициналык камсыздандырууга байланышкан кошумча төлөм катары эсептелет. Өндүрүштөгү кырсык камсыздандыруусун адатта иш берүүчү төлөйт, ошондуктан аны кызматкердин кармоосу катары киргизбеңиз. 3,3 пайыздык режимде камсыздандыруу бөлүктөрүн эсеп барагында көрсөтүлгөндө гана күйгүзүңүз. Эмгек акы салыгы өзүнчө эсептелет; колдонмодогу чендер болжолдуу.", listOf("Виза жана документтерди текшерүү", "Иш берүүчү менен кызматкер үлүшүн бөлүү", "Эсеп барагы менен салыштыруу")),
            topic("Келишимдеги кошумча төлөмдөр", "Кошумча сумманы эсеп барагындагы аталышы жана өлчөмү менен кошумча төлөмдөр бөлүмүнө киргизиңиз.", "Бонус, жол акысы жана башка кошумча төлөмдөр эсеп барагында көрсөтүлсө жалпы суммага кошулат. Кошумча төлөмдөр бөлүмүндө аталыш менен сумманы өзүңүз киргизе аласыз, ар бир түр үчүн өзүнчө талаа керек эмес. Ар бир сумманы келишим жана эсеп барагы менен салыштырыңыз.", listOf("Эсеп барагындагы аталышты колдонуу", "Кошумча сумманы кол менен киргизүү", "Документ менен салыштыруу"))
        )),
        section("3. Иш убактысы жана кошумча акылар", "Негизги сааттар, жумалык эс алуу акысы жана жогорулатылган коэффициенттер.", listOf(
            topic("Айлык 209 сааттык көрсөткүч", "209 саат 40 сааттык жумага жана жумалык эс алуу акысына негизделген айлык көрсөткүч.", "209 саат орточо айлык көрсөткүч. Чыныгы сааттар календарга, нөөмөткө жана келишимге жараша болот. Жумалык эс алуу акысы иш сааты жана катышуу шарттары менен текшерилет.", listOf("209 саат көрсөткүч гана", "Чыныгы графикти текшерүү", "Эс алуу акысын өзүнчө көрүү")),
            topic("Кошумча, түнкү жана майрамдык иш", "Колдонмо жогорулатылган коэффициенттерди колдонот.", "Колдонмо кошумча ишке 1,5 эсе, 22:00–06:00 түнкү ишке кошумча 0,5 эсе, майрамдын биринчи 8 саатына 1,5 жана андан кийинки сааттарына 2,0 эсе колдонот. Бир саатты бир нече топко киргизбеңиз.", listOf("Кошумча иш 1,5×", "Түнкү кошумча 0,5×", "Майрам 1,5× жана 2,0×"))
        )),
        section("4. Өргүү жана төлөнбөгөн эмгек акы", "Жылдык өргүү, компенсация жана айлык кечиккендеги аракеттер.", listOf(
            topic("Жылдык акы төлөнүүчү өргүү", "Өргүү күндөрү стажга жана катышууга жараша болот.", "Биринчи жылы шарттар аткарылса, толук иштеген ар бир ай үчүн 1 күн, эң көп 11 күн берилиши мүмкүн. Кийинки күндөр стажга жана учурдагы эрежелерге жараша болот. Пайдаланылбаган өргүүнү кадр документтери менен салыштырыңыз.", listOf("Биринчи жыл 11 күнгө чейин", "Стажды жана катышууну текшерүү", "Пайдаланылбаган өргүүнү салыштыруу")),
            topic("Айлык кечиккенде", "Далилдерди сактап, эмгек акы төлөнбөсө эмгек кызматына кайрылыңыз.", "Келишимди, графикти, эсеп барагын, банк которууларын жана каттарды сактаңыз. Акыркы төлөм адатта 14 күндө берилет. Кореяда эмгек боюнча 1350 номерине кайрылса болот.", listOf("14 күндүк акыркы эсеп", "Эмгек линиясы 1350", "Төлөм далилдерин сактоо"))
        )),
        section("5. Визага байланыштуу эмгек эрежелери", "Кореядагы E-9, H-2 жана F визалары боюнча негизги текшерүүлөр.", listOf(
            topic("E-9 визасы", "EPS системасында иш ордун өзгөртүү жана камсыздандыруу боюнча өзүнчө эрежелер бар.", "E-9 боюнча ишке уруксат берүү системасы колдонулат. Иш ордун өзгөртүүдөн мурда камсыздандыруу документтерин жана учурдагы EPS эрежелерин текшериңиз.", listOf("Камсыздандыруу документин текшерүү", "Иш ордун өзгөртүү шарттары", "Учурдагы EPS маалыматы")),
            topic("H-2 визасы", "Окутуу, жумуш издөө каттоосу жана уруксат берилген тармак текшерилет.", "H-2 ээсинен жумушка даярдоо жана жумуш издөөчү катары катталуу талап кылынышы мүмкүн. Иш баштоодон мурда орун жана милдет уруксат берилгенин текшериңиз.", listOf("Уруксат берилген тармак", "Жумуш документтерин сактоо", "Камсыздандырууну өзүнчө кароо")),
            topic("F сериясындагы визалар", "F-2, F-4, F-5 жана F-6 шарттары бирдей эмес.", "F визалары иш укугу жана чектөөлөрү боюнча айырмаланат. Так жашоо макамын текшериңиз; салык, камсыздандыруу, өргүү жана жөлөкпул чыныгы эмгек мамилесине жараша болот.", listOf("Так виза макамын текшерүү", "Иш чектөөлөрү", "Келишимдин көчүрмөсү"))
        ))
    ))

    private fun kazakhSections(): List<GuideSection> = buildSections(listOf(
        section("1. Жұмыстан шығу төлемі", "Құқық шарттары, орташа жалақы және қалыпты күндік жалақыны салыстыру.", listOf(
            topic("Құқық қашан пайда болады", "Үздіксіз еңбек өтілі мен апталық орташа жұмыс уақытын тексеріңіз.", """
                Бір жұмыс берушіде кемінде 1 жыл үздіксіз жұмыс істеу және 4 апта ішінде аптасына орташа кемінде 15 сағат жұмыс істеу шарттары тексеріледі. Шарт пен жұмыс уақытының құжаттарын салыстырыңыз.

                Есепті жалақы парағымен және жұмыс берушінің құжаттарымен жеке салыстырыңыз.
            """, listOf("Кемінде 365 күн", "Аптасына кемінде 15 сағат", "Еңбек құжаттарымен салыстыру")),
            topic("Сома қалай есептеледі", "Алдымен күндік сома, кейін еңбек өтілі бойынша жалпы сома есептеледі.", "Орташа күндік жалақы = соңғы 3 айдағы жалақы / есептік кезеңдегі күнтізбелік күндер саны. Жылдық бонус пен пайдаланылмаған демалыс өтемі шартқа сай болса, 3/12 үлесімен қосылуы мүмкін. Орташа күндік жалақы мен қалыпты күндік жалақының үлкені қолданылады.", listOf("Нақты күнтізбелік күндерді енгізу", "Екі күндік көрсеткішті салыстыру", "Есеп парағын тексеру")),
            topic("Шығу сақтандыруы", "E-9 және H-2 визаларында сақтандыру мен төлем арасындағы болжамды айырма көрсетіледі.", "Сақтандыру компаниясы немесе жұмыс беруші құжатында көрсетілген соманы енгізіңіз. Қолданба оны жұмыстан шығу төлемінен шегеріп, болжамды айырманы көрсетеді. Жұмыстан шығу табысына салынатын салық бөлек есептеледі, соңғы төлем әдетте 14 күн ішінде жасалады.", listOf("Ресми құжаттағы соманы қолданыңыз", "Айырма алдын ала есептеледі", "Салық бөлек есептеледі"))
        )),
        section("2. Салықтар және жұмыс түрі", "3,3% ұсталымы, сақтандыру ұсталымдары және шарттағы қосымша төлемдер.", listOf(
            topic("3,3% ұсталымы", "Бұл ұсталымның өзі фрилансер мәртебесін анықтамайды.", "3,3% табыс салығының 3,0% және жергілікті салықтың 0,3% бөлігінен тұрады. Шарт атауына ғана емес, нақты бақылауға, кестеге және еңбек қатынасына қараңыз. Соңғы салық жылдық декларацияда анықталады.", listOf("3,3% фрилансер екенін дәлелдемейді", "Нақты жұмыс жағдайын тексеру", "Жылдық декларацияны қарау")),
            topic("Негізгі сақтандыру ұсталымдары", "Зейнетақы, медициналық, ұзақ мерзімді күтім және жұмыспен қамту сақтандыруы құжатқа байланысты.", "Сақтандыру ұсталымдары визаға, азаматтыққа, жұмысшы мәртебесіне және есеп парағына байланысты. Ұзақ мерзімді күтім сақтандыруы медициналық сақтандыруға байланысты қосымша төлем ретінде есептеледі. Өндірістегі жазатайым оқиғалар сақтандыруын әдетте жұмыс беруші төлейді, сондықтан оны жұмысшы ұсталымы ретінде енгізбеңіз.", listOf("Виза мен құжаттарды тексеру", "Жұмыс беруші мен жұмысшы үлесін ажырату", "Есеп парағымен салыстыру")),
            topic("Шарттағы қосымша төлемдер", "Қосымша соманы есеп парағындағы атауы мен мөлшері бойынша енгізіңіз.", "Бонус, жол ақысы және басқа қосымша төлемдер есеп парағында көрсетілсе, жалпы есепке қосылады. Қосымша төлемдер бөлімінде атауы мен сомасын өзіңіз енгізе аласыз. Әр соманы шартпен және есеп парағымен салыстырыңыз.", listOf("Есеп парағындағы атауды қолданыңыз", "Соманы қолмен енгізіңіз", "Құжаттармен салыстырыңыз"))
        )),
        section("3. Жұмыс уақыты және қосымша төлемдер", "Негізгі сағаттар, апталық ақылы демалыс және жоғарылатылған коэффициенттер.", listOf(
            topic("Айлық 209 сағат көрсеткіші", "209 сағат 40 сағаттық апта мен апталық ақылы демалысқа негізделген айлық бағдар болып табылады.", "209 сағат орташа айлық бағдар ғана. Нақты сағаттар күнтізбеге, ауысымға және шартқа байланысты. Апталық ақылы демалыс төлемін нақты жұмыс уақыты мен қатысу шарттары бойынша тексеріңіз.", listOf("209 сағат тек бағдар", "Нақты кестені тексеру", "Демалыс төлемін бөлек қарау")),
            topic("Үстеме, түнгі және мерекедегі жұмыс", "Қолданба жұмыс ережелеріне сай жоғарылатылған коэффициенттерді қолданады.", "Қолданба үстеме жұмысқа 1.5x, 22:00–06:00 аралығындағы түнгі жұмысқа қосымша 0.5x, мерекедегі алғашқы 8 сағатқа 1.5x және 8 сағаттан асқан уақытқа 2.0x қолданады. Бір сағатты бірнеше санатқа қайталап енгізбеңіз.", listOf("Үстеме жұмыс 1.5x", "Түнгі қосымша 0.5x", "Мереке 1.5x және 2.0x"))
        )),
        section("4. Демалыс және төленбеген жалақы", "Жыл сайынғы демалыс, өтемақы және жалақы кешіккен кездегі әрекеттер.", listOf(
            topic("Ақылы жыл сайынғы демалыс", "Демалыс күндері еңбек өтілі мен жұмысқа қатысуға байланысты.", "Бірінші жылы шарттар орындалса, толық жұмыс істеген әр айға 1 күн, ең көбі 11 күн берілуі мүмкін. Кейінгі демалыс еңбек өтілі мен қолданыстағы ережелерге байланысты. Пайдаланылмаған демалысты кадр құжаттарымен салыстырыңыз.", listOf("Бірінші жылы 11 күнге дейін", "Еңбек өтілі мен қатысуды тексеру", "Пайдаланылмаған демалысты салыстыру")),
            topic("Жалақы кешіккенде", "Дәлелдерді сақтап, жалақы төленбесе еңбек қызметіне жүгініңіз.", "Шартты, кестені, есеп парағын, банк аударымдарын және хабарламаларды сақтаңыз. Соңғы төлем әдетте 14 күн ішінде жасалады. Кореядағы еңбек мәселелері бойынша 1350 нөміріне хабарласуға болады.", listOf("14 күн ішінде соңғы есеп", "Еңбек желісі 1350", "Төлем дәлелдерін сақтау"))
        )),
        section("5. Визаға байланысты жұмыс ережелері", "Кореядағы E-9, H-2 және F сериялы визалар бойынша негізгі тексерулер.", listOf(
            topic("E-9 визасы", "EPS жүйесінде жұмыс орнын ауыстыру мен сақтандыруға қатысты жеке ережелер бар.", "E-9 жұмысы шетелдіктерді жұмысқа орналастыруға рұқсат беру жүйесімен реттеледі. Жұмыс орнын ауыстырмас бұрын шығу сақтандыруы құжаттарын және қолданыстағы EPS ережелерін тексеріңіз.", listOf("Сақтандыру құжатын тексеру", "Жұмыс ауыстыру шарттарын растау", "Қолданыстағы EPS нұсқаулығын пайдалану")),
            topic("H-2 визасы", "Оқыту, жұмыс іздеуші ретінде тіркелу және рұқсат етілген саланы тексеру керек.", "H-2 иесінен еңбекке даярлау курсы мен жұмыс іздеуші ретінде тіркелу талап етілуі мүмкін. Жұмысты бастамас бұрын орын мен міндеттің рұқсат етілгенін тексеріңіз.", listOf("Рұқсат етілген саланы тексеру", "Жұмыс құжаттарын сақтау", "Сақтандыруды бөлек қарау")),
            topic("F сериялы визалар", "F-2, F-4, F-5 және F-6 шарттары бірдей емес.", "F сериялы визалар жұмыс істеу құқығы мен шектеулері бойынша ерекшеленеді. Нақты тұру мәртебесін тексеріңіз; салық, сақтандыру, демалыс және жұмыстан шығу төлемі нақты еңбек қатынасына байланысты.", listOf("Нақты виза мәртебесін тексеру", "Жұмыс шектеулерін қарау", "Шарт пен тұру картасының көшірмесін сақтау"))
        ))
    ))

    private fun thaiSections(): List<GuideSection> = buildSections(listOf(
        section("1. เงินชดเชยเมื่อออกจากงาน", "เงื่อนไขสิทธิ์ ค่าจ้างเฉลี่ย และการเปรียบเทียบค่าจ้างรายวันปกติ", listOf(
            topic("สิทธิ์เกิดขึ้นเมื่อใด", "ตรวจสอบระยะเวลาทำงานต่อเนื่องและชั่วโมงทำงานเฉลี่ยต่อสัปดาห์", """
                ตรวจสอบการทำงานต่อเนื่องกับนายจ้างรายเดียวอย่างน้อย 1 ปี และชั่วโมงทำงานเฉลี่ยอย่างน้อย 15 ชั่วโมงต่อสัปดาห์ในช่วง 4 สัปดาห์ โดยใช้สัญญาและบันทึกการทำงาน

            """, listOf("ทำงานต่อเนื่องอย่างน้อย 365 วัน", "อย่างน้อย 15 ชั่วโมงต่อสัปดาห์", "ตรวจสอบกับเอกสารการทำงาน")),
            topic("คำนวณจำนวนเงินอย่างไร", "คำนวณจำนวนเงินรายวันก่อน แล้วคูณตามระยะเวลาทำงาน", "ค่าจ้างเฉลี่ยรายวัน = ค่าจ้าง 3 เดือนสุดท้าย / จำนวนวันตามปฏิทินในช่วงคำนวณ โบนัสประจำปีและค่าชดเชยวันลาที่เข้าเกณฑ์อาจคิดในสัดส่วน 3/12 จากนั้นเปรียบเทียบกับค่าจ้างรายวันปกติและใช้ค่าที่สูงกว่า", listOf("ใช้จำนวนวันตามปฏิทินจริง", "เปรียบเทียบค่าจ้างรายวัน", "ตรวจสอบกับใบจ่ายเงิน")),
            topic("ประกันเมื่อเดินทางออกนอกประเทศ", "แอปแสดงส่วนต่างโดยประมาณสำหรับผู้ถือวีซ่า E-9 และ H-2", "กรอกจำนวนเงินประกันจากเอกสารของบริษัทประกันหรือนายจ้าง แอปจะหักออกจากเงินชดเชยและแสดงส่วนต่างโดยประมาณ ภาษีเงินได้จากเงินชดเชยคำนวณแยก และเงินงวดสุดท้ายโดยทั่วไปต้องจ่ายภายใน 14 วัน", listOf("ใช้เอกสารทางการ", "ส่วนต่างเป็นค่าประมาณ", "ภาษีคำนวณแยก"))
        )),
        section("2. ภาษีและรูปแบบการจ้างงาน", "การหัก 3.3% รายการหักประกัน และเงินเพิ่มตามสัญญา", listOf(
            topic("การหัก 3.3%", "การหักนี้เพียงอย่างเดียวไม่กำหนดว่าเป็นฟรีแลนซ์", "3.3% ประกอบด้วยภาษีเงินได้ 3.0% และภาษีท้องถิ่น 0.3% ต้องตรวจสอบการควบคุมงาน ตารางเวลา และความสัมพันธ์จริง ไม่ใช่ดูจากชื่อสัญญาเท่านั้น การยื่นภาษีประจำปีจะกำหนดภาษีสุดท้าย", listOf("3.3% ไม่ใช่หลักฐานว่าเป็นฟรีแลนซ์", "ตรวจสอบงานจริง", "ตรวจสอบการยื่นภาษี")),
            topic("รายการหักประกันหลัก", "ประกันบำนาญ สุขภาพ การดูแลระยะยาว และการจ้างงานขึ้นอยู่กับเอกสาร", "รายการหักประกันขึ้นอยู่กับวีซ่า สัญชาติ สถานะการทำงาน และสลิปเงินเดือน ประกันการดูแลระยะยาวคำนวณเป็นเงินสมทบเพิ่มเติมที่เชื่อมโยงกับประกันสุขภาพ ประกันอุบัติเหตุจากการทำงานโดยทั่วไปนายจ้างเป็นผู้จ่าย จึงไม่ต้องกรอกเป็นรายการหักของลูกจ้าง ในโหมด 3.3% ให้เปิดรายการประกันเฉพาะเมื่อมีแสดงในสลิปเงินเดือน ภาษีเงินได้จากค่าจ้างคำนวณแยกต่างหาก และอัตราในแอปใช้สำหรับประมาณการ", listOf("ตรวจสอบวีซ่าและเอกสาร", "แยกส่วนลูกจ้างและนายจ้าง", "ตรวจสอบกับสลิปเงินเดือน")),
            topic("เงินเพิ่มตามสัญญา", "กรอกเงินเพิ่มในส่วนเบี้ยเลี้ยงด้วยชื่อและจำนวนตามใบจ่ายเงิน", "โบนัส ค่าเดินทาง และเงินเพิ่มอื่น ๆ จะรวมในยอดรับเมื่อมีระบุในใบจ่ายเงิน ส่วนเบี้ยเลี้ยงให้ตั้งชื่อและจำนวนเองได้ จึงไม่ต้องมีช่องพิเศษแยกตามประเภท ตรวจสอบแต่ละรายการกับสัญญาและใบจ่ายเงิน", listOf("ใช้ชื่อจากใบจ่ายเงิน", "กรอกเงินเพิ่มเอง", "ตรวจสอบกับเอกสาร"))
        )),
        section("3. เวลาทำงานและค่าตอบแทนเพิ่ม", "ชั่วโมงพื้นฐาน ค่าหยุดประจำสัปดาห์ และอัตราเพิ่มตามกฎหมาย", listOf(
            topic("เกณฑ์ 209 ชั่วโมงต่อเดือน", "209 ชั่วโมงเป็นค่าอ้างอิงจากสัปดาห์ 40 ชั่วโมงและค่าหยุดประจำสัปดาห์", "209 ชั่วโมงเป็นค่าเฉลี่ยรายเดือน ชั่วโมงจริงขึ้นอยู่กับปฏิทิน ตารางกะ และสัญญา ค่าหยุดประจำสัปดาห์ต้องตรวจสอบจากชั่วโมงและการมาทำงานจริง", listOf("เป็นค่าอ้างอิง", "ตรวจสอบตารางจริง", "แสดงค่าหยุดแยกต่างหาก")),
            topic("ทำงานล่วงเวลา กลางคืน และวันหยุด", "แอปใช้ตัวคูณเพิ่มตามกฎหมาย", "แอปใช้ 1.5 เท่าสำหรับล่วงเวลา เพิ่ม 0.5 เท่าสำหรับกลางคืน 22:00–06:00 ใช้ 1.5 เท่าสำหรับ 8 ชั่วโมงแรกของวันหยุด และ 2.0 เท่าสำหรับชั่วโมงเกิน 8 ชั่วโมง อย่ากรอกชั่วโมงเดียวกันซ้ำหลายกลุ่ม", listOf("ล่วงเวลา 1.5 เท่า", "กลางคืนเพิ่ม 0.5 เท่า", "วันหยุด 1.5 และ 2.0 เท่า"))
        )),
        section("4. วันลาและค่าจ้างค้างจ่าย", "วันลาประจำปี เงินชดเชย และการดำเนินการเมื่อจ่ายค่าจ้างล่าช้า", listOf(
            topic("วันลาประจำปีมีค่าจ้าง", "จำนวนวันลาขึ้นอยู่กับอายุงานและการมาทำงาน", "ปีแรก หากเข้าเงื่อนไขอาจได้วันลาวันละ 1 วันต่อเดือนที่ทำงานครบ สูงสุด 11 วัน หลังจากนั้นให้ตรวจสอบอายุงานและเอกสารฝ่ายบุคคล", listOf("ปีแรกสูงสุด 11 วัน", "ตรวจสอบอายุงาน", "ตรวจสอบเงินวันลาที่เหลือ")),
            topic("ค่าจ้างล่าช้า", "เก็บหลักฐานและติดต่อหน่วยงานแรงงานเมื่อไม่ได้รับค่าจ้าง", "เก็บสัญญา ตารางงาน ใบจ่ายเงิน รายการธนาคาร และข้อความกับนายจ้าง เงินงวดสุดท้ายโดยทั่วไปต้องจ่ายภายใน 14 วัน และสามารถติดต่อสายด่วนแรงงาน 1350", listOf("ระยะเวลา 14 วัน", "สายด่วน 1350", "เก็บหลักฐานการจ่ายเงิน"))
        )),
        section("5. กฎการทำงานตามวีซ่า", "ประเด็นที่ต้องตรวจสอบสำหรับวีซ่า E-9, H-2 และกลุ่ม F ในเกาหลี", listOf(
            topic("วีซ่า E-9", "ระบบ EPS มีกฎเฉพาะเรื่องเปลี่ยนสถานที่ทำงานและประกัน", "งาน E-9 อยู่ภายใต้ระบบใบอนุญาตทำงาน ตรวจสอบเอกสารประกันและกฎ EPS ปัจจุบันก่อนเปลี่ยนสถานที่ทำงาน", listOf("ตรวจสอบเอกสารประกัน", "ยืนยันกฎการเปลี่ยนงาน", "ใช้ข้อมูล EPS ปัจจุบัน")),
            topic("วีซ่า H-2", "ตรวจสอบการอบรม การลงทะเบียนหางาน และอุตสาหกรรมที่อนุญาต", "ผู้ถือ H-2 อาจต้องผ่านการอบรมและลงทะเบียนหางาน ตรวจสอบสถานที่และหน้าที่ว่าอยู่ในขอบเขตก่อนเริ่มงาน และเก็บเอกสารการทำงานไว้", listOf("ตรวจสอบอุตสาหกรรม", "เก็บเอกสารงาน", "ตรวจสอบประกันแยกต่างหาก")),
            topic("วีซ่ากลุ่ม F", "F-2, F-4, F-5 และ F-6 มีเงื่อนไขการทำงานต่างกัน", "วีซ่ากลุ่ม F มีสิทธิและข้อจำกัดต่างกัน ตรวจสอบสถานะของตนเองโดยตรง ภาษี ประกัน วันลา และเงินชดเชยขึ้นอยู่กับความสัมพันธ์การจ้างงานจริง", listOf("ตรวจสอบวีซ่าที่ถูกต้อง", "ดูข้อจำกัดงาน", "เก็บสัญญาและบัตรพำนัก"))
        ))
    ))

    private fun filipinoSections(): List<GuideSection> = buildSections(listOf(
        section("1. Severance pay", "Mga kondisyon, karaniwang sahod bawat araw, at paghahambing sa regular na daily wage.", listOf(
            topic("Kailan nagkakaroon ng karapatan", "Suriin ang tuloy-tuloy na serbisyo at karaniwang oras bawat linggo.", """
                Sinusuri ang hindi bababa sa isang taong tuloy-tuloy na trabaho sa iisang employer at average na hindi bababa sa 15 oras bawat linggo sa loob ng 4 na linggo. Ihambing ang kontrata at attendance records.

            """, listOf("Hindi bababa sa 365 araw", "Hindi bababa sa 15 oras bawat linggo", "Suriin ayon sa mga dokumento sa trabaho")),
            topic("Paano kinakalkula ang halaga", "Kinakalkula muna ang halaga bawat araw at saka inaayon sa haba ng serbisyo.", "Average daily wage = sahod sa huling 3 buwan / bilang ng calendar days. Ang kwalipikadong annual bonus at bayad sa hindi nagamit na leave ay maaaring isama sa bahaging 3/12. Ihambing sa ordinary daily wage at gamitin ang mas mataas.", listOf("Gamitin ang calendar days", "Ihambing ang daily wages", "Suriin ang payslip")),
            topic("Departure insurance at final settlement", "Ipinapakita ang tinatayang diperensya para sa E-9 at H-2.", "Ilagay ang halaga ng departure insurance mula sa dokumento ng insurer o employer. Ibabawas ito ng app sa severance at ipapakita ang tinatayang diperensya. Hiwalay ang retirement-income tax at karaniwang 14 araw ang final-settlement period.", listOf("Gamitin ang opisyal na dokumento", "Tantiya ang diperensya", "Hiwalay ang retirement-income tax"))
        )),
        section("2. Buwis at uri ng trabaho", "3.3% withholding, mga kaltas sa insurance, at dagdag na bayad ayon sa kontrata.", listOf(
            topic("3.3% withholding", "Hindi sapat ang deduction na ito upang sabihing freelancer ang isang tao.", "Ang 3.3% ay 3.0% income tax at 0.3% local income tax. Suriin ang aktuwal na supervision, schedule at working relationship, hindi lamang ang label sa kontrata. Ang taunang return ang tumutukoy sa final tax.", listOf("Hindi patunay ng freelancer status", "Suriin ang aktuwal na trabaho", "Tingnan ang annual return")),
            topic("Mga pangunahing kaltas sa insurance", "Ang pension, health, long-term care at employment insurance ay nakabatay sa iyong mga dokumento.", "Ang mga kaltas sa insurance ay nakabatay sa visa, nasyonalidad, katayuan bilang manggagawa at payslip. Ang long-term care ay karagdagang singil na kaugnay ng health insurance. Ang insurance para sa aksidente sa trabaho ay karaniwang binabayaran ng employer, kaya huwag itong ilagay bilang kaltas ng manggagawa. Sa 3.3% mode, i-on lamang ang insurance kapag nakalista ito sa payslip. Hiwalay ang wage-income tax; pagtatantiya lamang ang mga rate sa app.", listOf("Suriin ang visa at mga dokumento", "Paghiwalayin ang employer at worker share", "Gamitin ang payslip")),
            topic("Dagdag na bayad ayon sa kontrata", "Ilagay ang dagdag na bayad sa allowance gamit ang pangalan at halaga sa payslip.", "Kasama sa kabuuang kita ang bonus, pamasahe at iba pang dagdag na bayad kapag nasa payslip. Maaaring gumawa ng sariling pangalan at halaga sa allowance section kaya walang hiwalay na field para sa bawat uri. Ihambing ang bawat item sa kontrata at payslip.", listOf("Gamitin ang pangalan sa payslip", "Ilagay nang manu-mano", "Ihambing sa mga dokumento"))
        )),
        section("3. Oras ng trabaho at dagdag bayad", "Basic hours, weekly holiday pay, at mga enhanced rate.", listOf(
            topic("Monthly 209-hour reference", "Ang 209 oras ay buwanang reference mula sa 40-hour week at weekly holiday pay.", "Ang 209 oras ay monthly average reference lamang. Depende ang aktuwal na oras sa calendar, schedule at kontrata. Suriin ang weekly holiday pay ayon sa oras at attendance.", listOf("Reference lamang", "Suriin ang aktuwal na schedule", "Ipakita ang weekly holiday pay nang hiwalay")),
            topic("Overtime, night at holiday work", "Ginagamit ng app ang mga statutory reference multiplier.", "Gumagamit ang app ng 1.5× para sa overtime, dagdag na 0.5× para sa night work 22:00–06:00, 1.5× sa unang 8 holiday hours at 2.0× lampas 8 oras. Huwag ilagay ang parehong oras sa maraming category.", listOf("Overtime 1.5×", "Night premium 0.5×", "Holiday 1.5× at 2.0×"))
        )),
        section("4. Leave at hindi nabayarang sahod", "Annual leave, compensation, at hakbang kapag naantala ang sahod.", listOf(
            topic("Annual paid leave", "Nakasalalay ang leave sa haba ng serbisyo at attendance.", "Sa unang taon, ang kwalipikadong worker ay maaaring makatanggap ng 1 araw sa bawat buwang kumpleto ang trabaho, hanggang 11 araw. Pagkatapos ay suriin ang service record at unused leave compensation.", listOf("Hanggang 11 araw sa unang taon", "Suriin ang serbisyo at attendance", "I-verify ang unused leave")),
            topic("Wage delays", "Itago ang ebidensiya at makipag-ugnayan sa labor service kapag walang bayad.", "Itago ang kontrata, schedule, payslip, bank record at messages. Karaniwang dapat bayaran ang final amounts sa loob ng 14 araw. Tumawag sa labor hotline 1350 sa Korea.", listOf("14-araw na final settlement", "Labor hotline 1350", "Itago ang payment evidence"))
        )),
        section("5. Mga tuntunin ayon sa visa", "Mga dapat suriin para sa E-9, H-2 at F-series visa sa Korea.", listOf(
            topic("E-9 visa", "May hiwalay na tuntunin ang EPS sa job change at insurance.", "Ang E-9 work ay nasa Employment Permit System. Suriin ang departure-insurance documents at kasalukuyang EPS rules bago magpalit ng workplace.", listOf("Suriin ang insurance documents", "Kumpirmahin ang job-change rules", "Gamitin ang kasalukuyang EPS guidance")),
            topic("H-2 visa", "Suriin ang training, job registration at mga industriyang pinapayagan.", "Maaaring kailanganin ang employment training at job-seeker registration. Kumpirmahin ang workplace at duties bago magsimula at itago ang work documents.", listOf("Suriin ang pinapayagang industriya", "Itago ang work documents", "Suriin nang hiwalay ang insurance")),
            topic("F-series visas", "Magkakaiba ang kondisyon ng F-2, F-4, F-5 at F-6.", "Magkakaiba ang work rights at restrictions ng F-series. Suriin ang eksaktong residence status; ang tax, insurance, leave at severance ay nakabatay sa aktuwal na employment relationship.", listOf("Suriin ang eksaktong visa", "Tingnan ang work restrictions", "Itago ang kontrata at residence card"))
        ))
    ))

    private fun indonesianSections(): List<GuideSection> = buildSections(listOf(
        section("1. Pesangon", "Syarat hak, upah rata-rata, dan perbandingan dengan upah harian biasa.", listOf(
            topic("Kapan hak muncul", "Periksa masa kerja berkelanjutan dan rata-rata jam kerja mingguan.", """
                Syarat yang diperiksa adalah bekerja terus-menerus pada satu pemberi kerja sekurang-kurangnya 1 tahun dan rata-rata sedikitnya 15 jam per minggu selama 4 minggu. Bandingkan kontrak dan catatan kerja.

            """, listOf("Sedikitnya 365 hari", "Sedikitnya 15 jam per minggu", "Periksa berdasarkan dokumen kerja")),
            topic("Cara menghitung jumlahnya", "Jumlah harian dihitung lebih dulu, kemudian disesuaikan dengan masa kerja.", "Upah harian rata-rata = upah 3 bulan terakhir / jumlah hari kalender. Bonus tahunan dan kompensasi cuti yang memenuhi syarat dapat dimasukkan sebesar 3/12. Bandingkan dengan upah harian biasa dan gunakan nilai yang lebih tinggi.", listOf("Gunakan hari kalender", "Bandingkan upah harian", "Periksa slip gaji")),
            topic("Asuransi keberangkatan dan penyelesaian", "Aplikasi menampilkan perkiraan selisih untuk visa E-9 dan H-2.", "Masukkan jumlah asuransi keberangkatan dari dokumen perusahaan asuransi atau pemberi kerja. Aplikasi mengurangkannya dari pesangon dan menampilkan selisih perkiraan. Pajak saat berhenti kerja terpisah dan pembayaran akhir biasanya jatuh tempo dalam 14 hari.", listOf("Gunakan dokumen resmi", "Selisih adalah perkiraan", "Pajak dihitung terpisah"))
        )),
        section("2. Pajak dan status pekerjaan", "Pemotongan 3,3%, potongan asuransi, dan pembayaran tambahan sesuai kontrak.", listOf(
            topic("Pemotongan 3,3%", "Pemotongan ini saja tidak menentukan status freelancer.", "3,3% terdiri dari pajak penghasilan 3,0% dan pajak daerah 0,3%. Periksa hubungan kerja, pengawasan dan jadwal yang sebenarnya, bukan hanya label kontrak. SPT tahunan menentukan pajak akhir.", listOf("Bukan bukti status freelancer", "Periksa pekerjaan sebenarnya", "Tinjau SPT tahunan")),
            topic("Potongan asuransi utama", "Pensiun, kesehatan, perawatan jangka panjang, dan ketenagakerjaan bergantung pada dokumen Anda.", "Potongan asuransi bergantung pada visa, kewarganegaraan, status pekerja, dan slip gaji. Perawatan jangka panjang dihitung sebagai iuran tambahan yang terkait dengan asuransi kesehatan. Asuransi kecelakaan kerja biasanya dibayar oleh pemberi kerja, jadi jangan memasukkannya sebagai potongan pekerja. Dalam mode 3,3%, aktifkan potongan asuransi hanya jika tercantum di slip gaji. Pajak penghasilan upah dihitung terpisah; tarif dalam aplikasi hanya perkiraan.", listOf("Periksa visa dan dokumen", "Pisahkan bagian pemberi kerja dan pekerja", "Bandingkan dengan slip gaji")),
            topic("Pembayaran tambahan sesuai kontrak", "Masukkan pembayaran tambahan di bagian tunjangan sesuai nama dan jumlah pada slip gaji.", "Bonus, uang transportasi, dan pembayaran tambahan lain masuk ke total ketika tercantum di slip gaji. Bagian tunjangan menerima nama dan jumlah buatan sendiri, sehingga tidak perlu kolom khusus untuk setiap jenis. Bandingkan setiap pembayaran dengan kontrak dan slip gaji.", listOf("Gunakan nama di slip gaji", "Masukkan secara manual", "Bandingkan dengan dokumen"))
        )),
        section("3. Jam kerja dan tunjangan", "Jam dasar, bayaran libur mingguan, dan tarif tambahan.", listOf(
            topic("Acuan 209 jam per bulan", "209 jam adalah acuan bulanan dari minggu 40 jam dan bayaran libur mingguan.", "209 jam adalah acuan rata-rata bulanan. Jam sebenarnya bergantung pada kalender, jadwal dan kontrak. Periksa bayaran libur mingguan berdasarkan jam dan kehadiran.", listOf("Hanya acuan", "Periksa jadwal sebenarnya", "Tampilkan bayaran libur terpisah")),
            topic("Lembur, malam dan hari libur", "Aplikasi memakai pengali tambahan menurut aturan kerja.", "Aplikasi memakai 1,5× untuk lembur, tambahan 0,5× untuk kerja malam 22:00–06:00, 1,5× untuk 8 jam pertama hari libur dan 2,0× setelah 8 jam. Jangan memasukkan satu jam ke beberapa kategori.", listOf("Lembur 1,5×", "Premi malam 0,5×", "Hari libur 1,5× dan 2,0×"))
        )),
        section("4. Cuti dan upah yang belum dibayar", "Cuti tahunan, kompensasi, dan langkah saat pembayaran terlambat.", listOf(
            topic("Cuti tahunan berbayar", "Cuti bergantung pada masa kerja dan kehadiran.", "Pada tahun pertama, pekerja yang memenuhi syarat dapat menerima 1 hari untuk setiap bulan kerja penuh, hingga 11 hari. Setelah itu periksa masa kerja dan kompensasi cuti yang tersisa dengan catatan HR.", listOf("Hingga 11 hari pada tahun pertama", "Periksa masa kerja dan kehadiran", "Verifikasi cuti tersisa")),
            topic("Upah terlambat", "Simpan bukti dan hubungi layanan tenaga kerja jika upah belum dibayar.", "Simpan kontrak, jadwal, slip gaji, catatan bank dan pesan. Pembayaran akhir biasanya jatuh tempo dalam 14 hari. Hubungi hotline tenaga kerja Korea 1350.", listOf("Penyelesaian 14 hari", "Hotline tenaga kerja 1350", "Simpan bukti pembayaran"))
        )),
        section("5. Aturan kerja menurut visa", "Hal yang perlu diperiksa untuk visa E-9, H-2, dan seri F di Korea.", listOf(
            topic("Visa E-9", "EPS memiliki aturan khusus untuk perubahan tempat kerja dan asuransi.", "Pekerjaan E-9 berada di bawah Employment Permit System. Periksa dokumen asuransi keberangkatan dan aturan EPS terbaru sebelum pindah tempat kerja.", listOf("Periksa dokumen asuransi", "Konfirmasi aturan pindah kerja", "Gunakan panduan EPS terbaru")),
            topic("Visa H-2", "Periksa pelatihan, pendaftaran pencari kerja dan industri yang diizinkan.", "Pemegang H-2 mungkin memerlukan pelatihan kerja dan pendaftaran pencari kerja. Pastikan tempat dan tugas diizinkan sebelum mulai bekerja, lalu simpan dokumen kerja.", listOf("Periksa industri yang diizinkan", "Simpan dokumen kerja", "Tinjau asuransi secara terpisah")),
            topic("Visa seri F", "F-2, F-4, F-5 dan F-6 memiliki syarat kerja yang berbeda.", "Visa seri F berbeda dalam hak dan batasan kerja. Periksa status tinggal yang tepat; pajak, asuransi, cuti dan pesangon mengikuti hubungan kerja yang sebenarnya.", listOf("Periksa visa yang tepat", "Tinjau batasan kerja", "Simpan kontrak dan kartu tinggal"))
        ))
    ))

    private fun malaySections(): List<GuideSection> = buildSections(listOf(
        section("1. Pampasan penamatan kerja", "Syarat kelayakan, gaji purata dan perbandingan dengan gaji harian biasa.", listOf(
            topic("Bila hak wujud", "Semak tempoh kerja berterusan dan purata jam seminggu.", """
                Syarat yang disemak ialah kerja berterusan dengan seorang majikan sekurang-kurangnya 1 tahun dan purata sekurang-kurangnya 15 jam seminggu dalam tempoh 4 minggu. Bandingkan kontrak dan rekod kerja.

            """, listOf("Sekurang-kurangnya 365 hari", "Sekurang-kurangnya 15 jam seminggu", "Semak berdasarkan dokumen kerja")),
            topic("Cara jumlah dikira", "Jumlah harian dikira dahulu, kemudian didarab mengikut tempoh kerja.", "Gaji harian purata = gaji 3 bulan terakhir / jumlah hari kalendar. Bonus tahunan dan pampasan cuti yang layak boleh dimasukkan pada kadar 3/12. Bandingkan dengan gaji harian biasa dan gunakan nilai yang lebih tinggi.", listOf("Gunakan hari kalendar", "Bandingkan gaji harian", "Semak slip gaji")),
            topic("Insurans keluar negara dan penyelesaian", "Aplikasi menunjukkan perbezaan anggaran untuk visa E-9 dan H-2.", "Masukkan jumlah insurans keluar negara daripada dokumen syarikat insurans atau majikan. Aplikasi menolaknya daripada pampasan dan menunjukkan perbezaan anggaran. Cukai penamatan dikira berasingan dan bayaran akhir biasanya perlu dibuat dalam 14 hari.", listOf("Gunakan dokumen rasmi", "Perbezaan ialah anggaran", "Cukai dikira berasingan"))
        )),
        section("2. Cukai dan status pekerjaan", "Potongan 3.3%, potongan insurans dan bayaran tambahan mengikut kontrak.", listOf(
            topic("Potongan 3.3%", "Potongan ini sahaja tidak menentukan status freelancer.", "3.3% terdiri daripada cukai pendapatan 3.0% dan cukai tempatan 0.3%. Semak hubungan kerja, penyeliaan dan jadual sebenar, bukan label kontrak sahaja. Penyata tahunan menentukan cukai akhir.", listOf("Bukan bukti status freelancer", "Semak kerja sebenar", "Tinjau penyata tahunan")),
            topic("Potongan insurans utama", "Pencen, kesihatan, penjagaan jangka panjang dan pekerjaan bergantung pada dokumen anda.", "Potongan insurans bergantung pada visa, kewarganegaraan, status pekerja dan slip gaji. Penjagaan jangka panjang dikira sebagai caruman tambahan yang berkaitan dengan insurans kesihatan. Insurans kemalangan pekerjaan biasanya dibayar oleh majikan, jadi jangan masukkannya sebagai potongan pekerja. Dalam mod 3.3%, aktifkan item insurans hanya jika tercatat pada slip gaji. Cukai pendapatan gaji dikira berasingan; kadar dalam aplikasi hanyalah anggaran.", listOf("Semak visa dan dokumen", "Asingkan bahagian majikan dan pekerja", "Bandingkan dengan slip gaji")),
            topic("Bayaran tambahan mengikut kontrak", "Masukkan bayaran tambahan dalam bahagian elaun dengan nama dan jumlah pada slip gaji.", "Bonus, wang perjalanan dan bayaran tambahan lain masuk ke jumlah kasar apabila tercatat pada slip gaji. Bahagian elaun menerima nama dan jumlah sendiri, jadi tiada ruangan khas berasingan mengikut jenis diperlukan. Bandingkan setiap bayaran dengan kontrak dan slip gaji.", listOf("Gunakan nama pada slip gaji", "Masukkan secara manual", "Bandingkan dengan dokumen"))
        )),
        section("3. Waktu bekerja dan elaun", "Jam asas, bayaran cuti mingguan dan kadar tambahan.", listOf(
            topic("Rujukan 209 jam sebulan", "209 jam ialah rujukan bulanan daripada minggu 40 jam dan bayaran cuti mingguan.", "209 jam ialah purata rujukan bulanan. Jam sebenar bergantung pada kalendar, jadual dan kontrak. Semak bayaran cuti mingguan berdasarkan jam dan kehadiran sebenar.", listOf("Rujukan sahaja", "Semak jadual sebenar", "Paparkan bayaran cuti secara berasingan")),
            topic("Kerja lebih masa, malam dan cuti", "Aplikasi menggunakan pengganda tambahan mengikut peraturan kerja.", "Aplikasi menggunakan 1.5× untuk lebih masa, tambahan 0.5× untuk kerja malam 22:00–06:00, 1.5× bagi 8 jam pertama hari cuti dan 2.0× selepas 8 jam. Jangan masukkan jam yang sama dalam beberapa kategori.", listOf("Lebih masa 1.5×", "Premium malam 0.5×", "Cuti 1.5× dan 2.0×"))
        )),
        section("4. Cuti dan gaji tertunggak", "Cuti tahunan, pampasan dan langkah apabila gaji lewat dibayar.", listOf(
            topic("Cuti tahunan berbayar", "Hari cuti bergantung pada tempoh kerja dan kehadiran.", "Pada tahun pertama, pekerja yang memenuhi syarat mungkin menerima 1 hari bagi setiap bulan kerja penuh, sehingga 11 hari. Selepas itu semak tempoh kerja dan pampasan cuti yang belum digunakan dengan rekod HR.", listOf("Sehingga 11 hari tahun pertama", "Semak tempoh kerja dan kehadiran", "Sahkan cuti yang belum digunakan")),
            topic("Gaji lewat", "Simpan bukti dan hubungi perkhidmatan buruh apabila gaji belum dibayar.", "Simpan kontrak, jadual, slip gaji, rekod bank dan mesej. Bayaran akhir biasanya perlu dibuat dalam 14 hari. Hubungi talian buruh Korea 1350.", listOf("Penyelesaian 14 hari", "Talian buruh 1350", "Simpan bukti bayaran"))
        )),
        section("5. Peraturan kerja mengikut visa", "Perkara yang perlu disemak untuk visa E-9, H-2 dan siri F di Korea.", listOf(
            topic("Visa E-9", "EPS mempunyai peraturan khusus tentang pertukaran tempat kerja dan insurans.", "Kerja E-9 berada di bawah Employment Permit System. Semak dokumen insurans keluar negara dan peraturan EPS semasa sebelum bertukar tempat kerja.", listOf("Semak dokumen insurans", "Sahkan peraturan pertukaran kerja", "Gunakan panduan EPS semasa")),
            topic("Visa H-2", "Semak latihan, pendaftaran pencari kerja dan industri yang dibenarkan.", "Pemegang H-2 mungkin memerlukan latihan pekerjaan dan pendaftaran pencari kerja. Pastikan tempat kerja dan tugas dibenarkan sebelum mula bekerja dan simpan dokumen kerja.", listOf("Semak industri dibenarkan", "Simpan dokumen kerja", "Semak insurans secara berasingan")),
            topic("Visa siri F", "F-2, F-4, F-5 dan F-6 mempunyai syarat kerja yang berbeza.", "Visa siri F berbeza dari segi hak dan sekatan kerja. Semak status tinggal yang tepat; cukai, insurans, cuti dan pampasan mengikut hubungan kerja sebenar.", listOf("Semak visa yang tepat", "Tinjau sekatan kerja", "Simpan kontrak dan kad tinggal"))
        ))
    ))

    private fun burmeseSections(): List<GuideSection> = buildSections(listOf(
        section("၁။ အလုပ်ထွက်ကြေး", "ရပိုင်ခွင့်အခြေအနေ၊ ပျမ်းမျှလုပ်ခနှင့် ပုံမှန်နေ့စဉ်လုပ်ခ နှိုင်းယှဉ်မှု။", listOf(
            topic("ရပိုင်ခွင့်ရရှိသည့်အချိန်", "ဆက်တိုက်အလုပ်လုပ်သည့်ကာလနှင့် တစ်ပတ်ပျမ်းမျှအလုပ်ချိန်ကို စစ်ဆေးပါ။", """
                အလုပ်ရှင်တစ်ဦးတည်းထံတွင် အနည်းဆုံး ၁ နှစ် ဆက်တိုက်အလုပ်လုပ်ခြင်းနှင့် ၄ ပတ်အတွင်း တစ်ပတ်လျှင် ပျမ်းမျှ ၁၅ နာရီအနည်းဆုံး အလုပ်လုပ်ခြင်းကို စစ်ဆေးရပါမည်။ စာချုပ်နှင့် အလုပ်မှတ်တမ်းကို နှိုင်းယှဉ်ပါ။

            """, listOf("အနည်းဆုံး ၃၆၅ ရက်", "တစ်ပတ် ၁၅ နာရီအနည်းဆုံး", "အလုပ်စာရွက်စာတမ်းများဖြင့် စစ်ဆေးပါ")),
            topic("ပမာဏကို မည်သို့တွက်ချက်သနည်း", "နေ့စဉ်ပမာဏကို ဦးစွာတွက်ပြီး အလုပ်လုပ်သည့်ကာလအလိုက် မြှောက်ပါသည်။", "ပျမ်းမျှနေ့စဉ်လုပ်ခ = နောက်ဆုံး ၃ လလုပ်ခ / တွက်ချက်ကာလအတွင်း ပြက္ခဒိန်ရက်များ။ သတ်မှတ်ချက်နှင့်ကိုက်ညီသော နှစ်စဉ်ဆုကြေးနှင့် အသုံးမပြုရသေးသောခွင့်လစာကို 3/12 အချိုးဖြင့် ထည့်နိုင်ပါသည်။ ပျမ်းမျှနှင့် ပုံမှန်နေ့စဉ်လုပ်ခထဲမှ ပိုများသောပမာဏကို အသုံးပြုပါသည်။", listOf("ပြက္ခဒိန်ရက်အမှန်ကိုသုံးပါ", "နေ့စဉ်လုပ်ခနှစ်မျိုးကိုနှိုင်းပါ", "လစာစာရွက်စစ်ပါ")),
            topic("နိုင်ငံထွက်ခွာအာမခံနှင့် စာရင်းရှင်းခြင်း", "E-9 နှင့် H-2 ဗီဇာများအတွက် အာမခံနှင့် အလုပ်ထွက်ကြားရှိ ခန့်မှန်းကွာခြားချက်ကို ပြပါသည်။", "အာမခံကုမ္ပဏီ သို့မဟုတ် အလုပ်ရှင်စာရွက်စာတမ်းရှိ ပမာဏကို ထည့်ပါ။ အက်ပ်သည် အလုပ်ထွက်ကြေးမှ နုတ်ပြီး ခန့်မှန်းကွာခြားချက်ကို ပြပါသည်။ အလုပ်ထွက်ဝင်ငွေခွန်ကို သီးခြားတွက်ပြီး နောက်ဆုံးပေးချေမှုကို အများအားဖြင့် ၁၄ ရက်အတွင်း ပေးရပါသည်။", listOf("တရားဝင်စာရွက်စာတမ်းသုံးပါ", "ကွာခြားချက်မှာ ခန့်မှန်းချက်", "အခွန်ကို သီးခြားတွက်ပါ"))
        )),
        section("၂။ အခွန်နှင့် အလုပ်အကိုင်အခြေအနေ", "3.3% နုတ်ယူမှု၊ အာမခံနုတ်ယူမှုများနှင့် စာချုပ်ပါ ထပ်ဆောင်းပေးချေမှုများ။", listOf(
            topic("3.3% နုတ်ယူမှု", "ဤနုတ်ယူမှုတစ်ခုတည်းဖြင့် freelancer အခြေအနေကို မဆုံးဖြတ်နိုင်ပါ။", "3.3% တွင် ဝင်ငွေခွန် 3.0% နှင့် ဒေသခံအခွန် 0.3% ပါဝင်သည်။ စာချုပ်အမည်တစ်ခုတည်းမဟုတ်ဘဲ အမှန်တကယ်ကြီးကြပ်မှု၊ အချိန်ဇယားနှင့် အလုပ်ဆက်ဆံရေးကို စစ်ဆေးပါ။ နှစ်စဉ်ကြေညာချက်က နောက်ဆုံးအခွန်ကို သတ်မှတ်သည်။", listOf("Freelancer ဖြစ်ကြောင်း သက်သေမဟုတ်", "အမှန်တကယ်အလုပ်ကို စစ်ဆေးပါ", "နှစ်စဉ်ကြေညာချက်ကို ကြည့်ပါ")),
            topic("အဓိကအာမခံ နုတ်ယူမှုများ", "ပင်စင်၊ ကျန်းမာရေး၊ ရေရှည်စောင့်ရှောက်မှုနှင့် အလုပ်အကိုင်အာမခံများသည် စာရွက်စာတမ်းအပေါ် မူတည်သည်။", "အာမခံနုတ်ယူမှုများသည် ဗီဇာ၊ နိုင်ငံသားဖြစ်မှု၊ အလုပ်သမားအခြေအနေနှင့် လစာစာရွက်ပေါ် မူတည်သည်။ ရေရှည်စောင့်ရှောက်မှုအာမခံကို ကျန်းမာရေးအာမခံနှင့် ဆက်စပ်သည့် ထပ်ဆောင်းကြေးအဖြစ် တွက်ချက်သည်။ အလုပ်ခွင်မတော်တဆအာမခံကို ပုံမှန်အားဖြင့် အလုပ်ရှင်က ပေးဆောင်သဖြင့် အလုပ်သမားနုတ်ယူမှုအဖြစ် မထည့်ပါနှင့်။ 3.3% စနစ်တွင် လစာစာရွက်၌ အမှန်တကယ် ဖော်ပြထားသော အာမခံအချက်များကိုသာ ဖွင့်ပါ။ လုပ်ခဝင်ငွေခွန်ကို သီးခြားတွက်ပြီး အက်ပ်ရှိနှုန်းထားများမှာ ခန့်မှန်းချက်သာ ဖြစ်သည်။", listOf("ဗီဇာနှင့် စာရွက်စာတမ်းစစ်ဆေးပါ", "အလုပ်ရှင်နှင့် အလုပ်သမားဝေစုခွဲပါ", "လစာစာရွက်နှင့် နှိုင်းယှဉ်ပါ")),
            topic("စာချုပ်ပါ ထပ်ဆောင်းပေးချေမှုများ", "လစာစာရွက်ပါ အမည်နှင့် ပမာဏအတိုင်း ထပ်ဆောင်းပေးချေမှုအပိုင်းတွင် ထည့်ပါ။", "အပိုဆု၊ ခရီးစရိတ်နှင့် အခြားထပ်ဆောင်းပေးချေမှုများကို လစာစာရွက်တွင် ပါလျှင် စုစုပေါင်းထဲသို့ ထည့်ပါသည်။ ထပ်ဆောင်းပေးချေမှုအပိုင်းတွင် အမည်နှင့် ပမာဏကို ကိုယ်တိုင် သတ်မှတ်နိုင်သောကြောင့် အမျိုးအစားတိုင်းအတွက် သီးခြားအကွက် မလိုပါ။ စာချုပ်နှင့် လစာစာရွက်ကို နှိုင်းယှဉ်ပါ။", listOf("လစာစာရွက်ပါအမည်ကိုသုံးပါ", "ကိုယ်တိုင်ထည့်ပါ", "စာရွက်စာတမ်းနှင့်နှိုင်းယှဉ်ပါ"))
        )),
        section("၃။ အလုပ်ချိန်နှင့် အပိုကြေး", "အခြေခံနာရီ၊ အပတ်စဉ်နားရက်ကြေးနှင့် တိုးမြှင့်နှုန်းများ။", listOf(
            topic("တစ်လ ၂၀၉ နာရီ အညွှန်း", "၂၀၉ နာရီသည် တစ်ပတ် ၄၀ နာရီနှင့် အပတ်စဉ်နားရက်ကြေးမှ ရသော လစဉ်အညွှန်းဖြစ်သည်။", "၂၀၉ နာရီသည် ပျမ်းမျှလစဉ်အညွှန်းသာဖြစ်သည်။ အမှန်တကယ်နာရီသည် ပြက္ခဒိန်၊ အလုပ်ချိန်ဇယားနှင့် စာချုပ်ပေါ် မူတည်သည်။ နားရက်ကြေးကို အလုပ်ချိန်နှင့် တက်ရောက်မှုအရ စစ်ဆေးပါ။", listOf("အညွှန်းသာဖြစ်သည်", "အမှန်တကယ်ဇယားစစ်ပါ", "နားရက်ကြေးကို သီးခြားကြည့်ပါ")),
            topic("အချိန်ပို၊ ညပိုင်းနှင့် ပိတ်ရက်အလုပ်", "အက်ပ်သည် အလုပ်စည်းမျဉ်းအရ တိုးမြှင့်ကိန်းများကို အသုံးပြုသည်။", "အက်ပ်သည် အချိန်ပိုအတွက် 1.5×၊ 22:00–06:00 ညပိုင်းအတွက် ထပ်ဆောင်း 0.5×၊ ပိတ်ရက် ၈ နာရီအထိ 1.5× နှင့် ၈ နာရီကျော်လျှင် 2.0× အသုံးပြုသည်။ တစ်နာရီတည်းကို အုပ်စုများစွာတွင် မထည့်ပါနှင့်။", listOf("အချိန်ပို 1.5×", "ညပိုင်း 0.5×", "ပိတ်ရက် 1.5× နှင့် 2.0×"))
        )),
        section("၄။ ခွင့်ရက်နှင့် မရရှိသေးသော လုပ်ခ", "နှစ်စဉ်ခွင့်၊ လျော်ကြေးနှင့် လုပ်ခနောက်ကျသည့်အခါ လုပ်ဆောင်ရမည့်အရာများ။", listOf(
            topic("နှစ်စဉ်လစာပါခွင့်", "ခွင့်ရက်သည် အလုပ်သက်တမ်းနှင့် တက်ရောက်မှုအပေါ် မူတည်သည်။", "ပထမနှစ်တွင် သတ်မှတ်ချက်ပြည့်မီပါက ပြည့်စုံအလုပ်လုပ်သော လတိုင်းအတွက် ၁ ရက်၊ အများဆုံး ၁၁ ရက် ရရှိနိုင်သည်။ ထို့နောက် အလုပ်သက်တမ်းနှင့် အသုံးမပြုရသေးသောခွင့်ကို HR မှတ်တမ်းဖြင့် စစ်ပါ။", listOf("ပထမနှစ် ၁၁ ရက်အထိ", "အလုပ်သက်တမ်းစစ်ပါ", "ကျန်ခွင့်ကို အတည်ပြုပါ")),
            topic("လုပ်ခနောက်ကျခြင်း", "သက်သေများကို သိမ်းပြီး လုပ်ခမရပါက အလုပ်သမားဝန်ဆောင်မှုကို ဆက်သွယ်ပါ။", "စာချုပ်၊ အချိန်ဇယား၊ လစာစာရွက်၊ ဘဏ်မှတ်တမ်းနှင့် စာပို့ဆက်သွယ်မှုများကို သိမ်းပါ။ နောက်ဆုံးပေးချေမှုကို အများအားဖြင့် ၁၄ ရက်အတွင်း ပေးရသည်။ ကိုရီးယားအလုပ်သမား hotline 1350 ကို ဆက်သွယ်နိုင်သည်။", listOf("၁၄ ရက်အတွင်း စာရင်းရှင်း", "အလုပ်သမား hotline 1350", "ပေးချေမှုသက်သေသိမ်းပါ"))
        )),
        section("၅။ ဗီဇာအလိုက် အလုပ်စည်းမျဉ်းများ", "ကိုရီးယားရှိ E-9၊ H-2 နှင့် F အုပ်စုဗီဇာများအတွက် စစ်ဆေးရမည့်အချက်များ။", listOf(
            topic("E-9 ဗီဇာ", "EPS စနစ်တွင် အလုပ်နေရာပြောင်းခြင်းနှင့် အာမခံအတွက် သီးခြားစည်းမျဉ်းများရှိသည်။", "E-9 အလုပ်သည် Employment Permit System အောက်တွင်ရှိသည်။ အလုပ်နေရာမပြောင်းမီ နိုင်ငံထွက်ခွာအာမခံစာရွက်နှင့် လက်ရှိ EPS စည်းမျဉ်းများကို စစ်ပါ။", listOf("အာမခံစာရွက်စစ်ပါ", "အလုပ်ပြောင်းစည်းမျဉ်းအတည်ပြုပါ", "လက်ရှိ EPS လမ်းညွှန်သုံးပါ")),
            topic("H-2 ဗီဇာ", "သင်တန်း၊ အလုပ်ရှာဖွေသူစာရင်းနှင့် ခွင့်ပြုထားသော လုပ်ငန်းကဏ္ဍကို စစ်ပါ။", "H-2 ကိုင်ဆောင်သူများသည် အလုပ်သင်တန်းနှင့် အလုပ်ရှာဖွေသူစာရင်း လိုအပ်နိုင်သည်။ အလုပ်မစမီ နေရာနှင့် တာဝန်ခွင့်ပြုထားကြောင်း စစ်ပြီး အလုပ်စာရွက်များကို သိမ်းပါ။", listOf("ခွင့်ပြုကဏ္ဍစစ်ပါ", "အလုပ်စာရွက်သိမ်းပါ", "အာမခံကို သီးခြားစစ်ပါ")),
            topic("F အုပ်စုဗီဇာများ", "F-2၊ F-4၊ F-5 နှင့် F-6 တွင် အလုပ်အခြေအနေ မတူပါ။", "F အုပ်စုဗီဇာများသည် အလုပ်အခွင့်အရေးနှင့် ကန့်သတ်ချက်များ မတူကြပါ။ မိမိနေထိုင်ခွင့်အခြေအနေကို စစ်ပါ။ အခွန်၊ အာမခံ၊ ခွင့်နှင့် အလုပ်ထွက်ကြေးသည် အမှန်တကယ်အလုပ်ဆက်ဆံရေးအပေါ် မူတည်သည်။", listOf("မှန်ကန်သောဗီဇာစစ်ပါ", "အလုပ်ကန့်သတ်ချက်ကြည့်ပါ", "စာချုပ်နှင့် နေထိုင်ခွင့်ကတ်သိမ်းပါ"))
        ))
    ))
}
