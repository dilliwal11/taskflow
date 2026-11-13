package com.taskflow.user;

import com.taskflow.common.CurrentUser;
import com.taskflow.common.Exceptions.NotFoundException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final UserRepository users;

    public UserController(UserRepository users) {
        this.users = users;
    }

    /** Lets the frontend restore the session after a page reload. */
    @GetMapping("/api/me")
    public UserDto me(@AuthenticationPrincipal Jwt jwt) {
        return users.findById(CurrentUser.id(jwt))
                .map(UserDto::from)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
