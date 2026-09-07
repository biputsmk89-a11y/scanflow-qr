package com.scanflow.qr.core.utils

/**
 * Utilitas parser kamus prefiks GS1 dan validator checksum Modulo-10 offline.
 * Mampu mengidentifikasi negara asal produk dan tipe pengkodean barcode 1D
 * (EAN-13, EAN-8, UPC-A) tanpa memerlukan koneksi internet.
 */
object Gs1BarcodeParser {

    data class Gs1Info(
        val countryOrType: String,
        val flagEmoji: String,
        val prefix: String,
        val isChecksumValid: Boolean? = null
    )

    fun parse(code: String, format: String): Gs1Info? {
        val digitsOnly = code.filter { it.isDigit() }
        if (digitsOnly.length < 3) return null

        val isEan13 = format.contains("EAN_13", ignoreCase = true) || (format.contains("EAN", ignoreCase = true) && digitsOnly.length == 13)
        val isUpcA = format.contains("UPC_A", ignoreCase = true) || digitsOnly.length == 12
        val isEan8 = format.contains("EAN_8", ignoreCase = true) || (format.contains("EAN", ignoreCase = true) && digitsOnly.length == 8)

        val checksumValid = when {
            isEan13 && digitsOnly.length == 13 -> validateEan13Checksum(digitsOnly)
            isUpcA && digitsOnly.length == 12 -> validateUpcAChecksum(digitsOnly)
            isEan8 && digitsOnly.length == 8 -> validateEan8Checksum(digitsOnly)
            else -> null
        }

        // Prefiks standar GS1 menggunakan 3 digit pertama
        val prefix3 = digitsOnly.take(3).toIntOrNull() ?: return null

        val (country, flag) = lookupGs1Country(prefix3)

        return Gs1Info(
            countryOrType = country,
            flagEmoji = flag,
            prefix = digitsOnly.take(3),
            isChecksumValid = checksumValid
        )
    }

