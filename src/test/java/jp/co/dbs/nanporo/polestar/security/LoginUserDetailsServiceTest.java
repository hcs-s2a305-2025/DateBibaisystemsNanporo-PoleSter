package jp.co.dbs.nanporo.polestar.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jp.co.dbs.nanporo.polestar.data.UserData;
import jp.co.dbs.nanporo.polestar.entity.UserEntity;
import jp.co.dbs.nanporo.polestar.service.UserService;

@ExtendWith(MockitoExtension.class)
class LoginUserDetailsServiceTest {

    @Mock
    private UserService service;

    @InjectMocks
    private LoginUserDetailsService serviceUnderTest;

    @Test
    @DisplayName("ユーザー名からUserDetailsを生成する")
    void testLoadUserByUsername() {
        UserEntity entity = new UserEntity();
        entity.setMail("user@example.com");
        entity.setPassword("encoded");
        entity.setRole("店員");
        entity.setAlive(false);
        when(service.getUser(any(UserData.class))).thenReturn(entity);

        UserDetails details = serviceUnderTest.loadUserByUsername("user@example.com");

        assertThat(details.getUsername()).isEqualTo("user@example.com");
        assertThat(details.getPassword()).isEqualTo("encoded");
        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_店員");
        assertThat(details.isEnabled()).isTrue();
        verify(service).getUser(argThat(userData -> "user@example.com".equals(userData.getMail())));
    }
}
