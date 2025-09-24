package com.milesight.beaveriot.authentication.config;

import com.milesight.beaveriot.authentication.converter.CustomAuthenticationManagerResolver;
import com.milesight.beaveriot.authentication.exception.CustomAuthenticationHandler;
import com.milesight.beaveriot.authentication.exception.CustomOAuth2AccessDeniedHandler;
import com.milesight.beaveriot.authentication.exception.CustomOAuth2ExceptionEntryPoint;
import com.milesight.beaveriot.authentication.filter.SecurityUserContextCleanupFilter;
import com.milesight.beaveriot.authentication.handler.CustomOAuth2AccessTokenResponseHandler;
import com.milesight.beaveriot.authentication.provider.*;
import com.milesight.beaveriot.authentication.util.OAuth2EndpointUtils;
import com.milesight.beaveriot.user.facade.IUserFacade;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.*;
import org.springframework.security.oauth2.server.authorization.web.authentication.DelegatingAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;

/**
 * @author loong
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfiguration {

    @Autowired
    UserDetailsService userDetailsService;
    @Autowired
    OAuth2TokenCustomizer tokenCustomizer;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    OAuth2Properties oAuth2Properties;
    @Autowired
    IUserFacade userFacade;

    @Bean
    @Order(3)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .tokenEndpoint(tokenEndpoint ->
                        tokenEndpoint.accessTokenRequestConverter(new DelegatingAuthenticationConverter(Arrays.asList(
                                        new CustomOAuth2RefreshTokenAuthenticationConverter(),
                                        new CustomOAuth2PasswordAuthenticationConverter()))
                                )
                                .authenticationProvider(new CustomOAuth2PasswordAuthenticationProvider(authorizationService(), tokenGenerator(), userFacade, authenticationProvider()))
                                .authenticationProvider(new CustomOAuth2RefreshTokenAuthenticationProvider(authorizationService(), tokenGenerator(), userFacade, jwtDecoder()))
                                .errorResponseHandler(new CustomAuthenticationHandler())
                                .accessTokenResponseHandler(new CustomOAuth2AccessTokenResponseHandler())
                )
                .clientAuthentication(clientAuthentication -> clientAuthentication.errorResponseHandler(new CustomAuthenticationHandler()))
                .oidc(Customizer.withDefaults());
        http.exceptionHandling(
                exception -> exception.authenticationEntryPoint(new CustomOAuth2ExceptionEntryPoint())
                        .accessDeniedHandler(new CustomOAuth2AccessDeniedHandler())
        );
        http.addFilterAfter(new SecurityUserContextCleanupFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @Order(4)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher((request) -> !OAuth2EndpointUtils.getWhiteListMatcher(oAuth2Properties.getIgnoreUrls()).matches(request))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(oAuth2Properties.getIgnoreUrls()).permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(
                        AbstractHttpConfigurer::disable
                )
                .logout(
                        AbstractHttpConfigurer::disable
                )
                .csrf(
                        AbstractHttpConfigurer::disable
                )
                .oauth2ResourceServer(oauth2ResourceServer ->
                        oauth2ResourceServer
                                .authenticationManagerResolver(new CustomAuthenticationManagerResolver(authorizationService(), jwtDecoder()))
                                .authenticationEntryPoint(new CustomOAuth2ExceptionEntryPoint())
                                .accessDeniedHandler(new CustomOAuth2AccessDeniedHandler())
                );
        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(passwordEncoder());
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        TokenSettings tokenSettings = TokenSettings.builder()
                .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
                .reuseRefreshTokens(false)
                .accessTokenTimeToLive(Duration.ofDays(1))
                .refreshTokenTimeToLive(Duration.ofDays(1))
                .build();
        ClientSettings clientSettings = ClientSettings.builder()
                .requireAuthorizationConsent(false)
                .build();
        RegisteredClient registeredClient = RegisteredClient.withId(oAuth2Properties.getRegisteredClientId())
                .clientId(oAuth2Properties.getClientId())
                .clientSecret("{noop}" + oAuth2Properties.getClientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.PASSWORD)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .tokenSettings(tokenSettings)
                .clientSettings(clientSettings)
                .build();
        return new InMemoryRegisteredClientRepository(registeredClient);
//        return new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    @Bean
    public CustomOAuth2AuthorizationService authorizationService() {
//        return new InMemoryOAuth2AuthorizationService();
        return new CustomJdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository());
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        NimbusJwtEncoder jwtEncoder = new NimbusJwtEncoder(jwkSource());
        return jwtEncoder;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource());
    }

    @Bean
    public OAuth2TokenGenerator<?> tokenGenerator() {
        JwtGenerator jwtGenerator = new JwtGenerator(jwtEncoder());
        jwtGenerator.setJwtCustomizer(tokenCustomizer);
        OAuth2AccessTokenGenerator accessTokenGenerator = new OAuth2AccessTokenGenerator();
        OAuth2RefreshTokenGenerator refreshTokenGenerator = new OAuth2RefreshTokenGenerator();
        return new DelegatingOAuth2TokenGenerator(
                jwtGenerator, accessTokenGenerator, refreshTokenGenerator);
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
//        KeyPair keyPair = generateRsaKey();
//        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
//        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAPublicKey publicKey = loadPublicKeyFromPem(oAuth2Properties.getRsa().getPublicKey());
        RSAPrivateKey privateKey = loadPrivateKeyFromPem(oAuth2Properties.getRsa().getPrivateKey());
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    private static KeyPair generateRsaKey() {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return keyPair;
    }

    private static RSAPublicKey loadPublicKeyFromPem(String pem) {
        String publicKeyPEM = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(encoded);
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) kf.generatePublic(spec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load public key", e);
        }
    }

    private static RSAPrivateKey loadPrivateKeyFromPem(String pem) {
        String privateKeyPEM = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(encoded);
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) kf.generatePrivate(spec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load private key", e);
        }
    }

    //    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
