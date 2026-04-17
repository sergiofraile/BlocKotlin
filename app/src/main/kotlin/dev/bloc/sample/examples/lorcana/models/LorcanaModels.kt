package dev.bloc.sample.examples.lorcana.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LorcanaCard(
    @SerialName("Name")            val name: String,
    @SerialName("Artist")          val artist: String?          = null,
    @SerialName("Set_Name")        val setName: String?         = null,
    @SerialName("Set_Num")         val setNum: Int?             = null,
    @SerialName("Color")           val color: String?           = null,
    @SerialName("Image")           val image: String?           = null,
    @SerialName("Cost")            val cost: Int?               = null,
    @SerialName("Inkable")         val inkable: Boolean?        = null,
    @SerialName("Type")            val type: String?            = null,
    @SerialName("Classifications") val classifications: String? = null,
    @SerialName("Abilities")       val abilities: String?       = null,
    @SerialName("Flavor_Text")     val flavorText: String?      = null,
    @SerialName("Franchises")      val franchises: String?      = null,
    @SerialName("Rarity")          val rarity: String?          = null,
    @SerialName("Strength")        val strength: Int?           = null,
    @SerialName("Willpower")       val willpower: Int?          = null,
    @SerialName("Lore")            val lore: Int?               = null,
    @SerialName("Card_Num")        val cardNum: Int?            = null,
    @SerialName("Body_Text")       val bodyText: String?        = null,
    @SerialName("Set_ID")          val setId: String?           = null,
) {
    val id: String get() = "${setName.orEmpty()}-$name-${cardNum ?: 0}"

    val inkColor: InkColor get() = InkColor.of(color?.lowercase().orEmpty())
}

enum class InkColor(val displayName: String) {
    AMBER("Amber"), AMETHYST("Amethyst"), EMERALD("Emerald"),
    RUBY("Ruby"), SAPPHIRE("Sapphire"), STEEL("Steel"), UNKNOWN("Unknown");

    companion object {
        fun of(value: String): InkColor = entries.firstOrNull { it.name.lowercase() == value } ?: UNKNOWN
    }
}

data class LorcanaError(val message: String)

data class PaginationSummary(val hasMore: Boolean, val count: Int)
