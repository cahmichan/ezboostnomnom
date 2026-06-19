package com.ezboost.ga;

import com.ezboost.dao.UserSettingsDAO;
import com.ezboost.model.Room;
import com.ezboost.model.Season;
import com.ezboost.model.UserMultiplierSettings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class GeneticAlgorithm {

    private static final Logger logger = LoggerFactory.getLogger(GeneticAlgorithm.class);

    // GA hyperparameters
    private static final int POPULATION_SIZE = 200;
    private static final int GENERATIONS = 600;
    private static final double CROSSOVER_RATE = 0.85;
    private static final double BASE_MUTATION_RATE = 0.15;
    private static final int ELITISM_COUNT = 10;

    // Season constants
    static final double DAYS_PER_SEASON = 365.0 / 4.0; // ~91.25

    // Default season multipliers
    static final double DEFAULT_LOW_MULTIPLIER = 0.85;
    static final double DEFAULT_NORMAL_MULTIPLIER = 1.0;
    static final double DEFAULT_PEAK_MULTIPLIER = 1.15;
    static final double DEFAULT_SUPER_PEAK_MULTIPLIER = 1.35;

    // Price bound scaling factors
    private static final double MIN_PRICE_SCALE = 0.85;
    private static final double MAX_PRICE_SCALE = 1.25;

    private final double expectedRevenue;
    private final List<Room> allRooms;
    private final Random random = new Random();
    private Long randomSeed;

    private double naturalRevenue = -1;
    private Map<String, Double> userMultipliers;
    private int userId = -1;
    private DemandCurve demandCurve;

    /**
     * Constructor: Accepts rooms as parameter (used by RunGA with RoomDataDAO)
     */
    public GeneticAlgorithm(double expectedRevenue, List<Room> rooms) {
        this.expectedRevenue = expectedRevenue;
        this.allRooms = rooms;
        this.userMultipliers = getDefaultMultipliers();
        
        if (allRooms == null || allRooms.isEmpty()) {
            throw new RuntimeException("Room data is empty! Please provide valid room data.");
        }
        
        logger.debug("[GeneticAlgorithm] Initialized with {} room types", allRooms.size());
        logger.debug("[GeneticAlgorithm] Target revenue: RM {}", String.format("%,.0f", expectedRevenue));
    }

    /**
     * Constructor 3: Accepts rooms AND userId to load user-specific multipliers
     */
    public GeneticAlgorithm(double expectedRevenue, List<Room> rooms, int userId) {
        this.expectedRevenue = expectedRevenue;
        this.allRooms = rooms;
        this.userId = userId;
        
        // Load user-specific multipliers from database
        this.userMultipliers = loadUserMultipliers(userId);
        
        if (allRooms == null || allRooms.isEmpty()) {
            throw new RuntimeException("Room data is empty! Please provide valid room data.");
        }
        
        logger.debug("[GeneticAlgorithm] Initialized with {} room types", allRooms.size());
        logger.debug("[GeneticAlgorithm] Target revenue: RM {}", String.format("%,.0f", expectedRevenue));
        logger.debug("[GeneticAlgorithm] User multipliers loaded for userId: {}", userId);
        logger.debug("[GeneticAlgorithm] Multipliers: LOW={}, NORMAL={}, PEAK={}, SUPER_PEAK={}",
                     userMultipliers.get("LOW"), userMultipliers.get("NORMAL"),
                     userMultipliers.get("PEAK"), userMultipliers.get("SUPER_PEAK"));
    }

    /**
     * Constructor 4: Accepts rooms, userId, AND demand curve for price-demand optimization
     */
    public GeneticAlgorithm(double expectedRevenue, List<Room> rooms, int userId, DemandCurve demandCurve) {
        this(expectedRevenue, rooms, userId);
        this.demandCurve = demandCurve;

        if (demandCurve != null) {
            logger.debug("[GeneticAlgorithm] Demand curve enabled: {}", demandCurve);
            logger.debug("[GeneticAlgorithm] Optimal price point: RM{}",
                         String.format("%.0f", demandCurve.getOptimalPrice()));
        }
    }

    /**
     * Creates a reproducible optimization run. The algorithm and constraints
     * remain unchanged; only the random source is made replayable.
     */
    public GeneticAlgorithm(double expectedRevenue, List<Room> rooms, int userId,
                            DemandCurve demandCurve, long randomSeed) {
        this(expectedRevenue, rooms, userId, demandCurve);
        this.random.setSeed(randomSeed);
        this.randomSeed = randomSeed;
    }

    public Long getRandomSeed() {
        return randomSeed;
    }

    /**
     * Load user multipliers from database
     */
    private Map<String, Double> loadUserMultipliers(int userId) {
        Map<String, Double> multipliers = new HashMap<>();
        
        try {
            List<UserMultiplierSettings> settings = UserSettingsDAO.getUserSettings(userId);
            
            for (UserMultiplierSettings setting : settings) {
                // Only use global season multipliers (no specific room or segment)
                if (setting.getRoomType() == null && setting.getSegmentName() == null) {
                    multipliers.put(setting.getSeasonName(), setting.getCustomMultiplier());
                }
            }
            
            logger.debug("[GeneticAlgorithm] Loaded {} multipliers from database", multipliers.size());
        } catch (Exception e) {
            logger.error("[GeneticAlgorithm] Error loading multipliers: {}", e.getMessage());
        }
        
        // Fill in defaults for any missing seasons
        if (!multipliers.containsKey("LOW")) multipliers.put("LOW", DEFAULT_LOW_MULTIPLIER);
        if (!multipliers.containsKey("NORMAL")) multipliers.put("NORMAL", DEFAULT_NORMAL_MULTIPLIER);
        if (!multipliers.containsKey("PEAK")) multipliers.put("PEAK", DEFAULT_PEAK_MULTIPLIER);
        if (!multipliers.containsKey("SUPER_PEAK")) multipliers.put("SUPER_PEAK", DEFAULT_SUPER_PEAK_MULTIPLIER);
        
        return multipliers;
    }

    /**
     * Get default multipliers
     */
    private Map<String, Double> getDefaultMultipliers() {
        Map<String, Double> defaults = new HashMap<>();
        defaults.put("LOW", DEFAULT_LOW_MULTIPLIER);
        defaults.put("NORMAL", DEFAULT_NORMAL_MULTIPLIER);
        defaults.put("PEAK", DEFAULT_PEAK_MULTIPLIER);
        defaults.put("SUPER_PEAK", DEFAULT_SUPER_PEAK_MULTIPLIER);
        return defaults;
    }

    /**
     * Get user multiplier for a season
     */
    private double getUserMultiplier(Season season) {
        return userMultipliers.getOrDefault(season.name(), 1.0);
    }

    public List<Room> runGA() {
        logger.debug("=== Hotel Revenue Optimization Started ===");
        logger.debug("Target Revenue: RM{}", String.format("%.0f", expectedRevenue));
        logger.debug("Room Types: {}", allRooms.size());

        // Show user multipliers being used
        logger.debug("=== User Multiplier Settings ===");
        for (Season season : Season.values()) {
            double mult = getUserMultiplier(season);
            logger.debug("{}: {}x", season, String.format("%.2f", mult));
        }

        // Show a sample room's data
        if (!allRooms.isEmpty()) {
            Room sampleRoom = allRooms.get(0);
            logger.debug("=== Sample Room Data ===");
            logger.debug("Room: {}", sampleRoom.getName());
            logger.debug("BaseADR: {}", sampleRoom.getBaseAdr());
            logger.debug("MinADR: {}", sampleRoom.getMinAdr());
            logger.debug("MaxADR: {}", sampleRoom.getMaxAdr());
            logger.debug("Occupancy: {}%", sampleRoom.getOccupancy());
            logger.debug("Total Rooms: {}", sampleRoom.getTotalRooms());
        }
        
        // Show achievability
        printAchievableRangeSummary();

        // Step 1: Run GA with elitism + adaptive mutation
        List<List<Room>> population = initializePopulation();

        // Target-aware scaling of initial population
        if (!population.isEmpty()) {
            double sampleRevenue = calculateEstimatedRevenue(population.get(0));
            if (sampleRevenue > 0) {
                double scaleFactor = expectedRevenue / sampleRevenue;
                logger.debug("[GeneticAlgorithm] Init scale factor: {}", String.format("%.4f", scaleFactor));
                for (List<Room> chromosome : population) {
                    for (Room room : chromosome) {
                        for (Season s : Season.values()) {
                            double current = room.getSeasonalPrices().get(s);
                            double scaled = current * scaleFactor;
                            double minP = getSeasonalMinPrice(room, s);
                            double maxP = getSeasonalMaxPrice(room, s);
                            scaled = Math.max(minP, Math.min(maxP, scaled));
                            room.setPriceForSeason(s, scaled);
                        }
                        enforceSeasonalPriceOrdering(room);
                    }
                }
            }
        }

        List<Room> bestSolution = null;
        double bestFitness = Double.NEGATIVE_INFINITY;

        // Evaluate initial population
        for (List<Room> chromosome : population) {
            double fitness = calculateFitness(chromosome);
            if (fitness > bestFitness) {
                bestFitness = fitness;
                bestSolution = deepCopyChromosome(chromosome);
            }
        }

        for (int generation = 0; generation < GENERATIONS; generation++) {
            // Adaptive mutation: high exploration early, low exploitation late
            double mutationRate = BASE_MUTATION_RATE + (1.0 - (double) generation / GENERATIONS) * 0.25;

            // Sort population by fitness (descending) for elitism
            population.sort((a, b) -> Double.compare(calculateFitness(b), calculateFitness(a)));

            List<List<Room>> newPopulation = new ArrayList<>();

            // Elitism: carry top chromosomes forward unchanged
            for (int e = 0; e < ELITISM_COUNT && e < population.size(); e++) {
                newPopulation.add(deepCopyChromosome(population.get(e)));
            }

            // Fill remaining slots via crossover/mutation
            while (newPopulation.size() < POPULATION_SIZE) {
                List<Room> parent1 = tournamentSelection(population, 5);
                List<Room> parent2 = tournamentSelection(population, 5);

                List<Room> child;
                if (random.nextDouble() < CROSSOVER_RATE) {
                    child = arithmeticCrossover(parent1, parent2);
                } else {
                    child = deepCopyChromosome(parent1);
                }

                if (random.nextDouble() < mutationRate) {
                    scrambleMutation(child);
                }

                newPopulation.add(child);
            }

            // Track best across all generations
            for (List<Room> chromosome : newPopulation) {
                double fitness = calculateFitness(chromosome);
                if (fitness > bestFitness) {
                    bestFitness = fitness;
                    bestSolution = deepCopyChromosome(chromosome);
                }
            }

            population = newPopulation;
        }

        // Step 2: Apply local search
        if (bestSolution != null) {
            double beforeRevenue = calculateEstimatedRevenue(bestSolution);
            
            logger.debug("=== Optimization Results ===");
            logger.debug("GA Result: RM{}", String.format("%.0f", beforeRevenue));
            logger.debug("Target: RM{}", String.format("%.0f", expectedRevenue));

            bestSolution = forceExactTarget(bestSolution);
            
            double afterRevenue = calculateEstimatedRevenue(bestSolution);
            double accuracy = 100 - (Math.abs(afterRevenue - expectedRevenue) / expectedRevenue * 100);
            
            logger.debug("Final Result: RM{}", String.format("%.0f", afterRevenue));
            logger.debug("Accuracy: {}%", String.format("%.3f", accuracy));
            logger.debug("=== Optimization Complete ===");
        }

        return bestSolution;
    }

    /**
     * Compute the "natural" revenue — what prices at baseADR * multiplier would produce.
     * Cached after first call.
     */
    private double computeNaturalRevenue() {
        if (naturalRevenue > 0) return naturalRevenue;

        double total = 0;
        for (Room room : allRooms) {
            for (Season season : Season.values()) {
                double price = room.getBaseAdr() * getUserMultiplier(season);
                if (demandCurve != null) {
                    double occ = demandCurve.getOccupancy(price) / 100.0;
                    total += price * occ * room.getTotalRooms() * DAYS_PER_SEASON;
                } else {
                    total += price * room.getTotalRooms() * (room.getOccupancy() / 100.0) * DAYS_PER_SEASON;
                }
            }
        }
        naturalRevenue = total;
        logger.debug("[GeneticAlgorithm] Natural revenue (at multiplier prices): RM{}",
                     String.format("%,.0f", naturalRevenue));
        return naturalRevenue;
    }

    /**
     * Ratio of target revenue to natural revenue — used to scale price bounds dynamically.
     */
    private double getTargetScaleRatio() {
        double nat = computeNaturalRevenue();
        if (nat <= 0) return 1.0;
        return expectedRevenue / nat;
    }

    /**
     * Seasonal min price calculation using USER MULTIPLIERS with dynamic scaling
     */
    private double getSeasonalMinPrice(Room room, Season season) {
        double basePrice = room.getBaseAdr();
        double userMult = getUserMultiplier(season);
        double ratio = getTargetScaleRatio();

        // Scale the multiplied price proportionally to the target
        double theoreticalMinPrice = basePrice * userMult * ratio * MIN_PRICE_SCALE;

        // Never go below absolute room minimum
        double constrainedMin = Math.max(room.getMinAdr(), theoreticalMinPrice);

        return constrainedMin;
    }

    /**
     * Seasonal max price calculation using USER MULTIPLIERS with dynamic scaling
     */
    private double getSeasonalMaxPrice(Room room, Season season) {
        double basePrice = room.getBaseAdr();
        double userMult = getUserMultiplier(season);
        double ratio = getTargetScaleRatio();

        // Scale the multiplied price proportionally to the target
        double theoreticalMaxPrice = basePrice * userMult * ratio * MAX_PRICE_SCALE;

        // Never exceed absolute room maximum
        double constrainedMax = Math.min(room.getMaxAdr(), theoreticalMaxPrice);

        // Ensure max is never less than min
        double minPrice = getSeasonalMinPrice(room, season);
        constrainedMax = Math.max(constrainedMax, minPrice + 1);

        return constrainedMax;
    }

    /**
     * Initialize population with prices influenced by user multipliers
     */
    private List<List<Room>> initializePopulation() {
        List<List<Room>> population = new ArrayList<>();

        for (int i = 0; i < POPULATION_SIZE; i++) {
            List<Room> chromosome = new ArrayList<>();

            for (Room r : allRooms) {
                Room room = new Room(r.getName(), r.getBaseAdr(), r.getMinAdr(), r.getMaxAdr(), r.getOccupancy(), r.getTotalRooms());
                
                for (Season s : Season.values()) {
                    double minPrice = getSeasonalMinPrice(room, s);
                    double maxPrice = getSeasonalMaxPrice(room, s);
                    
                    // Initialize near the user's multiplier target
                    double targetPrice = room.getBaseAdr() * getUserMultiplier(s);
                    targetPrice = Math.max(minPrice, Math.min(maxPrice, targetPrice));
                    
                    // Add some randomness (±10%)
                    double randomFactor = 0.9 + (random.nextDouble() * 0.2);
                    double price = targetPrice * randomFactor;
                    price = Math.max(minPrice, Math.min(maxPrice, price));
                    
                    room.setPriceForSeason(s, price);
                }
                
                enforceSeasonalPriceOrdering(room);
                chromosome.add(room);
            }

            population.add(chromosome);
        }

        return population;
    }
    
    private double calculateFitness(List<Room> chromosome) {
        double estimated = calculateEstimatedRevenue(chromosome);
        return -Math.abs(estimated - expectedRevenue);
    }
    
    private List<Room> tournamentSelection(List<List<Room>> population, int tournamentSize) {
        List<Room> best = null;
        double bestFitness = Double.NEGATIVE_INFINITY;
        
        for (int i = 0; i < tournamentSize; i++) {
            List<Room> candidate = population.get(random.nextInt(population.size()));
            double fitness = calculateFitness(candidate);
            if (fitness > bestFitness) {
                bestFitness = fitness;
                best = candidate;
            }
        }
        
        return best;
    }

    private List<Room> arithmeticCrossover(List<Room> parent1, List<Room> parent2) {
        List<Room> child = new ArrayList<>();
        
        for (int i = 0; i < parent1.size(); i++) {
            Room room1 = parent1.get(i);
            Room room2 = parent2.get(i);
            
            Room childRoom = new Room(
                room1.getName(),
                room1.getBaseAdr(),
                room1.getMinAdr(),
                room1.getMaxAdr(),
                room1.getOccupancy(),
                room1.getTotalRooms()
            );
            
            for (Season season : Season.values()) {
                double parent1Price = room1.getSeasonalPrices().get(season);
                double parent2Price = room2.getSeasonalPrices().get(season);
                double averagePrice = (parent1Price + parent2Price) / 2.0; 
                
                double minPrice = getSeasonalMinPrice(childRoom, season);
                double maxPrice = getSeasonalMaxPrice(childRoom, season);
                averagePrice = Math.max(minPrice, Math.min(maxPrice, averagePrice));
                
                childRoom.setPriceForSeason(season, averagePrice);
            }
            
            enforceSeasonalPriceOrdering(childRoom);
            child.add(childRoom);
        }
        
        return child;
    }

    private void scrambleMutation(List<Room> chromosome) {
        int index = random.nextInt(chromosome.size());
        Room room = chromosome.get(index);

        for (Season season : Season.values()) {
            double minPrice = getSeasonalMinPrice(room, season);
            double maxPrice = getSeasonalMaxPrice(room, season);
            double price = minPrice + random.nextDouble() * (maxPrice - minPrice);
            room.setPriceForSeason(season, price);
        }
        
        enforceSeasonalPriceOrdering(room);
    }

    private List<Room> forceExactTarget(List<Room> solution) {
        List<Room> result = deepCopyChromosome(solution);
        double currentRevenue = calculateEstimatedRevenue(result);

        logger.debug("Starting local search with revenue: RM{}", String.format("%.0f", currentRevenue));

        if (currentRevenue <= 0) {
            return result;
        }

        double requiredScale = expectedRevenue / currentRevenue;
        logger.debug("Required scale factor: {}", String.format("%.6f", requiredScale));

        // Check if direct scaling is possible (only when no demand curve — scaling is linear)
        boolean canScaleDirectly = (demandCurve == null);
        if (canScaleDirectly) {
            for (Room room : result) {
                for (Season season : Season.values()) {
                    double currentPrice = room.getSeasonalPrices().get(season);
                    double targetPrice = currentPrice * requiredScale;

                    double minPrice = getSeasonalMinPrice(room, season);
                    double maxPrice = getSeasonalMaxPrice(room, season);

                    if (targetPrice < minPrice || targetPrice > maxPrice) {
                        canScaleDirectly = false;
                        break;
                    }
                }
                if (!canScaleDirectly) break;
            }
        }

        if (canScaleDirectly) {
            logger.debug("Applying direct scaling");
            for (Room room : result) {
                for (Season season : Season.values()) {
                    double currentPrice = room.getSeasonalPrices().get(season);
                    double targetPrice = currentPrice * requiredScale;
                    room.setPriceForSeason(season, targetPrice);
                }
                enforceSeasonalPriceOrdering(room);
            }
        } else {
            logger.debug("Using iterative adjustment{}",
                         demandCurve != null ? " (demand curve active)" : "");
            double bestErrorAbs = Double.MAX_VALUE;
            List<Room> bestLocalSolution = deepCopyChromosome(result);
            int convergenceCount = 0;

            double initialErrorPercent = Math.abs(expectedRevenue - currentRevenue) / expectedRevenue;

            for (int iteration = 0; iteration < 5000; iteration++) {
                currentRevenue = calculateEstimatedRevenue(result);
                double error = expectedRevenue - currentRevenue;
                double errorPercent = Math.abs(error) / expectedRevenue;

                if (Math.abs(error) < bestErrorAbs) {
                    bestErrorAbs = Math.abs(error);
                    bestLocalSolution = deepCopyChromosome(result);
                    convergenceCount = 0;
                } else {
                    convergenceCount++;
                }

                if (errorPercent < 0.001) {
                    logger.debug("Converged at iteration {} (error: {}%)", iteration,
                                String.format("%.4f", errorPercent * 100));
                    break;
                }

                if (convergenceCount > 200) {
                    logger.debug("No improvement for 200 iterations, using best solution");
                    result = bestLocalSolution;
                    break;
                }

                // Adaptive step: large when far from target, small when close
                double stepPercent = Math.max(0.005, 0.05 * (errorPercent / Math.max(0.001, initialErrorPercent)));

                // Find best adjustment
                double bestImpact = 0;
                Room bestRoom = null;
                Season bestSeason = null;
                double bestNewPrice = 0;

                for (Room room : result) {
                    for (Season season : Season.values()) {
                        double currentPrice = room.getSeasonalPrices().get(season);
                        double minPrice = getSeasonalMinPrice(room, season);
                        double maxPrice = getSeasonalMaxPrice(room, season);
                        double adjustmentStep = currentPrice * stepPercent;

                        if (demandCurve != null) {
                            double optimalPrice = demandCurve.getOptimalPrice();

                            if (error > 0 && currentPrice < maxPrice) {
                                boolean movesCorrectDirection = (currentPrice < optimalPrice);
                                if (!movesCorrectDirection) continue;

                                adjustmentStep = Math.min(maxPrice - currentPrice, adjustmentStep);
                                double newPrice = currentPrice + adjustmentStep;
                                double oldRev = currentPrice * demandCurve.getOccupancy(currentPrice) / 100.0
                                        * room.getTotalRooms() * DAYS_PER_SEASON;
                                double newRev = newPrice * demandCurve.getOccupancy(newPrice) / 100.0
                                        * room.getTotalRooms() * DAYS_PER_SEASON;
                                double potentialImpact = newRev - oldRev;

                                if (potentialImpact > bestImpact) {
                                    bestImpact = potentialImpact;
                                    bestRoom = room;
                                    bestSeason = season;
                                    bestNewPrice = newPrice;
                                }
                            } else if (error < 0 && currentPrice > minPrice) {
                                boolean movesCorrectDirection = (currentPrice > optimalPrice);
                                if (!movesCorrectDirection) {
                                    adjustmentStep = Math.min(currentPrice - minPrice, adjustmentStep);
                                    double newPrice = currentPrice - adjustmentStep;
                                    double oldRev = currentPrice * demandCurve.getOccupancy(currentPrice) / 100.0
                                            * room.getTotalRooms() * DAYS_PER_SEASON;
                                    double newRev = newPrice * demandCurve.getOccupancy(newPrice) / 100.0
                                            * room.getTotalRooms() * DAYS_PER_SEASON;
                                    double potentialImpact = oldRev - newRev;

                                    if (potentialImpact > bestImpact) {
                                        bestImpact = potentialImpact;
                                        bestRoom = room;
                                        bestSeason = season;
                                        bestNewPrice = newPrice;
                                    }
                                } else {
                                    adjustmentStep = Math.min(maxPrice - currentPrice, adjustmentStep);
                                    if (adjustmentStep > 0) {
                                        double newPrice = currentPrice + adjustmentStep;
                                        double oldRev = currentPrice * demandCurve.getOccupancy(currentPrice) / 100.0
                                                * room.getTotalRooms() * DAYS_PER_SEASON;
                                        double newRev = newPrice * demandCurve.getOccupancy(newPrice) / 100.0
                                                * room.getTotalRooms() * DAYS_PER_SEASON;
                                        double potentialImpact = oldRev - newRev;

                                        if (potentialImpact > bestImpact) {
                                            bestImpact = potentialImpact;
                                            bestRoom = room;
                                            bestSeason = season;
                                            bestNewPrice = newPrice;
                                        }
                                    }
                                }
                            }
                        } else {
                            double seasonalOccupancy = room.getTotalRooms() * (room.getOccupancy() / 100.0) * DAYS_PER_SEASON;

                            if (error > 0 && currentPrice < maxPrice) {
                                double maxIncrease = maxPrice - currentPrice;
                                double adjStep = Math.min(maxIncrease, adjustmentStep);
                                double potentialImpact = adjStep * seasonalOccupancy;

                                if (potentialImpact > bestImpact) {
                                    bestImpact = potentialImpact;
                                    bestRoom = room;
                                    bestSeason = season;
                                    bestNewPrice = currentPrice + adjStep;
                                }
                            } else if (error < 0 && currentPrice > minPrice) {
                                double maxDecrease = currentPrice - minPrice;
                                double adjStep = Math.min(maxDecrease, adjustmentStep);
                                double potentialImpact = adjStep * seasonalOccupancy;

                                if (potentialImpact > bestImpact) {
                                    bestImpact = potentialImpact;
                                    bestRoom = room;
                                    bestSeason = season;
                                    bestNewPrice = currentPrice - adjStep;
                                }
                            }
                        }
                    }
                }

                if (bestRoom != null && bestImpact > 0) {
                    bestRoom.setPriceForSeason(bestSeason, bestNewPrice);
                    enforceSeasonalPriceOrdering(bestRoom);
                } else {
                    break;
                }
            }
        }

        // Post-optimization: stamp per-season occupancies onto Room objects
        if (demandCurve != null) {
            for (Room room : result) {
                for (Season season : Season.values()) {
                    double price = room.getSeasonalPrices().get(season);
                    room.setOccupancyForSeason(season, demandCurve.getOccupancy(price));
                }
            }
        }

        return result;
    }

    private List<Room> deepCopyChromosome(List<Room> original) {
        List<Room> copy = new ArrayList<>();
        for (Room r : original) {
            Room newRoom = new Room(r.getName(), r.getBaseAdr(), r.getMinAdr(), r.getMaxAdr(), r.getOccupancy(), r.getTotalRooms());
            for (Season s : Season.values()) {
                newRoom.setPriceForSeason(s, r.getSeasonalPrices().get(s));
            }
            if (r.hasSeasonalOccupancies()) {
                for (Season s : Season.values()) {
                    newRoom.setOccupancyForSeason(s, r.getOccupancyForSeason(s));
                }
            }
            copy.add(newRoom);
        }
        return copy;
    }

    public double calculateEstimatedRevenue(List<Room> chromosome) {
        if (demandCurve == null) {
            return chromosome.stream().mapToDouble(Room::getEstimatedRevenue).sum();
        }

        double total = 0;
        for (Room room : chromosome) {
            for (Season season : Season.values()) {
                double price = room.getSeasonalPrices().getOrDefault(season, room.getBaseAdr());
                double occ = demandCurve.getOccupancy(price) / 100.0;
                total += price * occ * room.getTotalRooms() * DAYS_PER_SEASON;
            }
        }
        return total;
    }

    private void printAchievableRangeSummary() {
        double[] range = calculateAchievableRange();
        logger.debug("Achievable Range: RM{} - RM{}", String.format("%.0f", range[0]),
                     String.format("%.0f", range[1]));

        double tolerance = 0.05;
        double lowerBound = range[0] * (1.0 - tolerance);
        double upperBound = range[1] * (1.0 + tolerance);

        if (expectedRevenue >= lowerBound && expectedRevenue <= upperBound) {
            logger.debug("Target is achievable within tolerance");
        } else {
            logger.debug("Target requires adaptive constraints");
            if (expectedRevenue < lowerBound) {
                logger.debug("  Target is {}% below minimum",
                             String.format("%.1f", ((range[0] - expectedRevenue) / expectedRevenue * 100)));
            } else {
                logger.debug("  Target is {}% above maximum",
                             String.format("%.1f", ((expectedRevenue - range[1]) / expectedRevenue * 100)));
            }
        }
    }
    
    private double[] calculateAchievableRange() {
        double minRevenue = 0;
        double maxRevenue = 0;

        for (Room room : allRooms) {
            for (Season season : Season.values()) {
                double minPrice = getSeasonalMinPrice(room, season);
                double maxPrice = getSeasonalMaxPrice(room, season);

                if (demandCurve != null) {
                    // With demand curve: max revenue is at the optimal price (clamped to bounds)
                    double optPrice = Math.max(minPrice, Math.min(maxPrice, demandCurve.getOptimalPrice()));
                    double optRev = optPrice * demandCurve.getOccupancy(optPrice) / 100.0
                            * room.getTotalRooms() * DAYS_PER_SEASON;

                    // Min revenue is at whichever bound produces less
                    double revAtMin = minPrice * demandCurve.getOccupancy(minPrice) / 100.0
                            * room.getTotalRooms() * DAYS_PER_SEASON;
                    double revAtMax = maxPrice * demandCurve.getOccupancy(maxPrice) / 100.0
                            * room.getTotalRooms() * DAYS_PER_SEASON;

                    maxRevenue += optRev;
                    minRevenue += Math.min(revAtMin, revAtMax);
                } else {
                    double seasonalRevenue = room.getTotalRooms() * (room.getOccupancy() / 100.0) * DAYS_PER_SEASON;
                    minRevenue += minPrice * seasonalRevenue;
                    maxRevenue += maxPrice * seasonalRevenue;
                }
            }
        }

        return new double[]{minRevenue, maxRevenue};
    }
    
    private void enforceSeasonalPriceOrdering(Room room) {
        Season[] seasons = {Season.LOW, Season.NORMAL, Season.PEAK, Season.SUPER_PEAK};
        double[] mins = new double[seasons.length];
        double[] maxs = new double[seasons.length];
        double[] prices = new double[seasons.length];

        for (int i = 0; i < seasons.length; i++) {
            mins[i] = getSeasonalMinPrice(room, seasons[i]);
            maxs[i] = getSeasonalMaxPrice(room, seasons[i]);
            prices[i] = clamp(room.getSeasonalPrices().get(seasons[i]), mins[i], maxs[i]);
        }

        double gap = 1.0;
        for (int attempt = 0; attempt < 8; attempt++) {
            double[] adjusted = prices.clone();

            adjusted[0] = clamp(adjusted[0], mins[0], maxs[0]);
            for (int i = 1; i < adjusted.length; i++) {
                adjusted[i] = clamp(Math.max(adjusted[i], adjusted[i - 1] + gap), mins[i], maxs[i]);
            }

            for (int i = adjusted.length - 2; i >= 0; i--) {
                adjusted[i] = clamp(Math.min(adjusted[i], adjusted[i + 1] - gap), mins[i], maxs[i]);
            }

            boolean ordered = true;
            for (int i = 1; i < adjusted.length; i++) {
                if (adjusted[i] + 1e-9 < adjusted[i - 1] + gap) {
                    ordered = false;
                    break;
                }
            }

            prices = adjusted;
            if (ordered) {
                break;
            }

            gap /= 2.0;
        }

        for (int i = 0; i < seasons.length; i++) {
            room.setPriceForSeason(seasons[i], clamp(prices[i], mins[i], maxs[i]));
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
    
    // Getter for user multipliers (for display purposes)
    public Map<String, Double> getUserMultipliers() {
        return new HashMap<>(userMultipliers);
    }

    public DemandCurve getDemandCurve() {
        return demandCurve;
    }

    public double[] getAchievableRange() {
        return calculateAchievableRange().clone();
    }

    public double getSeasonalMinPriceForDisplay(Room room, Season season) {
        return getSeasonalMinPrice(room, season);
    }

    public double getSeasonalMaxPriceForDisplay(Room room, Season season) {
        return getSeasonalMaxPrice(room, season);
    }

    public boolean isFallbackDemandCurve() {
        return demandCurve != null
                && Math.abs(demandCurve.getIntercept() - 100.0) < 0.0001
                && Math.abs(demandCurve.getSlope() + 0.05) < 0.0001
                && Math.abs(demandCurve.getRSquared()) < 0.0001;
    }
}
