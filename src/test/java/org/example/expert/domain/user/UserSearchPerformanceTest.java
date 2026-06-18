package org.example.expert.domain.user;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.expert.domain.user.dto.response.UserSearchResponse;
import org.example.expert.domain.user.entity.User;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
@Tag("search-performance")
class UserSearchPerformanceTest {

    private static final String NICKNAME_INDEX_NAME = "idx_users_nickname";
    private static final String COVERING_INDEX_NAME = "idx_users_nickname_covering";
    private static final int MEASUREMENT_COUNT = 10;
    private static final String SEARCH_SQL = """
            SELECT id, email, nickname
            FROM users
            WHERE nickname = ?
            """;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void comparesNicknameSearchBeforeAndAfterIndexing() {
        long userCount = requiredLong("SELECT COUNT(*) FROM users");
        assertThat(userCount)
                .as("Run bulkDataTest before measuring search performance")
                .isGreaterThanOrEqualTo(1_000_000);

        String targetNickname = jdbcTemplate.queryForObject(
                "SELECT nickname FROM users ORDER BY id LIMIT 1 OFFSET ?",
                String.class,
                userCount / 2
        );
        assertThat(targetNickname).isNotBlank();

        try {
            dropNicknameIndexIfExists();
            BenchmarkResult withoutIndex = measure(targetNickname);
            List<Map<String, Object>> withoutIndexPlan = explainAnalyze(targetNickname);

            jdbcTemplate.execute("CREATE INDEX " + NICKNAME_INDEX_NAME + " ON users (nickname)");
            BenchmarkResult withIndex = measure(targetNickname);
            List<Map<String, Object>> withIndexPlan = explainAnalyze(targetNickname);

            jdbcTemplate.execute("DROP INDEX " + NICKNAME_INDEX_NAME + " ON users");
            jdbcTemplate.execute(
                    "CREATE INDEX " + COVERING_INDEX_NAME + " ON users (nickname, email)"
            );
            BenchmarkResult withCoveringIndex = measure(targetNickname);
            List<Map<String, Object>> withCoveringIndexPlan = explainAnalyze(targetNickname);

            assertThat(withoutIndex.resultCount()).isEqualTo(1);
            assertThat(withIndex.resultCount()).isEqualTo(1);
            assertThat(withCoveringIndex.resultCount()).isEqualTo(1);

            printResult("WITHOUT INDEX", withoutIndex, withoutIndexPlan);
            printResult("WITH NICKNAME INDEX", withIndex, withIndexPlan);
            printResult("WITH COVERING INDEX", withCoveringIndex, withCoveringIndexPlan);
            System.out.printf(
                    "Nickname index improvement: %.2fx%n",
                    withoutIndex.averageMillis() / withIndex.averageMillis()
            );
            System.out.printf(
                    "Covering index improvement over nickname index: %.2fx%n",
                    withIndex.averageMillis() / withCoveringIndex.averageMillis()
            );
        } finally {
            restoreNicknameIndex();
        }

        assertThat(hasIndex(NICKNAME_INDEX_NAME)).isTrue();
        assertThat(hasIndex(COVERING_INDEX_NAME)).isFalse();

        BenchmarkResult entityResult = measureEntitySearch(targetNickname);
        BenchmarkResult projectionResult = measureProjectionSearch(targetNickname);

        printResult("JPA ENTITY", entityResult, List.of());
        printResult("DTO PROJECTION", projectionResult, List.of());
        System.out.printf(
                "DTO projection improvement over entity query: %.2fx%n",
                entityResult.averageMillis() / projectionResult.averageMillis()
        );
    }

    private BenchmarkResult measure(String nickname) {
        executeSearch(nickname);

        List<Double> elapsedMillis = new ArrayList<>();
        int resultCount = 0;

        for (int index = 0; index < MEASUREMENT_COUNT; index++) {
            long startedAt = System.nanoTime();
            List<Map<String, Object>> result = executeSearch(nickname);
            long elapsedNanos = System.nanoTime() - startedAt;

            elapsedMillis.add(elapsedNanos / 1_000_000.0);
            resultCount = result.size();
        }

        Collections.sort(elapsedMillis);
        double average = elapsedMillis.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElseThrow();

        return new BenchmarkResult(
                average,
                elapsedMillis.get(0),
                elapsedMillis.get(elapsedMillis.size() - 1),
                resultCount
        );
    }

