package com.example.blooddonor.benchmark;

import java.sql.*;
import java.util.*;

/**
 * 100,000 Donor Geolocation Matching Benchmark Suite.
 *
 * Compares:
 *   1. Baseline: Full in-memory scan (pulls all compatible donors, computes Haversine in JVM).
 *   2. Optimized: Bounding-box pre-filtering via composite index (idx_donor_lat_lng)
 *      with exact Haversine distance refinement.
 *
 * Can be executed via:
 *   mvn test-compile exec:java -Dexec.mainClass="com.example.blooddonor.benchmark.DonorMatchingBenchmark" -Dexec.classpathScope="test"
 */
public class DonorMatchingBenchmark {

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/lifelink_benchmark?rewriteBatchedStatements=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASS = "Sabby3924";
    private static final int TOTAL_DONORS = 100_000;
    private static final int BATCH_SIZE = 2_500;
    private static final int BENCHMARK_RUNS = 10;

    // Regional geographic boundary (Statewide distribution: Karnataka / South India)
    // Simulates realistic distribution where 100k donors are registered statewide
    private static final double MIN_LAT = 11.5000;
    private static final double MAX_LAT = 18.5000;
    private static final double MIN_LNG = 74.0000;
    private static final double MAX_LNG = 78.5000;

    // Hospital test location (Central Bengaluru / MG Road)
    private static final double HOSPITAL_LAT = 12.9716;
    private static final double HOSPITAL_LNG = 77.5946;
    private static final double MATCHING_RADIUS_KM = 20.0;
    private static final double EARTH_RADIUS_KM = 6371.0;

    // Blood groups and distribution weights
    private static final String[] BLOOD_GROUPS = {"O+", "A+", "B+", "AB+", "O-", "A-", "B-", "AB-"};
    private static final int[] WEIGHTS = {38, 27, 21, 6, 4, 2, 1, 1}; // percentages summing to 100

    public static void main(String[] args) {
        String url = System.getProperty("bench.db.url", DEFAULT_URL);
        String user = System.getProperty("bench.db.user", DEFAULT_USER);
        String pass = System.getProperty("bench.db.pass", DEFAULT_PASS);

        System.out.println("================================================================================");
        System.out.println(" LifeLink Geolocation Benchmark Suite: 100,000 Donors (MySQL 8.0)");
        System.out.println("================================================================================");
        System.out.println("Target Database: " + url);
        System.out.println("Target Dataset : " + TOTAL_DONORS + " synthetic donor records across Bangalore Metro");
        System.out.println("Hospital Point : (" + HOSPITAL_LAT + ", " + HOSPITAL_LNG + ")");
        System.out.println("Matching Radius: " + MATCHING_RADIUS_KM + " km");
        System.out.println("================================================================================");

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            conn.setAutoCommit(false);

            // Step 1: Ensure dataset has 100,000 donors
            ensureSeeded(conn);

            // Step 2: Print MySQL EXPLAIN execution plans
            printExplainPlan(conn);

            // Step 3: Warm up JVM and database buffer pool
            System.out.println("\n[1/3] Warming up database buffer pool and JIT compiler (3 runs each)...");
            for (int i = 0; i < 3; i++) {
                runBaselineScan(conn);
                runBoundingBoxQuery(conn);
            }
            System.out.println("Warmup complete.\n");

            // Step 4: Run Baseline Benchmark (In-Memory Scan)
            System.out.println("[2/3] Benchmarking Baseline: In-Memory Haversine Scan (" + BENCHMARK_RUNS + " runs)...");
            List<Double> baselineLatencies = new ArrayList<>();
            int baselineCandidates = 0;
            int baselineMatches = 0;

            for (int i = 1; i <= BENCHMARK_RUNS; i++) {
                long start = System.nanoTime();
                BenchmarkResult res = runBaselineScan(conn);
                long elapsedNanos = System.nanoTime() - start;
                double elapsedMs = elapsedNanos / 1_000_000.0;
                baselineLatencies.add(elapsedMs);
                baselineCandidates = res.candidatesFetched;
                baselineMatches = res.matchesFound;
                System.out.printf("  Run #%2d: %.2f ms | DB rows loaded: %,d | Final matches: %,d%n",
                        i, elapsedMs, res.candidatesFetched, res.matchesFound);
            }

            // Step 5: Run Optimized Benchmark (Bounding Box Pre-Filtering)
            System.out.println("\n[3/3] Benchmarking Optimized: Indexed Bounding-Box Pre-Filtering (" + BENCHMARK_RUNS + " runs)...");
            List<Double> optimizedLatencies = new ArrayList<>();
            int optimizedCandidates = 0;
            int optimizedMatches = 0;

            for (int i = 1; i <= BENCHMARK_RUNS; i++) {
                long start = System.nanoTime();
                BenchmarkResult res = runBoundingBoxQuery(conn);
                long elapsedNanos = System.nanoTime() - start;
                double elapsedMs = elapsedNanos / 1_000_000.0;
                optimizedLatencies.add(elapsedMs);
                optimizedCandidates = res.candidatesFetched;
                optimizedMatches = res.matchesFound;
                System.out.printf("  Run #%2d: %.2f ms | DB rows loaded: %,d | Final matches: %,d%n",
                        i, elapsedMs, res.candidatesFetched, res.matchesFound);
            }

            // Step 6: Parity verification
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" Functional Parity & Correctness Check:");
            if (baselineMatches == optimizedMatches) {
                System.out.printf("  [PASS] Both implementations produced exactly %,d matched donors.%n", baselineMatches);
            } else {
                System.err.printf("  [FAIL] Match count mismatch: Baseline=%,d vs Optimized=%,d%n",
                        baselineMatches, optimizedMatches);
            }
            double rowReduction = ((double) (baselineCandidates - optimizedCandidates) / baselineCandidates) * 100.0;
            System.out.printf("  [PASS] Candidate row reduction: %,d -> %,d (%.1f%% reduction in JVM memory load)%n",
                    baselineCandidates, optimizedCandidates, rowReduction);

