package com.projecthub.backend.mapper;

import com.projecthub.backend.dto.UserRequest;
import com.projecthub.backend.dto.UserResponse;
import com.projecthub.backend.model.User;
import org.springframework.stereotype.Component;


@Component
public class UserMapper
{
    public User toUser(UserRequest userRequest)
    {
        User user = new User();
        user.setName(userRequest.getName());
        user.setEmail(userRequest.getEmail());
        user.setPassword(userRequest.getPassword());
        return user;
    }

    public UserResponse toUserResponse(User user)
    {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public void updateUser(UserRequest userRequest, User existingUser)
    {
        existingUser.setName(userRequest.getName());
        existingUser.setEmail(userRequest.getEmail());
        existingUser.setPassword(userRequest.getPassword());
    }
}
