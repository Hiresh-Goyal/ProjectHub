package com.projecthub.backend.controller;

import com.projecthub.backend.dto.UserResponse;
import com.projecthub.backend.mapper.UserMapper;
import com.projecthub.backend.model.User;
import com.projecthub.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController
{
    private final UserService userService;
    private final UserMapper userMapper;

    public AdminController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @GetMapping()
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> test()
    {
        return ResponseEntity.ok("Admin access granted!!");
    }

    @PostMapping("/makeAdmin/{userId}")
    public ResponseEntity<Void> makeAdmin(@PathVariable Long userId)
    {
        userService.makeAdmin(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/getGlobalAdmins")
    public ResponseEntity<List<UserResponse>> getGlobalAdmins()
    {
        List<User> admins = userService.getGlobalAdmins();
        List<UserResponse> userResponseList = admins.stream()
                .map(userMapper::toUserResponse)
                .toList();
        return ResponseEntity.ok(userResponseList);
    }
}
