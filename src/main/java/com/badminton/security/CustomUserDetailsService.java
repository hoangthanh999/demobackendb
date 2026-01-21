// security/CustomUserDetailsService.java
package com.badminton.security;

import com.badminton.entity.User;
import com.badminton.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

        private final UserRepository userRepository;

        @Override
        @Transactional
        public UserDetails loadUserByUsername(String emailOrPhone) throws UsernameNotFoundException {
                log.info("🔍 Loading user by username: {}", emailOrPhone);

                User user = userRepository.findByEmail(emailOrPhone)
                                .orElseGet(() -> userRepository.findByPhone(emailOrPhone)
                                                .orElseThrow(() -> new UsernameNotFoundException(
                                                                "Không tìm thấy người dùng với email hoặc số điện thoại: "
                                                                                + emailOrPhone)));

                log.info("👤 User found: email={}, role={}, active={}",
                                user.getEmail(), user.getRole(), user.getActive());

                // ✅ THAY ĐỔI: Return UserPrincipal thay vì Spring User
                UserPrincipal userPrincipal = UserPrincipal.create(user);
                log.info("🔐 Authorities created: {}", userPrincipal.getAuthorities());

                return userPrincipal;
        }
}
