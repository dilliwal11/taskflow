package com.taskflow.common;

import org.springframework.security.oauth2.jwt.Jwt;

/** The JWT "sub" claim holds the user's database id (see TokenService). */
public final class CurrentUser {

    private CurrentUser() {}

    public static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
