package app.keystone.common.utils.sql;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SqlLikeUtilsTest {

    @Test
    void escape_shouldEscapeSqlLikeWildcards() {
        String escaped = SqlLikeUtils.escape("50%_done\\tail");

        assertThat(escaped).isEqualTo("50\\%\\_done\\\\tail");
    }

    @Test
    void escape_shouldKeepNullAndEmptyValues() {
        assertThat(SqlLikeUtils.escape(null)).isNull();
        assertThat(SqlLikeUtils.escape("")).isEmpty();
    }
}
