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

    public Optional<User> findUserById(Long userId) {

        String sql = """
        SELECT *
        FROM users
        WHERE id = ?
        """;

        List<User> users = jdbcTemplate.query(
                sql,
                userRowMapper,
                userId
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

    public List<User> findAll(int limit, int offset) {

        String sql = """
        SELECT *
        FROM users
        ORDER BY id
        LIMIT ? OFFSET ?
        """;

        return jdbcTemplate.query(sql, userRowMapper, limit, offset);
    }

    public long count() {

        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users",
                Long.class
        );

        return count == null ? 0 : count;
    }

    public void updateProfile(Long id, String firstName, String lastName, String email) {

        jdbcTemplate.update(
                "UPDATE users SET first_name = ?, last_name = ?, email = ? WHERE id = ?",
                firstName, lastName, email, id
        );
    }

    public void updatePassword(Long id, String encodedPassword) {

        jdbcTemplate.update(
                "UPDATE users SET password = ? WHERE id = ?",
                encodedPassword, id
        );
    }

    public void updateRole(Long id, String role) {

        jdbcTemplate.update(
                "UPDATE users SET role = ? WHERE id = ?",
                role, id
        );
    }

    public void updateActive(Long id, boolean active) {

        jdbcTemplate.update(
                "UPDATE users SET active = ? WHERE id = ?",
                active, id
        );
    }

    public boolean deleteById(Long id) {

        return jdbcTemplate.update("DELETE FROM users WHERE id = ?", id) > 0;
    }
}
