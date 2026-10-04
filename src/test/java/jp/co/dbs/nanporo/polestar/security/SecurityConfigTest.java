package jp.co.dbs.nanporo.polestar.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = SecurityConfigTest.TestController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private LoginUserDetailsService userDetailsService;

    @Test
    @DisplayName("PasswordEncoder Bean は BCrypt を返す")
    void testPasswordEncoder() {
        PasswordEncoder encoder = new SecurityConfig().passwordEncoder();

        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
        assertThat(encoder.encode("password")).isNotEqualTo("password");
    }

    @Test
    @DisplayName("認証マネージャーを構成しログイン画面を公開する")
    void testSecurityBeansAndPublicLogin() throws Exception {
        assertThat(authenticationManager).isNotNull();

        mockMvc.perform(MockMvcRequestBuilders.get("/login"))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @DisplayName("店員専用パスとユーザー専用パスを権限で保護する")
    void testProtectedRoutes() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/w/test"))
                .andExpect(MockMvcResultMatchers.redirectedUrl("/login"));
        mockMvc.perform(MockMvcRequestBuilders.get("/w/test")
                .with(authenticated("店員")))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
        mockMvc.perform(MockMvcRequestBuilders.get("/user/test")
                .with(authenticated("ROLE_2")))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    private RequestPostProcessor authenticated(String authority) {
        return request -> {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                    "test-user", "password", List.of(new SimpleGrantedAuthority(authority))));
            request.getSession(true).setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
            return request;
        };
    }

    @RestController
    public static class TestController {
        @GetMapping({ "/w/test", "/user/test" })
        public String protectedEndpoint() {
            return "ok";
        }
    }
}
