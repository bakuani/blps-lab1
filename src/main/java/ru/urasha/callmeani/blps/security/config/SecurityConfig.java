package ru.urasha.callmeani.blps.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import ru.urasha.callmeani.blps.security.authorization.Permissions;
import ru.urasha.callmeani.blps.security.handler.RestAccessDeniedHandler;
import ru.urasha.callmeani.blps.security.handler.RestAuthenticationEntryPoint;
import ru.urasha.callmeani.blps.security.jwt.JwtAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        JwtAuthenticationFilter jwtAuthenticationFilter,
        RestAuthenticationEntryPoint restAuthenticationEntryPoint,
        RestAccessDeniedHandler restAccessDeniedHandler
    ) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(config -> config
                .authenticationEntryPoint(restAuthenticationEntryPoint)
                .accessDeniedHandler(restAccessDeniedHandler)
            )
            .authorizeHttpRequests(registry -> {
                configurePublicEndpoints(registry);
                configureSubscriberSelfServiceEndpoints(registry);
                configureTariffCatalogEndpoints(registry);
                configureTariffCategoryEndpoints(registry);
                configureTariffOptionEndpoints(registry);
                configureFeatureCatalogEndpoints(registry);
                configureFeatureCategoryEndpoints(registry);
                configureSubscriberEndpoints(registry);
                configureSubscriberFeatureEndpoints(registry);
                registry.anyRequest().authenticated();
            })
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    private void configurePublicEndpoints(
        AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry
    ) {
        registry.requestMatchers(
            "/api/v1/auth/login",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/api/v1/dev/**"
        ).permitAll();
    }

    private void configureSubscriberSelfServiceEndpoints(
        AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry
    ) {
        registry.requestMatchers(HttpMethod.GET, "/api/v1/tariffs").hasAuthority(Permissions.TARIFF_READ);
        registry.requestMatchers(HttpMethod.GET, "/api/v1/tariffs/categories").hasAuthority(Permissions.TARIFF_READ);
        registry.requestMatchers(HttpMethod.GET, "/api/v1/tariffs/*").hasAuthority(Permissions.TARIFF_READ);
        registry.requestMatchers(HttpMethod.GET, "/api/v1/subscribers/*/tariff").hasAuthority(Permissions.TARIFF_READ);
        registry.requestMatchers(HttpMethod.POST, "/api/v1/subscribers/*/tariff/change").hasAnyAuthority(
            Permissions.TARIFF_CHANGE_OWN,
            Permissions.TARIFF_CHANGE_ANY
        );

        registry.requestMatchers(HttpMethod.GET, "/api/v1/features/categories").hasAuthority(Permissions.FEATURE_READ);
        registry.requestMatchers(HttpMethod.GET, "/api/v1/features/*").hasAuthority(Permissions.FEATURE_READ);
        registry.requestMatchers(HttpMethod.GET, "/api/v1/subscribers/*/features").hasAuthority(Permissions.FEATURE_READ);
        registry.requestMatchers(HttpMethod.POST, "/api/v1/subscribers/*/features/*/disable").hasAnyAuthority(
            Permissions.FEATURE_DISABLE_OWN,
            Permissions.FEATURE_DISABLE_ANY
        );
    }

    private void configureTariffCatalogEndpoints(
        AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry
    ) {
        registry.requestMatchers(HttpMethod.GET, "/api/v1/tariff-catalog", "/api/v1/tariff-catalog/*")
            .hasAuthority(Permissions.TARIFF_READ);
        registry.requestMatchers(HttpMethod.POST, "/api/v1/tariff-catalog")
            .hasAuthority(Permissions.TARIFF_WRITE);
        registry.requestMatchers(HttpMethod.PUT, "/api/v1/tariff-catalog/*")
            .hasAuthority(Permissions.TARIFF_WRITE);
        registry.requestMatchers(HttpMethod.DELETE, "/api/v1/tariff-catalog/*")
            .hasAuthority(Permissions.TARIFF_DELETE);
    }

    private void configureTariffCategoryEndpoints(
        AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry
    ) {
        registry.requestMatchers(HttpMethod.GET, "/api/v1/tariff-categories", "/api/v1/tariff-categories/*")
            .hasAuthority(Permissions.TARIFF_CATEGORY_READ);
        registry.requestMatchers(HttpMethod.POST, "/api/v1/tariff-categories")
            .hasAuthority(Permissions.TARIFF_CATEGORY_WRITE);
        registry.requestMatchers(HttpMethod.PUT, "/api/v1/tariff-categories/*")
            .hasAuthority(Permissions.TARIFF_CATEGORY_WRITE);
        registry.requestMatchers(HttpMethod.DELETE, "/api/v1/tariff-categories/*")
            .hasAuthority(Permissions.TARIFF_CATEGORY_DELETE);
    }

    private void configureTariffOptionEndpoints(
        AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry
    ) {
        registry.requestMatchers(HttpMethod.GET, "/api/v1/tariff-options", "/api/v1/tariff-options/*")
            .hasAuthority(Permissions.TARIFF_OPTION_READ);
        registry.requestMatchers(HttpMethod.POST, "/api/v1/tariff-options")
            .hasAuthority(Permissions.TARIFF_OPTION_WRITE);
        registry.requestMatchers(HttpMethod.PUT, "/api/v1/tariff-options/*")
            .hasAuthority(Permissions.TARIFF_OPTION_WRITE);
        registry.requestMatchers(HttpMethod.DELETE, "/api/v1/tariff-options/*")
            .hasAuthority(Permissions.TARIFF_OPTION_DELETE);
    }

    private void configureFeatureCatalogEndpoints(
        AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry
    ) {
        registry.requestMatchers(HttpMethod.GET, "/api/v1/additional-features", "/api/v1/additional-features/*")
            .hasAuthority(Permissions.FEATURE_READ);
        registry.requestMatchers(HttpMethod.POST, "/api/v1/additional-features")
            .hasAuthority(Permissions.FEATURE_WRITE);
        registry.requestMatchers(HttpMethod.PUT, "/api/v1/additional-features/*")
            .hasAuthority(Permissions.FEATURE_WRITE);
        registry.requestMatchers(HttpMethod.DELETE, "/api/v1/additional-features/*")
            .hasAuthority(Permissions.FEATURE_DELETE);
    }

    private void configureFeatureCategoryEndpoints(
        AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry
    ) {
        registry.requestMatchers(HttpMethod.GET, "/api/v1/feature-categories", "/api/v1/feature-categories/*")
            .hasAuthority(Permissions.FEATURE_CATEGORY_READ);
        registry.requestMatchers(HttpMethod.POST, "/api/v1/feature-categories")
            .hasAuthority(Permissions.FEATURE_CATEGORY_WRITE);
        registry.requestMatchers(HttpMethod.PUT, "/api/v1/feature-categories/*")
            .hasAuthority(Permissions.FEATURE_CATEGORY_WRITE);
        registry.requestMatchers(HttpMethod.DELETE, "/api/v1/feature-categories/*")
            .hasAuthority(Permissions.FEATURE_CATEGORY_DELETE);
    }

    private void configureSubscriberEndpoints(
        AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry
    ) {
        registry.requestMatchers(HttpMethod.GET, "/api/v1/subscribers", "/api/v1/subscribers/*")
            .hasAuthority(Permissions.SUBSCRIBER_READ);
        registry.requestMatchers(HttpMethod.POST, "/api/v1/subscribers")
            .hasAuthority(Permissions.SUBSCRIBER_WRITE);
        registry.requestMatchers(HttpMethod.PUT, "/api/v1/subscribers/*")
            .hasAuthority(Permissions.SUBSCRIBER_WRITE);
        registry.requestMatchers(HttpMethod.DELETE, "/api/v1/subscribers/*")
            .hasAuthority(Permissions.SUBSCRIBER_DELETE);
    }

    private void configureSubscriberFeatureEndpoints(
        AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry
    ) {
        registry.requestMatchers(HttpMethod.GET, "/api/v1/subscriber-features", "/api/v1/subscriber-features/*")
            .hasAuthority(Permissions.SUBSCRIBER_FEATURE_READ);
        registry.requestMatchers(HttpMethod.POST, "/api/v1/subscriber-features")
            .hasAuthority(Permissions.SUBSCRIBER_FEATURE_WRITE);
        registry.requestMatchers(HttpMethod.PUT, "/api/v1/subscriber-features/*")
            .hasAuthority(Permissions.SUBSCRIBER_FEATURE_WRITE);
        registry.requestMatchers(HttpMethod.DELETE, "/api/v1/subscriber-features/*")
            .hasAuthority(Permissions.SUBSCRIBER_FEATURE_DELETE);
    }

    @Bean
    public AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

