package com.habitTracker.HabitTracker.auth;

import com.habitTracker.HabitTracker.auth.dto.LoginOptionsRequest;
import com.habitTracker.HabitTracker.auth.dto.LoginOptionsResponse;
import com.habitTracker.HabitTracker.auth.dto.LoginVerifyRequest;
import com.habitTracker.HabitTracker.auth.dto.LoginVerifyResponse;
import com.habitTracker.HabitTracker.auth.dto.RegistrationOptionsRequest;
import com.habitTracker.HabitTracker.auth.dto.RegistrationOptionsResponse;
import com.habitTracker.HabitTracker.auth.dto.RegistrationVerifyRequest;
import com.habitTracker.HabitTracker.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/webauthn")
@RequiredArgsConstructor
public class AuthController {

    private final WebauthnService webauthnService;

    @PostMapping("/registration/options")
    public RegistrationOptionsResponse registrationOptions(@Valid @RequestBody RegistrationOptionsRequest request) {
        return webauthnService.registrationOptions(request);
    }

    @PostMapping("/registration/verify")
    public UserResponse registrationVerify(@Valid @RequestBody RegistrationVerifyRequest request) {
        return webauthnService.registrationVerify(request);
    }

    @PostMapping("/login/options")
    public LoginOptionsResponse loginOptions(@Valid @RequestBody LoginOptionsRequest request) {
        return webauthnService.loginOptions(request);
    }

    @PostMapping("/login/verify")
    public LoginVerifyResponse loginVerify(@Valid @RequestBody LoginVerifyRequest request) {
        return webauthnService.loginVerify(request);
    }
}
