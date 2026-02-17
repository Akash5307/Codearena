package com.codearena.user.service;

import com.codearena.common.exception.ResourceNotFoundException;
import com.codearena.user.dto.UpdateProfileRequest;
import com.codearena.user.dto.UserProfileResponse;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Cacheable(value = "userProfile", key = "#username")
    public UserProfileResponse getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return UserProfileResponse.from(user);
    }

    @Transactional
    @CacheEvict(value = "userProfile", allEntries = true)
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }

        user = userRepository.save(user);
        return UserProfileResponse.from(user);
    }

    public Page<UserProfileResponse> getRatingLeaderboard(Pageable pageable) {
        return userRepository.findAllByOrderByRatingDesc(pageable)
                .map(UserProfileResponse::from);
    }
}
