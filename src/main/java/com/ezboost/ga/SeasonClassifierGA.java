package com.ezboost.ga;

import com.ezboost.model.MonthlySeasonData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * SeasonClassifierGA - Uses Genetic Algorithm to automatically determine
 * optimal occupancy thresholds for season classification.
 * 
 * This solves the problem of hardcoded thresholds (85%, 75%, 65%) that don't
 * work for all datasets. Instead, it finds the best 3 thresholds (T1, T2, T3)
 * that divide the data into 4 seasons optimally.
 * 
 * Fitness Function (Weighted):
 * - 40% Distribution Balance: Penalizes having seasons with 0 or too few months
 * - 40% Natural Clustering: Rewards finding natural gaps in occupancy data
 * - 20% Ordering Validity: Ensures LOW < NORMAL < PEAK < SUPER_PEAK
 */
public class SeasonClassifierGA {

    private static final Logger logger = LoggerFactory.getLogger(SeasonClassifierGA.class);

    private final List<MonthlySeasonData> monthlyData;
    private final double[] occupancyValues;
    
    // GA Parameters
    private static final int POPULATION_SIZE = 50;
    private static final int GENERATIONS = 200;
    private static final double CROSSOVER_RATE = 0.8;
    private static final double MUTATION_RATE = 0.3;
    private static final double MUTATION_STRENGTH = 5.0; // Max change in threshold per mutation
    
    private final Random random = new Random();
    private Long randomSeed;
    
    // Results
    private double[] bestThresholds; // [T1, T2, T3] where T1 < T2 < T3
    private Map<String, String> monthSeasonMap; // MonthYear -> Season

    public SeasonClassifierGA(List<MonthlySeasonData> monthlyData) {
        this.monthlyData = monthlyData;
        this.occupancyValues = monthlyData.stream()
                .mapToDouble(MonthlySeasonData::getOccupancyRate)
                .sorted()
                .toArray();
        
        if (occupancyValues.length < 4) {
            throw new IllegalArgumentException("Need at least 4 months of data for season classification");
        }
    }

    /** Creates a replayable season-classification run for testing and audit. */
    public SeasonClassifierGA(List<MonthlySeasonData> monthlyData, long randomSeed) {
        this(monthlyData);
        this.random.setSeed(randomSeed);
        this.randomSeed = randomSeed;
    }

    public Long getRandomSeed() {
        return randomSeed;
    }

    /**
     * Run the GA to find optimal thresholds
     * @return double[3] containing [T1, T2, T3] thresholds
     */
    public double[] runGA() {
        logger.debug("=== Season Classifier GA Started ===");
        logger.debug("Data points: {}", occupancyValues.length);
        logger.debug("Occupancy range: {}% - {}%", String.format("%.2f", occupancyValues[0]),
                     String.format("%.2f", occupancyValues[occupancyValues.length - 1]));

        // Initialize population
        List<double[]> population = initializePopulation();
        
        double[] bestSolution = null;
        double bestFitness = Double.NEGATIVE_INFINITY;

        // Evolution loop
        for (int generation = 0; generation < GENERATIONS; generation++) {
            List<double[]> newPopulation = new ArrayList<>();

            while (newPopulation.size() < POPULATION_SIZE) {
                // Selection
                double[] parent1 = tournamentSelection(population, 3);
                double[] parent2 = tournamentSelection(population, 3);

                // Crossover
                double[] child;
                if (random.nextDouble() < CROSSOVER_RATE) {
                    child = crossover(parent1, parent2);
                } else {
                    child = parent1.clone();
                }

                // Mutation
                if (random.nextDouble() < MUTATION_RATE) {
                    mutate(child);
                }

                // Ensure valid ordering
                ensureValidThresholds(child);

                newPopulation.add(child);

                // Track best
                double fitness = calculateFitness(child);
                if (fitness > bestFitness) {
                    bestFitness = fitness;
                    bestSolution = child.clone();
                }
            }

            population = newPopulation;

            // Debug output every 50 generations
            if (generation % 50 == 0) {
                logger.debug("Generation {}: Best fitness = {}", generation,
                             String.format("%.4f", bestFitness));
            }
        }

        this.bestThresholds = bestSolution;
        classifyMonths();

        // Print results
        printResults();

        return bestThresholds;
    }

