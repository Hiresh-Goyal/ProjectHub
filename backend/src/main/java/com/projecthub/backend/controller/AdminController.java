package com.projecthub.backend.controller;

import com.projecthub.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController
{
    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
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
}
