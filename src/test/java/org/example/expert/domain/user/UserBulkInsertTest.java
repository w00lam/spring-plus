package org.example.expert.domain.user;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.SplittableRandom;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
@Tag("bulk-data")
class UserBulkInsertTest {

    private static final int TOTAL_COUNT = 1_000_000;
    private static final int BATCH_SIZE = 10_000;
    private static final String INSERT_SQL = """
            INSERT INTO users (
                created_at,
                modified_at,
                email,
                password,
                user_role,
                nickname
            )
            VALUES (?, ?, ?, ?, ?, ?)
            """;
    private static final String TEST_PASSWORD = "bulk-test-password";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void insertsOneMillionUsersWithJdbcBatch() {
        String runId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        SplittableRandom random = new SplittableRandom();
        int multiplier = findCoprimeMultiplier(random);
        int offset = random.nextInt(TOTAL_COUNT);
        LocalDateTime now = LocalDateTime.now();
        Instant startedAt = Instant.now();

        for (int startIndex = 0; startIndex < TOTAL_COUNT; startIndex += BATCH_SIZE) {
            int batchStartIndex = startIndex;
            int currentBatchSize = Math.min(BATCH_SIZE, TOTAL_COUNT - startIndex);

            jdbcTemplate.batchUpdate(INSERT_SQL, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement preparedStatement, int batchIndex) throws SQLException {
                    int userIndex = batchStartIndex + batchIndex;
                    int nicknameNumber = permute(userIndex, multiplier, offset);

                    preparedStatement.setTimestamp(1, Timestamp.valueOf(now));
                    preparedStatement.setTimestamp(2, Timestamp.valueOf(now));
                    preparedStatement.setString(3, "bulk_" + runId + "_" + userIndex + "@example.com");
                    preparedStatement.setString(4, TEST_PASSWORD);
                    preparedStatement.setString(5, "USER");
                    preparedStatement.setString(
                            6,
                            "user_" + runId + "_" + String.format("%06d", nicknameNumber)
                    );
                }

                @Override
                public int getBatchSize() {
                    return currentBatchSize;
                }
            });

            System.out.printf("Inserted %,d / %,d users%n", startIndex + currentBatchSize, TOTAL_COUNT);
        }

        Duration elapsed = Duration.between(startedAt, Instant.now());
        Long insertedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE nickname LIKE ?",
                Long.class,
                "user_" + runId + "_%"
        );
        Long distinctNicknameCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT nickname) FROM users WHERE nickname LIKE ?",
                Long.class,
                "user_" + runId + "_%"
        );

        assertThat(insertedCount).isEqualTo(TOTAL_COUNT);
        assertThat(distinctNicknameCount).isEqualTo(TOTAL_COUNT);

        System.out.printf(
                "Bulk insert completed: runId=%s, count=%,d, elapsed=%d ms%n",
                runId,
                insertedCount,
                elapsed.toMillis()
        );
    }

    private int findCoprimeMultiplier(SplittableRandom random) {
        int candidate;
        do {
            candidate = random.nextInt(1, TOTAL_COUNT);
        } while (greatestCommonDivisor(candidate, TOTAL_COUNT) != 1);
        return candidate;
    }

    private int permute(int value, int multiplier, int offset) {
        return (int) (((long) multiplier * value + offset) % TOTAL_COUNT);
    }

    private int greatestCommonDivisor(int left, int right) {
        while (right != 0) {
            int remainder = left % right;
            left = right;
            right = remainder;
        }
        return left;
    }
}
