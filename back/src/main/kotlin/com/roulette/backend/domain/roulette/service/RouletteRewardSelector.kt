package com.roulette.backend.domain.roulette.service

import com.roulette.backend.domain.roulette.domain.RouletteRewardOption
import org.springframework.stereotype.Component
import java.util.concurrent.ThreadLocalRandom

interface RouletteRewardSelector {
    fun selectRewardPoints(): Int
}

@Component
class WeightedRandomRouletteRewardSelector : RouletteRewardSelector {
    override fun selectRewardPoints(): Int {
        val randomPercent = ThreadLocalRandom.current().nextInt(TOTAL_PERCENT) + 1
        if (randomPercent <= FIRST_SECTION_END) return RouletteRewardOption.POINTS_100.points
        if (randomPercent <= SECOND_SECTION_END) return RouletteRewardOption.POINTS_300.points
        if (randomPercent <= THIRD_SECTION_END) return RouletteRewardOption.POINTS_500.points
        return RouletteRewardOption.POINTS_1000.points
    }

    companion object {
        private const val TOTAL_PERCENT = 100
        private const val FIRST_SECTION_END = 40
        private const val SECOND_SECTION_END = 70
        private const val THIRD_SECTION_END = 90
    }
}
