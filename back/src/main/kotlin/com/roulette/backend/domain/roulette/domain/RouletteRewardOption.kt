package com.roulette.backend.domain.roulette.domain

enum class RouletteRewardOption(
    val points: Int,
    val probabilityPercent: Int,
) {
    POINTS_100(points = 100, probabilityPercent = 40),
    POINTS_300(points = 300, probabilityPercent = 30),
    POINTS_500(points = 500, probabilityPercent = 20),
    POINTS_1000(points = 1000, probabilityPercent = 10),
    ;

    companion object {
        val allowedPoints: Set<Int> = entries.map { it.points }.toSet()
    }
}