    /**
     * Initialize population with random thresholds within data range
     */
    private List<double[]> initializePopulation() {
        List<double[]> population = new ArrayList<>();
        
        double minOcc = occupancyValues[0];
        double maxOcc = occupancyValues[occupancyValues.length - 1];
        double range = maxOcc - minOcc;

        for (int i = 0; i < POPULATION_SIZE; i++) {
            double[] chromosome = new double[3];
            
            // Generate 3 random thresholds and sort them
            chromosome[0] = minOcc + random.nextDouble() * range;
            chromosome[1] = minOcc + random.nextDouble() * range;
            chromosome[2] = minOcc + random.nextDouble() * range;
            
            Arrays.sort(chromosome);
            
            // Ensure minimum spacing (at least 3% between thresholds)
            ensureMinimumSpacing(chromosome, 3.0);
            
            population.add(chromosome);
        }

        return population;
    }

    /**
     * WEIGHTED FITNESS FUNCTION
     * 40% Distribution Balance + 40% Natural Clustering + 20% Ordering
     */
    private double calculateFitness(double[] thresholds) {
        double t1 = thresholds[0]; // LOW/NORMAL boundary
        double t2 = thresholds[1]; // NORMAL/PEAK boundary
        double t3 = thresholds[2]; // PEAK/SUPER_PEAK boundary

        // Count months in each season
        int lowCount = 0, normalCount = 0, peakCount = 0, superPeakCount = 0;
        
        for (double occ : occupancyValues) {
            if (occ < t1) lowCount++;
            else if (occ < t2) normalCount++;
            else if (occ < t3) peakCount++;
            else superPeakCount++;
        }

        int totalMonths = occupancyValues.length;
        int idealPerSeason = totalMonths / 4;

        // ========== 1. DISTRIBUTION BALANCE (40%) ==========
        double distributionScore = 0;
        
        // Heavily penalize empty seasons
        int emptySeasons = 0;
        if (lowCount == 0) emptySeasons++;
        if (normalCount == 0) emptySeasons++;
        if (peakCount == 0) emptySeasons++;
        if (superPeakCount == 0) emptySeasons++;
        
        if (emptySeasons > 0) {
            distributionScore = -emptySeasons * 100; // Heavy penalty
        } else {
            // Calculate variance from ideal distribution
            double variance = Math.pow(lowCount - idealPerSeason, 2) +
                            Math.pow(normalCount - idealPerSeason, 2) +
                            Math.pow(peakCount - idealPerSeason, 2) +
                            Math.pow(superPeakCount - idealPerSeason, 2);
            
            // Normalize: lower variance = higher score
            double maxVariance = 3 * Math.pow(totalMonths, 2); // Worst case
            distributionScore = 100 * (1 - variance / maxVariance);
        }

        // ========== 2. NATURAL CLUSTERING (40%) ==========
        double clusteringScore = 0;
        
        // Find natural gaps in the data
        List<Double> gaps = new ArrayList<>();
        for (int i = 1; i < occupancyValues.length; i++) {
            gaps.add(occupancyValues[i] - occupancyValues[i-1]);
        }
        Collections.sort(gaps, Collections.reverseOrder());
        
        // Reward if thresholds are near large gaps
        double gapBonus = 0;
        for (double threshold : thresholds) {
            // Find closest gap to this threshold
            double minDistToGap = Double.MAX_VALUE;
            double gapSize = 0;
            
            for (int i = 1; i < occupancyValues.length; i++) {
                double gapMidpoint = (occupancyValues[i] + occupancyValues[i-1]) / 2;
                double dist = Math.abs(threshold - gapMidpoint);
                if (dist < minDistToGap) {
                    minDistToGap = dist;
                    gapSize = occupancyValues[i] - occupancyValues[i-1];
                }
            }
            
            // Larger gap + closer threshold = more bonus
            if (minDistToGap < 5) { // Within 5% of gap
                gapBonus += gapSize * (5 - minDistToGap) / 5;
            }
        }
        
        // Normalize clustering score
        double avgGap = gaps.stream().mapToDouble(d -> d).average().orElse(1);
        clusteringScore = Math.min(100, gapBonus / avgGap * 20);

        // ========== 3. THRESHOLD SPACING (20%) ==========
        double spacingScore = 0;
        
        double spacing1 = t2 - t1;
        double spacing2 = t3 - t2;
        
        // Penalize very uneven spacing
        double spacingRatio = Math.min(spacing1, spacing2) / Math.max(spacing1, spacing2);
        spacingScore = 100 * spacingRatio;
        
        // Bonus for reasonable minimum spacing (at least 5%)
        if (spacing1 >= 5 && spacing2 >= 5) {
            spacingScore += 20;
        }
        
        spacingScore = Math.min(100, spacingScore);

        // ========== WEIGHTED TOTAL ==========
        double totalFitness = (0.4 * distributionScore) + 
                             (0.4 * clusteringScore) + 
                             (0.2 * spacingScore);

        return totalFitness;
    }

