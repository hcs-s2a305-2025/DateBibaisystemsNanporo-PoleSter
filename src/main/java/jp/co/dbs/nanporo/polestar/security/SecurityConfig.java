package jp.co.dbs.nanporo.polestar.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private LoginUserDetailsService userDetailsService;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/signup/**", "/css/**", "/js/**", "/img/**", "/error", "/favicon.ico").permitAll()
                // 店員・店長専用パスのアクセス制御（必要に応じて）
                .requestMatchers("/w/**").hasAnyAuthority("店長", "店員", "ROLE_店長", "ROLE_店員")
                .requestMatchers("/user", "/user/**").hasRole("2")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login") // GET時のログイン画面
                .loginProcessingUrl("/login") // POSTでログイン処理するURL
                .usernameParameter("mail") // ユーザ名のパラメータ名
                .passwordParameter("password") // パスワードのパラメータ名
                .defaultSuccessUrl("/login-success", true) // ★ログイン成功後に判定エンドポイントへ遷移
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true") // ログアウト後にリダイレクトしたいURL
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            );
        return http.build();
    }

    @Bean
    AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder = 
            http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder
            .userDetailsService(userDetailsService)
            .passwordEncoder(passwordEncoder());
        return authenticationManagerBuilder.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
