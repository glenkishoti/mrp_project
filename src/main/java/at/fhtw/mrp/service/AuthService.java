package at.fhtw.mrp.service;

import at.fhtw.mrp.model.User;
import at.fhtw.mrp.repo.IUserRepository;
import at.fhtw.mrp.util.PasswordUtil;
import at.fhtw.mrp.util.TokenService;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class AuthService implements IAuthService {

    private final IUserRepository users;

    public AuthService(IUserRepository users) {
        this.users = users;
    }

    @Override
    public UUID register(String username, String password) throws SQLException {

        // hash password
        String hash = PasswordUtil.hash(password.toCharArray());

        // create new user object
        User u = new User(UUID.randomUUID(), username, hash, null);

        // store user
        users.insert(u);

        return u.getId();
    }

    @Override
    public String login(String username, String password) throws SQLException {

        Optional<User> opt = users.findByUsername(username);
        if (opt.isEmpty())
            throw new IllegalArgumentException("Invalid credentials");

        User user = opt.get();

        // verify password
        boolean ok = PasswordUtil.verify(password.toCharArray(), user.getPasswordHash());
        if (!ok)
            throw new IllegalArgumentException("Invalid credentials");

        // create token
        String token = TokenService.generateToken(user.getId(), username);

        // store token in DB
        users.updateToken(user.getId(), token);

        return token;
    }
}
