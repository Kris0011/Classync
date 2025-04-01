package com.classync.project.security;

import com.classync.project.entity.User;
import com.classync.project.services.impl.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

@EnableWebSecurity
@Configuration
public class SecurityConfiguration {

    @Autowired
    private UserService userService;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/api/user/*").permitAll(); // Allow unauthenticated access to /api/user
                    auth.requestMatchers("/api/classrooms/create").authenticated();
                    auth.requestMatchers("/api/classrooms/*").authenticated();
                    auth.requestMatchers("/api/users/*").authenticated();

                    auth.requestMatchers("/api/announcements/*").authenticated(); // Require authentication for this
                    auth.requestMatchers("/api/userclassroom/*").authenticated();
                    auth.requestMatchers("/api/comments/*").authenticated(); // Allow authenticated users to comment
                    auth.requestMatchers("/api/doubts/*").authenticated(); // Allow authenticated users to comment
                    auth.requestMatchers("/api/mail/*").authenticated();
                    auth.anyRequest().authenticated(); // Require authentication for all other endpoints // endpoint
                    // auth.anyRequest().authenticated(); // Require authentication for all other
                    // endpoints

                })
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Enable CORS
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for simplicity (enable it in production)
                .oauth2Login(oauth2Login -> oauth2Login
                        .userInfoEndpoint(userInfoEndpoint -> userInfoEndpoint.oidcUserService(oidcUserService()))
                        .successHandler(new AuthenticationSuccessHandler() {
                            @Override
                            public void onAuthenticationSuccess(HttpServletRequest request,
                                    HttpServletResponse response, Authentication authentication)
                                    throws IOException, ServletException {
                                String email = ((org.springframework.security.oauth2.core.oidc.user.OidcUser) authentication
                                        .getPrincipal()).getEmail();
                                String fullname = ((org.springframework.security.oauth2.core.oidc.user.OidcUser) authentication
                                        .getPrincipal()).getFullName();
                                String picture = ((org.springframework.security.oauth2.core.oidc.user.OidcUser) authentication
                                        .getPrincipal()).getPicture();
                                Optional<User> userOptional = userService.findUserByEmail(email);
                                System.out.println("User found: " + userOptional);
                                if (userOptional.isEmpty()) {
                                    User user = new User();
                                    user.setEmail(email);
                                    user.setFullName(fullname);
                                    user.setPicture(picture);
                                    User u = userService.saveUser(user);
                                    System.out.println("New User saved: " + u);
                                }
                                response.sendRedirect("http://localhost:5173");
                            }
                        }).failureHandler(authenticationFailureHandler()))
                // .logout(logout ->
                // logout.logoutUrl("/logout").logoutSuccessUrl("/").permitAll())
                .logout(logout -> logout
                        .logoutUrl("/api/logout")
                        .logoutSuccessHandler(logoutSuccessHandler())
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"))

                .build();
    }

    @Bean
    public OidcUserService oidcUserService() {
        return new OidcUserService();
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return new SimpleUrlAuthenticationFailureHandler();
    }

    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        return new SimpleUrlLogoutSuccessHandler();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(Arrays.asList("http://localhost:5173")); // Allow requests from your frontend
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS")); // Allow specific HTTP
                                                                                            // methods
        config.setAllowedHeaders(Arrays.asList("*")); // Allow all headers

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // Apply CORS to all endpoints
        return source;
    }
}