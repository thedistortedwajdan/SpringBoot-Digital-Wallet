package com.app.wallet.repository;

import com.app.wallet.mapper.UserRowMapper;
import com.app.wallet.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    private final UserRowMapper userRowMapper;

    public UserRepository(JdbcTemplate jdbcTemplate,UserRowMapper userRowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRowMapper= userRowMapper;
    }

    public long createUser(User user) {

        String sql = """
        INSERT INTO users
        (first_name, last_name, email, password, role)
        VALUES (?, ?, ?, ?, ?)
        """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {

            PreparedStatement ps = connection.prepareStatement(
                    sql,
                    new String[]{"id"}
            );

            ps.setString(1, user.getFirstName());
            ps.setString(2, user.getLastName());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPassword());
            ps.setString(5, user.getRole());

            return ps;

        }, keyHolder);

        return keyHolder.getKey().longValue();

    }

    public Optional<User> findUserByEmail(String email) {

        String sql = """
        SELECT *
        FROM users
        WHERE email = ?
        """;

        List<User> users = jdbcTemplate.query(
                sql,
                userRowMapper,
                email
        );

        return users.stream().findFirst();

    }

    public boolean existsByEmail(String email) {

        String sql = """
        SELECT COUNT(*)
        FROM users
        WHERE email = ?
        """;

        Integer count = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                email
        );

        return (count != null && count > 0);
    }
}
