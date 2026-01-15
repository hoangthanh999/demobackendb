// backend/src/main/java/com/badminton/security/CustomUserDetailsService.java

package com.badminton.security;

import com.badminton.entity.User;
import com.badminton.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // ← THÊM
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j // ← THÊM
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

                Collection<? extends GrantedAuthority> authorities = getAuthorities(user);
                log.info("🔐 Authorities created: {}", authorities);

                return new org.springframework.security.core.userdetails.User(
                                user.getEmail(),
                                user.getPassword(),
                                user.getActive(),
                                true,
                                true,
                                true,
                                authorities);
        }

        private Collection<? extends GrantedAuthority> getAuthorities(User user) {
                String authority = "ROLE_" + user.getRole().name();
                log.info("🎫 Creating authority: {}", authority);
                return Collections.singletonList(new SimpleGrantedAuthority(authority));
        }
}
