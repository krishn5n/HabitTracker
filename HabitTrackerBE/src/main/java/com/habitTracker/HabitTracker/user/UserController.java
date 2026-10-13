package com.habitTracker.HabitTracker.user;

import com.habitTracker.HabitTracker.user.dto.UserResponse;
import com.habitTracker.HabitTracker.user.dto.UserUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{userSeqId}")
    public UserResponse getUser(@PathVariable Long userSeqId) {
        return userService.getUser(userSeqId);
    }

    @PutMapping("/{userSeqId}")
    public UserResponse updateUser(@PathVariable Long userSeqId, @Valid @RequestBody UserUpdateRequest request) {
        return userService.updateUser(userSeqId, request);
    }
}
