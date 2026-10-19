package org.example.lab1.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.example.lab1.dto.UserResponse;
import org.example.lab1.service.UserService;

@RestController
@RequestMapping("/api")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** /api/users is the resource-oriented endpoint; /api/data remains for assignment compatibility. */
    @GetMapping({"/users", "/data"})
    public List<UserResponse> getUsers() {
        return userService.findAll();
    }
}
