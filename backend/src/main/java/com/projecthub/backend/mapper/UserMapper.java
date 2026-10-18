package com.projecthub.backend.mapper;

import com.projecthub.backend.dto.CreateUserRequest;
import com.projecthub.backend.dto.UpdateUserRequest;
import com.projecthub.backend.dto.UserResponse;
import com.projecthub.backend.model.User;
import org.springframework.stereotype.Component;


@Component
public class UserMapper
{
    public User toUser(CreateUserRequest createUserRequest)
    {
        User user = new User();
        user.setName(createUserRequest.getName());
        user.setEmail(createUserRequest.getEmail());
        user.setPassword(createUserRequest.getPassword());
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

    public void updateUser(UpdateUserRequest updateUserRequest, User existingUser)
    {
        existingUser.setName(updateUserRequest.getName());
        existingUser.setEmail(updateUserRequest.getEmail());
    }
}