    private fun lookupGs1Country(p3: Int): Pair<String, String> {
        return when (p3) {
            in 0..139 -> "United States & Canada" to "🇺🇸 🇨🇦"
            in 300..379 -> "France" to "🇫🇷"
            380 -> "Bulgaria" to "🇧🇬"
            383 -> "Slovenia" to "🇸🇮"
            385 -> "Croatia" to "🇭🇷"
            387 -> "Bosnia and Herzegovina" to "🇧🇦"
            in 400..440 -> "Germany" to "🇩🇪"
            in 450..459, in 490..499 -> "Japan" to "🇯🇵"
            in 460..469 -> "Russia" to "🇷🇺"
            471 -> "Taiwan" to "🇹🇼"
            474 -> "Estonia" to "🇪🇪"
            475 -> "Latvia" to "🇱🇻"
            476 -> "Azerbaijan" to "🇦🇿"
            477 -> "Lithuania" to "🇱🇹"
            478 -> "Uzbekistan" to "🇺🇿"
            479 -> "Sri Lanka" to "🇱🇰"
            480 -> "Philippines" to "🇵🇭"
            481 -> "Belarus" to "🇧🇾"
            482 -> "Ukraine" to "🇺🇦"
            484 -> "Moldova" to "🇲🇩"
            485 -> "Armenia" to "🇦🇲"
            486 -> "Georgia" to "🇬🇪"
            487 -> "Kazakhstan" to "🇰🇿"
            489 -> "Hong Kong" to "🇭🇰"
            in 500..509 -> "United Kingdom" to "🇬🇧"
            in 520..521 -> "Greece" to "🇬🇷"
            528 -> "Lebanon" to "🇱🇧"
            529 -> "Cyprus" to "🇨🇾"
            530 -> "Albania" to "🇦🇱"
            531 -> "North Macedonia" to "🇲🇰"
            535 -> "Malta" to "🇲🇹"
            539 -> "Ireland" to "🇮🇪"
            in 540..549 -> "Belgium & Luxembourg" to "🇧🇪 🇱🇺"
            560 -> "Portugal" to "🇵🇹"
            569 -> "Iceland" to "🇮🇸"
            in 570..579 -> "Denmark" to "🇩🇰"
            590 -> "Poland" to "🇵🇱"
            594 -> "Romania" to "🇷🇴"
            599 -> "Hungary" to "🇭🇺"
            in 600..601 -> "South Africa" to "🇿🇦"
            603 -> "Ghana" to "🇬🇭"
            604 -> "Senegal" to "🇸🇳"
            608 -> "Bahrain" to "🇧🇭"
            609 -> "Mauritius" to "🇲🇺"
            611 -> "Morocco" to "🇲🇦"
            613 -> "Algeria" to "🇩🇿"
            615 -> "Nigeria" to "🇳🇬"
            616 -> "Kenya" to "🇰🇪"
            618 -> "Ivory Coast" to "🇨🇮"
            619 -> "Tunisia" to "🇹🇳"
            621 -> "Syria" to "🇸🇾"
            622 -> "Egypt" to "🇪🇬"
            624 -> "Libya" to "🇱🇾"
            625 -> "Jordan" to "🇯🇴"
            626 -> "Iran" to "🇮🇷"
            627 -> "Kuwait" to "🇰🇼"
            628 -> "Saudi Arabia" to "🇸🇦"
            629 -> "United Arab Emirates" to "🇦🇪"
            in 640..649 -> "Finland" to "🇫🇮"
            in 690..699 -> "China" to "🇨🇳"
            in 700..709 -> "Norway" to "🇳🇴"
            729 -> "Israel" to "🇮🇱"
            in 730..739 -> "Sweden" to "🇸🇪"
            740 -> "Guatemala" to "🇬🇹"
            741 -> "El Salvador" to "🇸🇻"
            742 -> "Honduras" to "🇭🇳"
            743 -> "Nicaragua" to "🇳🇮"
            744 -> "Costa Rica" to "🇨🇷"
            745 -> "Panama" to "🇵🇦"
            746 -> "Dominican Republic" to "🇩🇴"
            750 -> "Mexico" to "🇲🇽"
            in 754..755 -> "Canada" to "🇨🇦"
            759 -> "Venezuela" to "🇻🇪"
            in 760..769 -> "Switzerland" to "🇨🇭"
            in 770..771 -> "Colombia" to "🇨🇴"
            773 -> "Uruguay" to "🇺🇾"
            775 -> "Peru" to "🇵🇪"
            777 -> "Bolivia" to "🇧🇴"
            779 -> "Argentina" to "🇦🇷"
            780 -> "Chile" to "🇨🇱"
            784 -> "Paraguay" to "🇵🇾"
            786 -> "Ecuador" to "🇪🇨"
            in 789..790 -> "Brazil" to "🇧🇷"
            in 800..839 -> "Italy" to "🇮🇹"
            in 840..849 -> "Spain" to "🇪🇸"
            850 -> "Cuba" to "🇨🇺"
            858 -> "Slovakia" to "🇸🇰"
            859 -> "Czech Republic" to "🇨🇿"
            860 -> "Serbia" to "🇷🇸"
            865 -> "Mongolia" to "🇲🇳"
            867 -> "North Korea" to "🇰🇵"
            in 868..869 -> "Turkey" to "🇹🇷"
            in 870..879 -> "Netherlands" to "🇳🇱"
            880 -> "South Korea" to "🇰🇷"
            884 -> "Cambodia" to "🇰🇭"
            885 -> "Thailand" to "🇹🇭"
            888 -> "Singapore" to "🇸🇬"
            890 -> "India" to "🇮🇳"
            893 -> "Vietnam" to "🇻🇳"
            896 -> "Pakistan" to "🇵🇰"
            899 -> "Indonesia" to "🇮🇩"
            in 900..919 -> "Austria" to "🇦🇹"
            in 930..939 -> "Australia" to "🇦🇺"
            in 940..949 -> "New Zealand" to "🇳🇿"
            955 -> "Malaysia" to "🇲🇾"
            958 -> "Macau" to "🇲🇴"
            977 -> "Serial Publications (ISSN)" to "📰"
            in 978..979 -> "Bookland (ISBN)" to "📚"
            in 980..984 -> "Coupons & Vouchers" to "🎟️"
            else -> "Global / International" to "🌐"
        }
    }

    fun validateEan13Checksum(code: String): Boolean {
        if (code.length != 13) return false
        val digits = code.map { it.digitToInt() }
        val sum = digits.take(12).mapIndexed { index, d ->
            if (index % 2 == 0) d else d * 3
        }.sum()
        val checkDigit = (10 - (sum % 10)) % 10
        return digits[12] == checkDigit
    }

    fun validateUpcAChecksum(code: String): Boolean {
        if (code.length != 12) return false
        val digits = code.map { it.digitToInt() }
        val sum = digits.take(11).mapIndexed { index, d ->
            if (index % 2 == 0) d * 3 else d
        }.sum()
        val checkDigit = (10 - (sum % 10)) % 10
        return digits[11] == checkDigit
    }

    fun validateEan8Checksum(code: String): Boolean {
        if (code.length != 8) return false
        val digits = code.map { it.digitToInt() }
        val sum = digits.take(7).mapIndexed { index, d ->
            if (index % 2 == 0) d * 3 else d
        }.sum()
        val checkDigit = (10 - (sum % 10)) % 10
        return digits[7] == checkDigit
    }
}