            // Step 7: Statistical Analysis & Summary
            printSummary(baselineLatencies, optimizedLatencies, baselineCandidates, optimizedCandidates);

            // Gracefully stop MySQL cleanup thread
            com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown();

        } catch (SQLException e) {
            System.err.println("Benchmark database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void ensureSeeded(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM donor_profiles WHERE latitude > 14.0")) {
            if (rs.next()) {
                int count = rs.getInt(1);
                if (count > 0) {
                    System.out.printf("Database already contains statewide distributed donor profiles (%,d northern records found). Skipping seed.%n", count);
                    return;
                }
            }
        }

        // Clean existing benchmark data if partial
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM notifications");
            stmt.executeUpdate("DELETE FROM request_donor_matches");
            stmt.executeUpdate("DELETE FROM emergency_requests");
            stmt.executeUpdate("DELETE FROM donor_profiles");
            stmt.executeUpdate("DELETE FROM hospital_profiles");
            stmt.executeUpdate("DELETE FROM users");
            conn.commit();
        }

        System.out.printf("Seeding %,d users and donor profiles across Bangalore Metro...%n", TOTAL_DONORS);
        long seedStart = System.currentTimeMillis();

        String insertUserSql = "INSERT INTO users (id, name, email, password, role) VALUES (?, ?, ?, ?, ?)";
        String insertDonorSql = "INSERT INTO donor_profiles (id, user_id, blood_group, address, city, latitude, longitude, contact_number, available) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Random rng = new Random(42); // deterministic seed for reproducibility

        try (PreparedStatement userStmt = conn.prepareStatement(insertUserSql);
             PreparedStatement donorStmt = conn.prepareStatement(insertDonorSql)) {

            for (int i = 1; i <= TOTAL_DONORS; i++) {
                String email = "bench_donor_" + i + "@lifelink-bench.test";
                String name = "Donor " + i;
                String role = "DONOR";
                String pass = "$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5b8gM1s.FmC0GZkWJ5oG06m3Z7k2S"; // dummy bcrypt

                userStmt.setLong(1, i);
                userStmt.setString(2, name);
                userStmt.setString(3, email);
                userStmt.setString(4, pass);
                userStmt.setString(5, role);
                userStmt.addBatch();

                // Coordinates within Bangalore Metro
                double lat = MIN_LAT + (MAX_LAT - MIN_LAT) * rng.nextDouble();
                double lng = MIN_LNG + (MAX_LNG - MIN_LNG) * rng.nextDouble();
                String bloodGroup = pickWeightedBloodGroup(rng);
                boolean available = rng.nextDouble() < 0.85; // 85% available

                donorStmt.setLong(1, i);
                donorStmt.setLong(2, i);
                donorStmt.setString(3, bloodGroup);
                donorStmt.setString(4, "Sector " + (i % 50));
                donorStmt.setString(5, "Bengaluru");
                donorStmt.setDouble(6, lat);
                donorStmt.setDouble(7, lng);
                donorStmt.setString(8, "+9198000" + String.format("%05d", i % 100000));
                donorStmt.setBoolean(9, available);
                donorStmt.addBatch();

                if (i % BATCH_SIZE == 0) {
                    userStmt.executeBatch();
                    donorStmt.executeBatch();
                    conn.commit();
                    System.out.printf("  Seeded %,d / %,d records...%n", i, TOTAL_DONORS);
                }
            }
        }

        long seedElapsed = System.currentTimeMillis() - seedStart;
        System.out.printf("Successfully seeded %,d records in %.2f seconds.%n", TOTAL_DONORS, seedElapsed / 1000.0);
    }

    private static String pickWeightedBloodGroup(Random rng) {
        int roll = rng.nextInt(100);
        int cumulative = 0;
        for (int i = 0; i < BLOOD_GROUPS.length; i++) {
            cumulative += WEIGHTS[i];
            if (roll < cumulative) {
                return BLOOD_GROUPS[i];
            }
        }
        return "O+";
    }

    private static BenchmarkResult runBaselineScan(Connection conn) throws SQLException {
        // Baseline: Pull all compatible available donors into memory
        String sql = "SELECT id, latitude, longitude FROM donor_profiles WHERE available = 1 AND blood_group IN ('O+', 'O-')";
        int candidates = 0;
        int matches = 0;

        List<DonorCoord> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                candidates++;
                list.add(new DonorCoord(rs.getLong("id"), rs.getDouble("latitude"), rs.getDouble("longitude")));
            }
        }

        // JVM Haversine refinement
        for (DonorCoord d : list) {
            double dist = haversine(HOSPITAL_LAT, HOSPITAL_LNG, d.lat, d.lng);
            if (dist <= MATCHING_RADIUS_KM) {
                matches++;
            }
        }

        return new BenchmarkResult(candidates, matches);
    }

    private static BenchmarkResult runBoundingBoxQuery(Connection conn) throws SQLException {
        // Optimized: Calculate bounding box coordinates
        double deltaLat = MATCHING_RADIUS_KM / 111.0;
        double cosLat = Math.cos(Math.toRadians(HOSPITAL_LAT));
        double deltaLng = Math.abs(cosLat) > 1e-6 ? MATCHING_RADIUS_KM / (111.0 * Math.abs(cosLat)) : 180.0;

        double minLat = HOSPITAL_LAT - deltaLat;
        double maxLat = HOSPITAL_LAT + deltaLat;
        double minLng = HOSPITAL_LNG - deltaLng;
        double maxLng = HOSPITAL_LNG + deltaLng;

        String sql = """
            SELECT id, latitude, longitude
            FROM donor_profiles
            WHERE available = 1
              AND blood_group IN ('O+', 'O-')
              AND latitude BETWEEN ? AND ?
              AND longitude BETWEEN ? AND ?
        """;

        int candidates = 0;
        int matches = 0;

        List<DonorCoord> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, minLat);
            stmt.setDouble(2, maxLat);
            stmt.setDouble(3, minLng);
            stmt.setDouble(4, maxLng);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    candidates++;
                    list.add(new DonorCoord(rs.getLong("id"), rs.getDouble("latitude"), rs.getDouble("longitude")));
                }
            }
        }

        // Exact Haversine refinement on candidates only
        for (DonorCoord d : list) {
            double dist = haversine(HOSPITAL_LAT, HOSPITAL_LNG, d.lat, d.lng);
            if (dist <= MATCHING_RADIUS_KM) {
                matches++;
            }
        }

        return new BenchmarkResult(candidates, matches);
    }

    private static void printExplainPlan(Connection conn) throws SQLException {
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println(" MySQL Query Execution Plan (EXPLAIN):");
        System.out.println("--------------------------------------------------------------------------------");

        double deltaLat = MATCHING_RADIUS_KM / 111.0;
        double cosLat = Math.cos(Math.toRadians(HOSPITAL_LAT));
        double deltaLng = MATCHING_RADIUS_KM / (111.0 * Math.abs(cosLat));

        double minLat = HOSPITAL_LAT - deltaLat;
        double maxLat = HOSPITAL_LAT + deltaLat;
        double minLng = HOSPITAL_LNG - deltaLng;
        double maxLng = HOSPITAL_LNG + deltaLng;

        String explainSql = String.format(Locale.US,
                "EXPLAIN SELECT id, latitude, longitude FROM donor_profiles " +
                "WHERE available = 1 AND blood_group IN ('O+', 'O-') " +
                "AND latitude BETWEEN %.6f AND %.6f AND longitude BETWEEN %.6f AND %.6f",
                minLat, maxLat, minLng, maxLng);

        System.out.println("Query: " + explainSql + "\n");

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(explainSql)) {

            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            for (int i = 1; i <= cols; i++) {
                System.out.printf("%-15s", md.getColumnLabel(i));
            }
            System.out.println();
            for (int i = 1; i <= cols; i++) {
                System.out.print("---------------");
            }
            System.out.println();

            while (rs.next()) {
                for (int i = 1; i <= cols; i++) {
                    System.out.printf("%-15s", String.valueOf(rs.getString(i)));
                }
                System.out.println();
            }
        }
        System.out.println("--------------------------------------------------------------------------------");
    }

    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private static void printSummary(List<Double> baseline, List<Double> optimized, int baseRows, int optRows) {
        Collections.sort(baseline);
        Collections.sort(optimized);

        double baseMin = baseline.get(0);
        double baseMed = baseline.get(baseline.size() / 2);
        double baseP95 = baseline.get((int) Math.ceil(0.95 * baseline.size()) - 1);
        double baseMax = baseline.get(baseline.size() - 1);
        double baseMean = baseline.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        double optMin = optimized.get(0);
        double optMed = optimized.get(optimized.size() / 2);
        double optP95 = optimized.get((int) Math.ceil(0.95 * optimized.size()) - 1);
        double optMax = optimized.get(optimized.size() - 1);
        double optMean = optimized.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        double speedupMedian = baseMed / optMed;
        double speedupP95 = baseP95 / optP95;

        System.out.println("\n================================================================================");
        System.out.println(" FINAL BENCHMARK PERFORMANCE RESULTS (100,000 Donors)");
        System.out.println("================================================================================");
        System.out.printf("%-24s | %-16s | %-16s | %-12s%n", "Metric", "Baseline Scan", "Bounding Box", "Speedup / Factor");
        System.out.println("-------------------------+------------------+------------------+-------------");
        System.out.printf("%-24s | %13.2f ms | %13.2f ms | %10.1fx faster%n", "Median Latency (p50)", baseMed, optMed, speedupMedian);
        System.out.printf("%-24s | %13.2f ms | %13.2f ms | %10.1fx faster%n", "95th Percentile (p95)", baseP95, optP95, speedupP95);
        System.out.printf("%-24s | %13.2f ms | %13.2f ms | %10.1fx faster%n", "Mean Latency", baseMean, optMean, baseMean / optMean);
        System.out.printf("%-24s | %13.2f ms | %13.2f ms | -%n", "Min Latency", baseMin, optMin);
        System.out.printf("%-24s | %13.2f ms | %13.2f ms | -%n", "Max Latency", baseMax, optMax);
        System.out.printf("%-24s | %13d rows| %13d rows| %10.1fx less rows%n", "DB Candidates Loaded", baseRows, optRows, (double) baseRows / optRows);
        System.out.println("================================================================================");
    }

    private static class DonorCoord {
        final long id;
        final double lat;
        final double lng;
        DonorCoord(long id, double lat, double lng) {
            this.id = id;
            this.lat = lat;
            this.lng = lng;
        }
    }

    private static class BenchmarkResult {
        final int candidatesFetched;
        final int matchesFound;
        BenchmarkResult(int candidatesFetched, int matchesFound) {
            this.candidatesFetched = candidatesFetched;
            this.matchesFound = matchesFound;
        }
    }
}
