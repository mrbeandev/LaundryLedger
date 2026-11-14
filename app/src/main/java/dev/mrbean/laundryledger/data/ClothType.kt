package dev.mrbean.laundryledger.data

import androidx.annotation.DrawableRes
import dev.mrbean.laundryledger.R

/** Who a cloth type is usually relevant for. Used only to filter the catalog. */
enum class Audience { ALL, MEN, WOMEN }

enum class Gender(val label: String) {
    MALE("Male"),
    FEMALE("Female"),
    UNSPECIFIED("Prefer not to say");

    fun sees(a: Audience) = when (this) {
        MALE -> a != Audience.WOMEN
        FEMALE -> a != Audience.MEN
        UNSPECIFIED -> true
    }
}

/**
 * Stored in the DB by [name], so entries may be reordered or added but never renamed.
 * Enum order is the display order.
 */
enum class ClothType(val label: String, @DrawableRes val icon: Int, val audience: Audience) {
    SHIRT("Shirt", R.drawable.cloth_shirt, Audience.ALL),
    TSHIRT("T-Shirt", R.drawable.cloth_tshirt, Audience.ALL),
    TOP("Top", R.drawable.cloth_top, Audience.WOMEN),
    KURTI("Kurti", R.drawable.cloth_kurti, Audience.WOMEN),
    KURTA("Kurta", R.drawable.cloth_kurta, Audience.MEN),
    SAREE("Saree", R.drawable.cloth_saree, Audience.WOMEN),
    BLOUSE("Blouse", R.drawable.cloth_blouse, Audience.WOMEN),
    FROCK("Frock / Dress", R.drawable.cloth_frock, Audience.WOMEN),
    JEANS("Jeans", R.drawable.cloth_jeans, Audience.ALL),
    TROUSERS("Trousers", R.drawable.cloth_trousers, Audience.ALL),
    NIGHT_PANT("Night Pant", R.drawable.cloth_nightpant, Audience.ALL),
    TRACK_PANT("Track Pant", R.drawable.cloth_trackpant, Audience.ALL),
    SHORTS("Shorts", R.drawable.cloth_shorts, Audience.ALL),
    LEGGINGS("Leggings", R.drawable.cloth_leggings, Audience.WOMEN),
    SALWAR("Salwar", R.drawable.cloth_salwar, Audience.WOMEN),
    SKIRT("Skirt", R.drawable.cloth_skirt, Audience.WOMEN),
    LUNGI("Lungi / Dhoti", R.drawable.cloth_lungi, Audience.MEN),
    NIGHTIE("Nightie", R.drawable.cloth_nightie, Audience.WOMEN),
    UNDERWEAR("Underwear", R.drawable.cloth_underwear, Audience.MEN),
    BANIAN("Banian", R.drawable.cloth_banian, Audience.MEN),
    BRA("Bra", R.drawable.cloth_bra, Audience.WOMEN),
    PANTIES("Panties", R.drawable.cloth_panties, Audience.WOMEN),
    SOCKS("Socks", R.drawable.cloth_socks, Audience.ALL),
    KERCHIEF("Kerchief", R.drawable.cloth_kerchief, Audience.ALL),
    DUPATTA("Dupatta", R.drawable.cloth_dupatta, Audience.WOMEN),
    SWEATER("Sweater", R.drawable.cloth_sweater, Audience.ALL),
    HOODIE("Hoodie", R.drawable.cloth_hoodie, Audience.ALL),
    JACKET("Jacket", R.drawable.cloth_jacket, Audience.ALL),
    UNIFORM("Uniform", R.drawable.cloth_uniform, Audience.ALL),
    KIDS_WEAR("Kids Wear", R.drawable.cloth_kidswear, Audience.ALL),
    TOWEL("Towel", R.drawable.cloth_towel, Audience.ALL),
    BEDSHEET("Bedsheet", R.drawable.cloth_bedsheet, Audience.ALL),
    PILLOW_COVER("Pillow Cover", R.drawable.cloth_pillowcover, Audience.ALL),
    BLANKET("Blanket", R.drawable.cloth_blanket, Audience.ALL),
    CURTAIN("Curtain", R.drawable.cloth_curtain, Audience.ALL);

    companion object {
        fun of(name: String): ClothType = entries.firstOrNull { it.name == name } ?: SHIRT

        fun catalog(g: Gender): List<ClothType> = entries.filter { g.sees(it.audience) }

        private val menDefaults = setOf(
            UNDERWEAR, BANIAN, SHIRT, TSHIRT, JEANS, NIGHT_PANT, TOWEL, BEDSHEET, KERCHIEF, SOCKS, PILLOW_COVER,
        )
        private val womenDefaults = setOf(
            TOP, KURTI, SAREE, BLOUSE, TSHIRT, JEANS, LEGGINGS, SALWAR, NIGHTIE, DUPATTA, BRA, PANTIES,
            TOWEL, BEDSHEET, PILLOW_COVER,
        )

        fun defaults(g: Gender): Set<ClothType> = when (g) {
            Gender.MALE -> menDefaults
            Gender.FEMALE -> womenDefaults
            Gender.UNSPECIFIED -> menDefaults + womenDefaults
        }
    }
}
