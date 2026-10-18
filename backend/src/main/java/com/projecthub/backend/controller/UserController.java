package com.projecthub.backend.controller;

import com.projecthub.backend.dto.CreateUserRequest;
import com.projecthub.backend.dto.UpdatePasswordRequest;
import com.projecthub.backend.dto.UpdateUserRequest;
import com.projecthub.backend.dto.UserResponse;
import com.projecthub.backend.mapper.UserMapper;
import com.projecthub.backend.model.User;
import com.projecthub.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class UserController
{
    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }


    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getUsers(){
        List<User> users = userService.getUsers();
        List<UserResponse> userList = users.stream()
                .map(userMapper::toUserResponse)
                .toList();

        return ResponseEntity.ok(userList);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id){
        return  ResponseEntity.ok(userMapper.toUserResponse(userService.getUserById(id)));
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> addUser(@Valid @RequestBody CreateUserRequest createUserRequest){
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.addUser(createUserRequest));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<UserResponse> updateUser(@Valid @RequestBody UpdateUserRequest updateUserRequest, @PathVariable Long id){
        return ResponseEntity.ok(userService.updateUser(updateUserRequest,id));
    }

    @PatchMapping("/users/{id}/password")
    public ResponseEntity<Void> updatePassword(@PathVariable Long id, @Valid @RequestBody UpdatePasswordRequest updatePasswordRequest){
        userService.updatePassword(updatePasswordRequest,id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id){
        userService.getUserById(id);
        userService.deleteUser(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
