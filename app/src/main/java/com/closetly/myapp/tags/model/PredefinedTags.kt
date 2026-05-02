package com.closetly.myapp.tags.model

object PredefinedTags {

    val SEASON: List<Tag> = listOf(
        Tag(id = "season_spring", name = "Frühling"),
        Tag(id = "season_summer", name = "Sommer"),
        Tag(id = "season_autumn", name = "Herbst"),
        Tag(id = "season_winter", name = "Winter")
    )

    val OCCASION: List<Tag> = listOf(
        Tag(id = "occasion_casual", name = "Casual"),
        Tag(id = "occasion_formal", name = "Formal"),
        Tag(id = "occasion_sport", name = "Sport"),
        Tag(id = "occasion_party", name = "Party"),
        Tag(id = "occasion_work", name = "Arbeit")
    )

    val STYLE: List<Tag> = listOf(
        Tag(id = "style_streetwear", name = "Streetwear"),
        Tag(id = "style_classic", name = "Classic"),
        Tag(id = "style_minimal", name = "Minimal"),
        Tag(id = "style_vintage", name = "Vintage"),
        Tag(id = "style_business", name = "Business")
    )

    val ALL: List<Tag> = SEASON + OCCASION + STYLE

    fun findById(id: String): Tag? = ALL.find { it.id == id }
}
