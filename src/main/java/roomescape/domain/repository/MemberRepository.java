package roomescape.domain.repository;

import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import roomescape.domain.Member;
import roomescape.domain.Role;

@Repository
public class MemberRepository {
    private JdbcTemplate jdbcTemplate;

    public MemberRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Member> findById(long id) {
        String sql = "Select id, name, role from member where id = ?";

        return jdbcTemplate.query(sql,
                ((rs, rowNum) -> new Member(
                        rs.getLong("id"),
                        rs.getString("name"),
                        Role.of(rs.getString("role"))
                        ))
                ,id).stream().findFirst();
    }

}
