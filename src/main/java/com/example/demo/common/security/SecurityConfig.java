
// package com.example.demo.common.security;

// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;

// import org.springframework.security.config.Customizer;
// import org.springframework.security.config.annotation.web.builders.HttpSecurity;
// import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
// import org.springframework.security.config.http.SessionCreationPolicy;

// import org.springframework.security.core.userdetails.User;
// import org.springframework.security.core.userdetails.UserDetailsService;
// import org.springframework.security.provisioning.InMemoryUserDetailsManager;

// import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
// import org.springframework.security.crypto.password.PasswordEncoder;
// import org.springframework.security.web.SecurityFilterChain;

// @Configuration
// public class SecurityConfig {

//     @Bean
//     public SecurityFilterChain securityFilterChain(
//             HttpSecurity http) throws Exception {

//         return http
//                 .authorizeHttpRequests(auth -> auth
//                         .requestMatchers("/api/revenue/**")
//                         .hasAnyRole("MANAGER", "ADMIN")
//                         .anyRequest().denyAll())
//                 .httpBasic(Customizer.withDefaults())
//                 .formLogin(AbstractHttpConfigurer::disable)
//                 .sessionManagement(session -> session
//                         .sessionCreationPolicy(
//                                 SessionCreationPolicy.STATELESS))
//                 .build();
//     }

//     @Bean
//     public PasswordEncoder passwordEncoder() {
//         return new BCryptPasswordEncoder();
//     }

//     // Tai khoan chi dung cho demo local
//     @Bean
//     public UserDetailsService userDetailsService(
//             @Value("${RR8_MANAGER_PASSWORD}") String managerPassword,
//             @Value("${RR8_ADMIN_PASSWORD}") String adminPassword,
//             @Value("${RR8_STAFF_PASSWORD}") String staffPassword,
//             PasswordEncoder encoder) {

//         return new InMemoryUserDetailsManager(
//                 User.withUsername("manager-demo")
//                         .password(encoder.encode(managerPassword))
//                         .roles("MANAGER")
//                         .build(),

//                 User.withUsername("admin-demo")
//                         .password(encoder.encode(adminPassword))
//                         .roles("ADMIN")
//                         .build(),

//                 User.withUsername("staff-demo")
//                         .password(encoder.encode(staffPassword))
//                         .roles("STAFF")
//                         .build());
//     }
// }
