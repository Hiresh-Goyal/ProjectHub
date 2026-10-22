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
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        if(user.getCompany() == null)
        {
            response.setCompanyId(null);
            response.setCompanyName(null);
            response.setCompanyRole(null);
        }
        else
        {
            response.setCompanyId(user.getCompany().getId());
            response.setCompanyName(user.getCompany().getName());
            response.setCompanyRole(user.getCompanyRole());
        }
        return response;
    }

    public void updateUser(UpdateUserRequest updateUserRequest, User existingUser)
    {
        existingUser.setName(updateUserRequest.getName());
        existingUser.setEmail(updateUserRequest.getEmail());
    }
}
