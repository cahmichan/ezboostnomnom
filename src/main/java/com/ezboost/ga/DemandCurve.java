package com.ezboost.ga;

import com.ezboost.model.MonthlySeasonData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Linear demand model: occupancy = intercept + slope * price
 * Higher price -> lower occupancy (slope is negative).
 * Revenue per room-night = price * occupancy/100, which is quadratic in price
 * and has a genuine maximum at -intercept / (2*slope).
 */
public class DemandCurve {

    private static final Logger logger = LoggerFactory.getLogger(DemandCurve.class);

    private final double intercept;
    private final double slope;
    private final double rSquared;

    private DemandCurve(double intercept, double slope, double rSquared) {
        this.intercept = intercept;
        this.slope = slope;
        this.rSquared = rSquared;
    }

    /**
     * Fit a linear demand curve from historical (avgRoomRate, occupancyRate) data.
     * Falls back to default curve if fewer than 3 valid points or positive slope.
     */
    public static DemandCurve fitFromData(List<MonthlySeasonData> data) {
        if (data == null || data.size() < 3) {
            logger.debug("[DemandCurve] Insufficient data ({} points), using default curve",
                         data == null ? 0 : data.size());
            return createDefault();
        }

        // Filter to valid points (positive rate and occupancy)
        double[] prices = new double[data.size()];
        double[] occupancies = new double[data.size()];
        int n = 0;

        for (MonthlySeasonData d : data) {
            if (d.getAvgRoomRate() > 0 && d.getOccupancyRate() > 0) {
                prices[n] = d.getAvgRoomRate();
                occupancies[n] = d.getOccupancyRate();
                n++;
            }
        }

        if (n < 3) {
            logger.debug("[DemandCurve] Only {} valid data points, using default curve", n);
            return createDefault();
        }

        // Least-squares linear regression: occ = intercept + slope * price
        double sumX = 0, sumY = 0, sumXX = 0, sumXY = 0;
        for (int i = 0; i < n; i++) {
            sumX += prices[i];
            sumY += occupancies[i];
            sumXX += prices[i] * prices[i];
            sumXY += prices[i] * occupancies[i];
        }

        double meanX = sumX / n;
        double meanY = sumY / n;
        double slopeNumer = sumXY - n * meanX * meanY;
        double slopeDenom = sumXX - n * meanX * meanX;

        if (Math.abs(slopeDenom) < 1e-10) {
            logger.debug("[DemandCurve] Degenerate data (all same price), using default curve");
            return createDefault();
        }

        double fitSlope = slopeNumer / slopeDenom;
        double fitIntercept = meanY - fitSlope * meanX;

        // Validate: slope must be negative (higher price -> lower demand)
        if (fitSlope >= 0) {
            logger.debug("[DemandCurve] WARNING: Positive slope ({}) - data suggests higher price = higher occupancy. Using default curve.",
                         String.format("%.6f", fitSlope));
            return createDefault();
        }

        // Calculate R-squared
        double ssTot = 0, ssRes = 0;
        for (int i = 0; i < n; i++) {
            double predicted = fitIntercept + fitSlope * prices[i];
            ssRes += (occupancies[i] - predicted) * (occupancies[i] - predicted);
            ssTot += (occupancies[i] - meanY) * (occupancies[i] - meanY);
        }
        double r2 = (ssTot > 0) ? 1.0 - ssRes / ssTot : 0.0;

        DemandCurve curve = new DemandCurve(fitIntercept, fitSlope, r2);

        logger.debug("[DemandCurve] Fitted from {} data points", n);
        logger.debug("[DemandCurve] intercept={}, slope={}", String.format("%.4f", fitIntercept),
                     String.format("%.6f", fitSlope));
        logger.debug("[DemandCurve] R-squared={}", String.format("%.4f", r2));
        logger.debug("[DemandCurve] Sample: at RM300 -> {}% occ, at RM600 -> {}% occ",
                     String.format("%.1f", curve.getOccupancy(300)),
                     String.format("%.1f", curve.getOccupancy(600)));
        logger.debug("[DemandCurve] Optimal price (max rev/room-night): RM{}",
                     String.format("%.0f", curve.getOptimalPrice()));

        return curve;
    }

    /**
     * Fallback curve with reasonable industry assumptions.
     * intercept=100, slope=-0.05 means:
     *   at RM400 -> 80% occupancy
     *   at RM1000 -> 50% occupancy
     */
    public static DemandCurve createDefault() {
        logger.debug("[DemandCurve] Using default curve: intercept=100, slope=-0.05");
        return new DemandCurve(100.0, -0.05, 0.0);
    }

    /**
     * Get predicted occupancy (%) for a given price, clamped to [5%, 98%].
     */
    public double getOccupancy(double price) {
        double occ = intercept + slope * price;
        return Math.max(5.0, Math.min(98.0, occ));
    }

    /**
     * The price that maximizes revenue per room-night.
     * Revenue = price * (intercept + slope * price) / 100
     * d(Revenue)/d(price) = (intercept + 2*slope*price) / 100 = 0
     * => optimal price = -intercept / (2 * slope)
     */
    public double getOptimalPrice() {
        if (slope >= 0) return Double.MAX_VALUE;
        return -intercept / (2.0 * slope);
    }

    public double getIntercept() {
        return intercept;
    }

    public double getSlope() {
        return slope;
    }

    public double getRSquared() {
        return rSquared;
    }

    @Override
    public String toString() {
        return "DemandCurve{intercept=" + String.format("%.4f", intercept) +
                ", slope=" + String.format("%.6f", slope) +
                ", R²=" + String.format("%.4f", rSquared) + "}";
    }
}
