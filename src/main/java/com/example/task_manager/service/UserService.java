package com.example.task_manager.service;

import com.example.task_manager.entity.User;
import com.example.task_manager.form.RegistrationForm;
import com.example.task_manager.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegistrationForm form) {
        if (userRepository.existsByUsername(form.getUsername())) {
            throw new IllegalArgumentException("ユーザー名は既に使用されています");
        }
        if (userRepository.existsByEmail(form.getEmail())) {
            throw new IllegalArgumentException("メールアドレスは既に使用されています");
        }
        User user = new User(form.getUsername(), form.getEmail(),
                passwordEncoder.encode(form.getPassword()), form.getDisplayName());
        return userRepository.save(user);
    }
}
