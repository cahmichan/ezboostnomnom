package com.ezboost.ga;

import com.ezboost.model.Room;
import com.ezboost.model.Season;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeneticAlgorithmReproducibilityTest {

    @Test
    void sameSeedProducesTheSameSeasonalPricingPlan() {
        List<Room> firstInput = Arrays.asList(new Room("Deluxe", 200, 150, 300, 75, 20));
        List<Room> secondInput = Arrays.asList(new Room("Deluxe", 200, 150, 300, 75, 20));

        List<Room> first = new GeneticAlgorithm(1_500_000, firstInput, DemandCurve.createDefault(), 123456789L).runGA();
        List<Room> second = new GeneticAlgorithm(1_500_000, secondInput, DemandCurve.createDefault(), 123456789L).runGA();

        for (Season season : Season.values()) {
            assertEquals(first.get(0).getSeasonalPrices().get(season),
                    second.get(0).getSeasonalPrices().get(season));
        }
    }
}
