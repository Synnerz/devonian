package com.github.synnerz.devonian.api.kuudra

enum class KuudraTier(
    val num: Int,
) {
    None(0),
    Basic(1),
    Hot(2),
    Burning(3),
    Fiery(4),
    Infernal(5);

    companion object {
        fun from(name: String) = entries.find { it.name == name } ?: None
    }
}