    /**
     * Tournament selection
     */
    private double[] tournamentSelection(List<double[]> population, int tournamentSize) {
        double[] best = null;
        double bestFitness = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < tournamentSize; i++) {
            double[] candidate = population.get(random.nextInt(population.size()));
            double fitness = calculateFitness(candidate);
            if (fitness > bestFitness) {
                bestFitness = fitness;
                best = candidate;
            }
        }

        return best;
    }

    /**
     * Arithmetic crossover
     */
    private double[] crossover(double[] parent1, double[] parent2) {
        double[] child = new double[3];
        double alpha = random.nextDouble();
        
        for (int i = 0; i < 3; i++) {
            child[i] = alpha * parent1[i] + (1 - alpha) * parent2[i];
        }
        
        Arrays.sort(child);
        return child;
    }

    /**
     * Gaussian mutation
     */
    private void mutate(double[] chromosome) {
        int index = random.nextInt(3);
        double mutation = random.nextGaussian() * MUTATION_STRENGTH;
        chromosome[index] += mutation;
        
        // Clamp to data range
        double minOcc = occupancyValues[0] - 5;
        double maxOcc = occupancyValues[occupancyValues.length - 1] + 5;
        chromosome[index] = Math.max(minOcc, Math.min(maxOcc, chromosome[index]));
    }

    /**
     * Ensure thresholds are in ascending order with minimum spacing
     */
    private void ensureValidThresholds(double[] thresholds) {
        Arrays.sort(thresholds);
        ensureMinimumSpacing(thresholds, 3.0);
    }

    /**
     * Ensure minimum spacing between thresholds
     */
    private void ensureMinimumSpacing(double[] thresholds, double minSpacing) {
        for (int i = 1; i < thresholds.length; i++) {
            if (thresholds[i] - thresholds[i-1] < minSpacing) {
                thresholds[i] = thresholds[i-1] + minSpacing;
            }
        }
    }

    /**
     * Classify all months based on best thresholds
     */
    private void classifyMonths() {
        monthSeasonMap = new HashMap<>();
        
        for (MonthlySeasonData data : monthlyData) {
            double occ = data.getOccupancyRate();
            String season;
            
            if (occ < bestThresholds[0]) {
                season = "LOW";
            } else if (occ < bestThresholds[1]) {
                season = "NORMAL";
            } else if (occ < bestThresholds[2]) {
                season = "PEAK";
            } else {
                season = "SUPER_PEAK";
            }
            
            monthSeasonMap.put(data.getMonthYear(), season);
            data.setClassifiedSeason(season);
        }
    }

    /**
     * Print classification results
     */
    private void printResults() {
        logger.debug("=== Season Classification Results ===");
        logger.debug("Optimal Thresholds:");
        logger.debug("  LOW:        < {}%", String.format("%.2f", bestThresholds[0]));
        logger.debug("  NORMAL:     {}% - {}%", String.format("%.2f", bestThresholds[0]),
                     String.format("%.2f", bestThresholds[1]));
        logger.debug("  PEAK:       {}% - {}%", String.format("%.2f", bestThresholds[1]),
                     String.format("%.2f", bestThresholds[2]));
        logger.debug("  SUPER_PEAK: >= {}%", String.format("%.2f", bestThresholds[2]));

        // Count distribution
        Map<String, Integer> counts = new HashMap<>();
        counts.put("LOW", 0);
        counts.put("NORMAL", 0);
        counts.put("PEAK", 0);
        counts.put("SUPER_PEAK", 0);

        for (String season : monthSeasonMap.values()) {
            counts.put(season, counts.get(season) + 1);
        }

        logger.debug("Season Distribution:");
        logger.debug("  LOW:        {} months", counts.get("LOW"));
        logger.debug("  NORMAL:     {} months", counts.get("NORMAL"));
        logger.debug("  PEAK:       {} months", counts.get("PEAK"));
        logger.debug("  SUPER_PEAK: {} months", counts.get("SUPER_PEAK"));

        logger.debug("Month Classifications:");
        for (MonthlySeasonData data : monthlyData) {
            logger.debug("  {} ({}%): {}", data.getMonthName(),
                         String.format("%.2f", data.getOccupancyRate()),
                         data.getClassifiedSeason());
        }

        logger.debug("=== Classification Complete ===");
    }

    // Getters
    public double[] getBestThresholds() {
        return bestThresholds;
    }

    public Map<String, String> getMonthSeasonMap() {
        return monthSeasonMap;
    }

    public double getThresholdLowNormal() {
        return bestThresholds != null ? bestThresholds[0] : 65.0;
    }

    public double getThresholdNormalPeak() {
        return bestThresholds != null ? bestThresholds[1] : 75.0;
    }

    public double getThresholdPeakSuperPeak() {
        return bestThresholds != null ? bestThresholds[2] : 85.0;
    }
}
