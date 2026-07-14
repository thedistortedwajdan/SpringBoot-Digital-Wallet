package com.app.wallet.repository;

import com.app.wallet.mapper.UserRowMapper;
import com.app.wallet.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

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

    public int save(User user) {

        String sql = """
        INSERT INTO users
        (first_name, last_name, email, password, role)
        VALUES (?, ?, ?, ?, ?)
        """;

        return jdbcTemplate.update(
                sql,
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPassword(),
                user.getRole()
        );
    }

    public Optional<User> findByEmail(String email) {

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
}
