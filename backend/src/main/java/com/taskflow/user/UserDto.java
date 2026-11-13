package com.taskflow.user;

/** What the API exposes about a user. The password hash never leaves the entity. */
public record UserDto(Long id, String email, String displayName) {

    public static UserDto from(User user) {
        return new UserDto(user.getId(), user.getEmail(), user.getDisplayName());
    }
}
