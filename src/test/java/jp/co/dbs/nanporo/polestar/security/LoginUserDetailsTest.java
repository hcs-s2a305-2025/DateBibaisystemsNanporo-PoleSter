package jp.co.dbs.nanporo.polestar.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import jp.co.dbs.nanporo.polestar.entity.UserEntity;

class LoginUserDetailsTest {

    @Test
    @DisplayName("ログインユーザーの権限・識別情報を生成する")
    void testUserDetailsProperties() {
        UserEntity entity = new UserEntity();
        entity.setMail("user@example.com");
        entity.setPassword("encodedPassword");
        entity.setRole("店長");
        entity.setAlive(false);

        LoginUserDetails details = new LoginUserDetails(entity);

        assertThat(details.getUsername()).isEqualTo("user@example.com");
        assertThat(details.getPassword()).isEqualTo("encodedPassword");
        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_店長");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.isAccountNonExpired()).isTrue();
        assertThat(details.isCredentialsNonExpired()).isTrue();
    }

    @Test
    @DisplayName("アカウントが有効な場合は無効とみなす")
    void testDisabledWhenAliveIsTrue() {
        UserEntity entity = new UserEntity();
        entity.setMail("user@example.com");
        entity.setPassword("pass");
        entity.setRole("店員");
        entity.setAlive(true);

        LoginUserDetails details = new LoginUserDetails(entity);

        assertThat(details.isEnabled()).isFalse();
    }
}
