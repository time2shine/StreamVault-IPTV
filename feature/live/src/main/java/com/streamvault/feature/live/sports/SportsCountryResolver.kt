package com.streamvault.feature.live.sports

/**
 * Resolves international country flags and cricket board badges for sports competitors.
 *
 * Provides instant, zero-cost flag resolution for national team fixtures (e.g. Cricket,
 * FIFA, Olympics, Rugby) without requiring third-party API accounts or web scraping.
 */
internal object SportsCountryResolver {
    private const val FLAGCDN_BASE = "https://flagcdn.com/w80/"
    private const val WEST_INDIES_DRAWABLE = "local_res://ic_sports_flag_west_indies"

    private val countryCodeMap: Map<String, String> = mapOf(
        // Cricket & Asian Sports
        "india" to "in",
        "bangladesh" to "bd",
        "pakistan" to "pk",
        "sri lanka" to "lk",
        "afghanistan" to "af",
        "nepal" to "np",
        "united arab emirates" to "ae",
        "uae" to "ae",
        "oman" to "om",
        "namibia" to "na",
        "zimbabwe" to "zw",
        "ireland" to "ie",
        "scotland" to "gb-sct",
        "netherlands" to "nl",
        "holland" to "nl",
        "papua new guinea" to "pg",
        "png" to "pg",
        "uganda" to "ug",
        "kenya" to "ke",
        "hong kong" to "hk",
        "canada" to "ca",
        "united states" to "us",
        "usa" to "us",
        "australia" to "au",
        "new zealand" to "nz",
        "south africa" to "za",
        "england" to "gb-eng",
        "wales" to "gb-wls",
        "northern ireland" to "gb-nir",

        // Football (Soccer) & Global Sports
        "argentina" to "ar",
        "brazil" to "br",
        "france" to "fr",
        "germany" to "de",
        "spain" to "es",
        "portugal" to "pt",
        "italy" to "it",
        "belgium" to "be",
        "croatia" to "hr",
        "uruguay" to "uy",
        "colombia" to "co",
        "mexico" to "mx",
        "japan" to "jp",
        "south korea" to "kr",
        "korea" to "kr",
        "morocco" to "ma",
        "senegal" to "sn",
        "nigeria" to "ng",
        "egypt" to "eg",
        "saudi arabia" to "sa",
        "iran" to "ir",
        "switzerland" to "ch",
        "denmark" to "dk",
        "sweden" to "se",
        "poland" to "pl",
        "norway" to "no",
        "austria" to "at",
        "turkey" to "tr",
        "turkiye" to "tr",
        "chile" to "cl",
        "peru" to "pe",
        "ecuador" to "ec",
        "paraguay" to "py",
        "venezuela" to "ve",
        "costa rica" to "cr",
        "panama" to "pa",
        "jamaica" to "jm",
        "ghana" to "gh",
        "cameroon" to "cm",
        "ivory coast" to "ci",
        "cote d ivoire" to "ci",
        "algeria" to "dz",
        "tunisia" to "tn",
        "qatar" to "qa",
        "iraq" to "iq",
        "china" to "cn",
        "thailand" to "th",
        "vietnam" to "vn",
        "indonesia" to "id",
        "malaysia" to "my",
        "singapore" to "sg",
        "philippines" to "ph",
        "fiji" to "fj",
        "samoa" to "ws",
        "tonga" to "to",
        "georgia" to "ge",
        "greece" to "gr",
        "serbia" to "rs",
        "romania" to "ro",
        "ukraine" to "ua",
        "czech republic" to "cz",
        "czechia" to "cz",
        "hungary" to "hu",
    )

    private val specialTeamMap: Map<String, String> = mapOf(
        "west indies" to WEST_INDIES_DRAWABLE,
        "windies" to WEST_INDIES_DRAWABLE,
    )

    private val noiseWordsPattern = Regex(
        """\b(?:men|women|u19|u21|u23|xi|cricket|national|team|football|soccer|a)\b""",
        RegexOption.IGNORE_CASE,
    )

    private val whitespacePattern = Regex("""\s+""")

    /**
     * Resolves a public flag URL or local drawable resource URI for the given competitor name.
     * Returns null if the competitor is not a recognized national/international sports team.
     */
    fun resolveFlagUrl(competitorName: String): String? {
        val trimmed = competitorName.trim()
        if (trimmed.isBlank()) return null

        val normalized = SportsTextNormalizer.normalize(trimmed)
        specialTeamMap[normalized]?.let { return it }
        countryCodeMap[normalized]?.let { return "$FLAGCDN_BASE$it.png" }

        val stripped = normalized
            .replace(noiseWordsPattern, " ")
            .replace(whitespacePattern, " ")
            .trim()

        if (stripped.isNotBlank() && stripped != normalized) {
            specialTeamMap[stripped]?.let { return it }
            countryCodeMap[stripped]?.let { return "$FLAGCDN_BASE$it.png" }
        }

        return null
    }
}
