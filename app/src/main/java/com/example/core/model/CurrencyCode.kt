package com.example.core.model

enum class CurrencyCode(val symbol: String, val arabicName: String, val scale: Int = 2) {
    YER("ر.ي", "ريال يمني"),
    USD("$", "دولار أمريكي"),
    SAR("ر.س", "ريال سعودي"),
    EGP("ج.م", "جنيه مصري"),
    IQD("ع.د", "دينار عراقي"),
    AED("د.إ", "درهم إماراتي"),
    CUSTOM("؟", "عملة مخصصة");

    companion object {
        var FUNCTIONAL = YER

        fun fromString(value: String): CurrencyCode {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: FUNCTIONAL
        }
    }
}
