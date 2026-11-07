package com.shopkart.service;

import com.shopkart.exception.ApiException;
import com.shopkart.model.User;
import com.shopkart.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User require() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw ApiException.unauthorized("Not authenticated");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> ApiException.unauthorized("Not authenticated"));
    }
}
