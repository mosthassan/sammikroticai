package com.example.core.model

/**
 * Geographic Exchange Rate Zones in Yemen (IAS 21 compliant multi-zone pricing).
 * SANAA: Northern Governorates (Central Bank Sana'a rate regime).
 * ADEN: Southern Governorates (Central Bank Aden rate regime).
 */
enum class RateZone(val arabicName: String) {
    SANAA("صنعاء والمحافظات الشمالية (اليمن)"),
    ADEN("عدن والمحافظات الجنوبية (اليمن)"),
    KSA("المملكة العربية السعودية"),
    EGYPT("جمهورية مصر العربية"),
    IRAQ("العراق"),
    UAE("الإمارات العربية المتحدة"),
    CUSTOM("دولة مخصصة / عالمي");

    companion object {
        var DEFAULT = SANAA

        fun fromString(value: String): RateZone {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DEFAULT
        }
    }
}
