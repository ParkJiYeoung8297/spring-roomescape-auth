package roomescape.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import roomescape.domain.Reservation;
import roomescape.domain.Store;
import roomescape.domain.Time;
import roomescape.domain.Theme;

@Repository
public class ReservationDao {
    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;

    private final RowMapper<Reservation> reservationRowMapper = (rs, rowNum) -> new Reservation(
            rs.getLong("id"),
            rs.getLong("member_id"),
            rs.getString("name"),
            rs.getDate("date").toLocalDate(),
            new Time(rs.getLong("time_id"), rs.getTime("time_value").toLocalTime()),
            new Theme(rs.getLong("theme_id"), rs.getString("theme_name"), rs.getString("theme_description"), rs.getString("theme_thumbnail")),
            new Store(rs.getLong("store_id"), rs.getString("store_name"), rs.getLong("store_owner_id"))
    );

    public ReservationDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.jdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("reservation")
                .usingGeneratedKeyColumns("id");
    }

    public Long save(Long memberId, String name, LocalDate date, Long timeId, Long themeId, Long storeId) {
        return jdbcInsert.executeAndReturnKey(Map.of(
                "name", name,
                "member_id", memberId,
                "date", date,
                "time_id", timeId,
                "theme_id", themeId,
                "store_id", storeId
        )).longValue();
    }

    public Reservation findById(Long id) {
        String sql = """
                SELECT r.id, 
                       r.member_id,
                       r.name, 
                       r.date,
                       r.store_id,
                       t.id AS time_id, 
                       t.start_at AS time_value,
                       th.id AS theme_id, 
                       th.name AS theme_name, 
                       th.description AS theme_description, 
                       th.thumbnail_url AS theme_thumbnail,
                       s.id AS store_id,
                       s.name AS store_name,
                       s.member_id AS store_owner_id
                FROM reservation AS r
                INNER JOIN reservation_time AS t ON r.time_id = t.id
                INNER JOIN theme AS th ON r.theme_id = th.id
                INNER JOIN store AS s ON r.store_id = s.id
                where r.id = ?
                """;
        return jdbcTemplate.queryForObject(sql, reservationRowMapper, id);
    }

    public List<Reservation> findByUserId(long id) {
        String sql = """
                SELECT r.id, 
                       r.member_id,
                       r.name, 
                       r.date,
                       t.id AS time_id, 
                       t.start_at AS time_value,
                       th.id AS theme_id, 
                       th.name AS theme_name, 
                       th.description AS theme_description, 
                       th.thumbnail_url AS theme_thumbnail,
                       s.id AS store_id,
                       s.name AS store_name,
                       s.member_id AS store_owner_id
                FROM reservation AS r
                INNER JOIN reservation_time AS t ON r.time_id = t.id
                INNER JOIN theme AS th ON r.theme_id = th.id
                INNER JOIN store AS s ON r.store_id = s.id
                WHERE r.member_id = ?
                """;
        return jdbcTemplate.query(sql, reservationRowMapper,id);
    }

    public List<Reservation> findAll() {
        String sql = """
                SELECT r.id, 
                       r.member_id,
                       r.name, 
                       r.date,
                       t.id AS time_id, 
                       t.start_at AS time_value,
                       th.id AS theme_id, 
                       th.name AS theme_name, 
                       th.description AS theme_description, 
                       th.thumbnail_url AS theme_thumbnail,
                       s.id AS store_id,
                       s.name AS store_name,
                       s.member_id AS store_owner_id
                FROM reservation AS r
                INNER JOIN reservation_time AS t ON r.time_id = t.id
                INNER JOIN theme AS th ON r.theme_id = th.id
                INNER JOIN store AS s ON r.store_id = s.id
                """;
        return jdbcTemplate.query(sql, reservationRowMapper);
    }

    public List<Reservation> findByUserName(String username) {
        String sql = """
                SELECT r.id, 
                       r.member_id,
                       r.name, 
                       r.date,
                       t.id AS time_id, 
                       t.start_at AS time_value,
                       th.id AS theme_id, 
                       th.name AS theme_name, 
                       th.description AS theme_description, 
                       th.thumbnail_url AS theme_thumbnail
                       s.id AS store_id,
                       s.name AS store_name,
                       s.member_id AS store_owner_id
                FROM reservation AS r
                INNER JOIN reservation_time AS t ON r.time_id = t.id
                INNER JOIN theme AS th ON r.theme_id = th.id
                INNER JOIN store AS s ON r.store_id = s.id
                WHERE r.name = ?
                """;
        return jdbcTemplate.query(sql, reservationRowMapper, username);
    }

    public void updateDateAndTimeById(long id, LocalDate date, long timeId) {
        jdbcTemplate.update("UPDATE reservation SET date = ?, time_id = ? WHERE id = ?", date, timeId, id);
    }


    public void delete(Long id) {
        jdbcTemplate.update("DELETE FROM reservation WHERE id = ?", id);
    }
}
