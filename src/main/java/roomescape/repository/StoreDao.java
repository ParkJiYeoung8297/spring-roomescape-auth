package roomescape.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import roomescape.domain.Store;

@Repository
public class StoreDao {
    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;

    private final RowMapper<Store> storeRowMapper = (rs, rowNum) -> new Store(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getLong("ownerId"));

    public StoreDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.jdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("store")
                .usingGeneratedKeyColumns("id");
    }

    public List<Store> findByOwnerId(Long ownerId) {
        String sql = "SELECT * FROM store WHERE owner_id = ?";
        return jdbcTemplate.query(sql, storeRowMapper, ownerId);
    }
}
