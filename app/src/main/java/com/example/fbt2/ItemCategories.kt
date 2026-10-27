package com.example.fbt2

object ItemCategories {

    val CATEGORIES = arrayOf(
        "Projector", "Camera", "Photography", "Art", "Tab", "Exhibition Equipment"
    )

    val TYPES: Map<String, Array<String>> = mapOf(
        "Projector" to arrayOf(
            "Projector"
        ),
        "Camera" to arrayOf(
            "Canon 70D",
            "Sony A7IV",
            "Camera Tripod",
            "Sony Battery",
            "Canon Battery",
            "Gimbal Stabilizer",
            "Lens Samyang 50 mm F1.4"
        ),
        "Photography" to arrayOf(
            "Studio LED light",
            "Studio Strobe Light",
            "Light Stand",
            "C Stand",
            "Softbox Honey Comb (80 X 120)",
            "Softbox Honey Comb (50 X 130)",
            "Softbox Lantern"
        ),
        "Art" to arrayOf(
            "Easel/Art Stand",
            "A3 Poster Stand (Black)"
        ),
        "Tab" to arrayOf(
            "WACOM Drawing Tab",
            "Light Tab"
        ),
        "Exhibition Equipment" to arrayOf(
            "Set of PVC Cubic",
            "Black Panel",
            "White Panel",
            "Panel Light"
        )
    )

    fun getTypesFor(category: String): Array<String> {
        return TYPES[category] ?: arrayOf()
    }
}