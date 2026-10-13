package com.habitTracker.HabitTracker.user;

import com.habitTracker.HabitTracker.user.dto.UserResponse;
import com.habitTracker.HabitTracker.user.dto.UserUpdateRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserResponse getUser(Long userSeqId) {
        User user = findOrThrow(userSeqId);
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long userSeqId, UserUpdateRequest request) {
        User user = findOrThrow(userSeqId);

        if (request.displayName() != null) {
            user.setDisplayName(request.displayName());
        }
        if (request.gender() != null) {
            user.setGender(request.gender());
        }
        if (request.dateOfBirth() != null) {
            user.setDateOfBirth(request.dateOfBirth());
        }
        if (request.weightKg() != null) {
            user.setWeightKg(request.weightKg());
        }
        if (request.heightCm() != null) {
            user.setHeightCm(request.heightCm());
        }
        user.setUpdatedAt(LocalDateTime.now());

        return toResponse(user);
    }

    private User findOrThrow(Long userSeqId) {
        return userRepository.findById(userSeqId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userSeqId));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getUserSeqId(),
                user.getDisplayName(),
                user.getEmail(),
                user.getGender(),
                user.getDateOfBirth(),
                UserResponse.ageFrom(user.getDateOfBirth()),
                user.getWeightKg(),
                user.getHeightCm()
        );
    }
}
