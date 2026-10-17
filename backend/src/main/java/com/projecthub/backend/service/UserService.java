package com.projecthub.backend.service;

import com.projecthub.backend.dto.UserRequest;
import com.projecthub.backend.dto.UserResponse;
import com.projecthub.backend.exception.DuplicateUserEmailException;
import com.projecthub.backend.exception.UserNotFoundException;
import com.projecthub.backend.mapper.UserMapper;
import com.projecthub.backend.model.User;
import com.projecthub.backend.repository.UserRepo;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class UserService
{
    private final UserRepo userRepo;
    private final UserMapper userMapper;

    public UserService(UserRepo userRepo, UserMapper userMapper) {
        this.userRepo = userRepo;
        this.userMapper = userMapper;
    }

    public List<User> getUsers(){
        return userRepo.findAll();
    }

    public User getUserById(long id){
        return userRepo.findById(id).orElseThrow(() ->
                new UserNotFoundException("User not found with id : " + id)
        );
    }

    public UserResponse addUser(UserRequest userRequest){
        User user = userMapper.toUser(userRequest);
        String email = user.getEmail().trim().toLowerCase(Locale.ROOT);
        user.setEmail(email);
        if(userRepo.existsByEmail(email)){
            throw new DuplicateUserEmailException("Email already exists");
        }
        User savedUser = userRepo.save(user);
        return userMapper.toUserResponse(savedUser);
    }

    @Transactional
    public UserResponse updateUser(UserRequest userRequest, long id){
        User existingUser = getUserById(id);
        userRequest.setEmail(userRequest.getEmail().trim().toLowerCase(Locale.ROOT));
        if(!(existingUser.getEmail().equals(userRequest.getEmail()))
                &&(userRepo.existsByEmail(userRequest.getEmail())))
        {
            throw new DuplicateUserEmailException("Email already exists");
        }
        userMapper.updateUser(userRequest,existingUser);
        return userMapper.toUserResponse(existingUser);
    }

    public void  deleteUser(long id){
        userRepo.deleteById(id);
    }

}