    private List<Map<String, Object>> executeSearch(String nickname) {
        return jdbcTemplate.queryForList(SEARCH_SQL, nickname);
    }

    private BenchmarkResult measureEntitySearch(String nickname) {
        return measureJpaQuery(() -> entityManager.createQuery(
                        "select u from User u where u.nickname = :nickname",
                        User.class
                )
                .setParameter("nickname", nickname)
                .getResultList()
                .size());
    }

    private BenchmarkResult measureProjectionSearch(String nickname) {
        return measureJpaQuery(() -> entityManager.createQuery(
                        """
                        select new org.example.expert.domain.user.dto.response.UserSearchResponse(
                            u.id,
                            u.email,
                            u.nickname
                        )
                        from User u
                        where u.nickname = :nickname
                        """,
                        UserSearchResponse.class
                )
                .setParameter("nickname", nickname)
                .getResultList()
                .size());
    }

    private BenchmarkResult measureJpaQuery(ResultCountSupplier query) {
        entityManager.clear();
        query.get();

        List<Double> elapsedMillis = new ArrayList<>();
        int resultCount = 0;

        for (int index = 0; index < MEASUREMENT_COUNT; index++) {
            entityManager.clear();
            long startedAt = System.nanoTime();
            resultCount = query.get();
            long elapsedNanos = System.nanoTime() - startedAt;

            elapsedMillis.add(elapsedNanos / 1_000_000.0);
        }

        Collections.sort(elapsedMillis);
        double average = elapsedMillis.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElseThrow();

        return new BenchmarkResult(
                average,
                elapsedMillis.get(0),
                elapsedMillis.get(elapsedMillis.size() - 1),
                resultCount
        );
    }

    private List<Map<String, Object>> explainAnalyze(String nickname) {
        return jdbcTemplate.queryForList("EXPLAIN ANALYZE " + SEARCH_SQL, nickname);
    }

    private void dropNicknameIndexIfExists() {
        dropIndexIfExists(NICKNAME_INDEX_NAME);
        dropIndexIfExists(COVERING_INDEX_NAME);
    }

    private void restoreNicknameIndex() {
        dropIndexIfExists(COVERING_INDEX_NAME);
        if (!hasIndex(NICKNAME_INDEX_NAME)) {
            jdbcTemplate.execute("CREATE INDEX " + NICKNAME_INDEX_NAME + " ON users (nickname)");
        }
    }

    private void dropIndexIfExists(String indexName) {
        if (hasIndex(indexName)) {
            jdbcTemplate.execute("DROP INDEX " + indexName + " ON users");
        }
    }

    private boolean hasIndex(String indexName) {
        Long indexCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = 'users'
                  AND index_name = ?
                """,
                Long.class,
                indexName
        );

        return indexCount != null && indexCount > 0;
    }

    private long requiredLong(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        if (value == null) {
            throw new IllegalStateException("Query returned null: " + sql);
        }
        return value;
    }

    private void printResult(
            String label,
            BenchmarkResult result,
            List<Map<String, Object>> executionPlan
    ) {
        System.out.printf(
                "%s - average=%.3f ms, min=%.3f ms, max=%.3f ms, resultCount=%d%n",
                label,
                result.averageMillis(),
                result.minimumMillis(),
                result.maximumMillis(),
                result.resultCount()
        );
        executionPlan.forEach(row -> System.out.println(row.values()));
    }

    private record BenchmarkResult(
            double averageMillis,
            double minimumMillis,
            double maximumMillis,
            int resultCount
    ) {
    }

    @FunctionalInterface
    private interface ResultCountSupplier {
        int get();
    }
}
