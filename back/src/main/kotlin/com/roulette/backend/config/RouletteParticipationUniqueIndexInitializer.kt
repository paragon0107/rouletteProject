package com.roulette.backend.config

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
class RouletteParticipationUniqueIndexInitializer(
    private val jdbcTemplate: JdbcTemplate,
    private val dataSource: DataSource,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        dropLegacyUniqueConstraint()
        createActiveOnlyUniqueIndex()
    }

    private fun dropLegacyUniqueConstraint() {
        jdbcTemplate.execute(SQL_DROP_LEGACY_UNIQUE_CONSTRAINT)
    }

    private fun createActiveOnlyUniqueIndex() {
        val databaseProductName = dataSource.connection.use { connection ->
            connection.metaData.databaseProductName.lowercase()
        }
        if (databaseProductName.contains("postgresql")) {
            jdbcTemplate.execute(SQL_CREATE_PARTIAL_UNIQUE_INDEX)
            return
        }
        log.info("부분 유니크 인덱스를 지원하지 않는 데이터베이스이므로 생성 작업을 건너뜁니다. db={}", databaseProductName)
    }

    companion object {
        private val log = LoggerFactory.getLogger(RouletteParticipationUniqueIndexInitializer::class.java)

        private const val SQL_DROP_LEGACY_UNIQUE_CONSTRAINT = """
            ALTER TABLE roulette_participations
            DROP CONSTRAINT IF EXISTS uk_roulette_participations_user_date
        """

        private const val SQL_CREATE_PARTIAL_UNIQUE_INDEX = """
            CREATE UNIQUE INDEX IF NOT EXISTS uk_roulette_participations_user_date_active
            ON roulette_participations (user_id, participation_date)
            WHERE is_canceled = FALSE
        """
    }
}
