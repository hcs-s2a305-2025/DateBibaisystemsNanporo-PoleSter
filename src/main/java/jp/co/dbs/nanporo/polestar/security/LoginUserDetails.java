package jp.co.dbs.nanporo.polestar.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jp.co.dbs.nanporo.polestar.entity.UserEntity;


/**
* Spring Securityがログイン処理時に使用する、「ユーザー情報のデータ構造」
* ログイン中のユーザーの情報（ユーザー名、パスワード、アカウント状態、権限など）を表現するために使われる
**/
public class LoginUserDetails implements UserDetails {
    private final UserEntity userEntity;
    public LoginUserDetails(UserEntity userEntity) {
        this.userEntity = userEntity;
    }
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + userEntity.getRole().toUpperCase()));
    }
    @Override
    public String getPassword() {
        return userEntity.getPassword();
    }
    @Override
    public String getUsername() {
        return userEntity.getMail();
    }
    @Override
    public boolean isAccountNonExpired() {
        // アカウント有効期限を考慮しないためすべて許可
        return true;
    }
    @Override
    public boolean isCredentialsNonExpired() {
        // パスワード有効期限を考慮しないためすべて許可
        return true;
    }
    @Override
    public boolean isEnabled() {
        return Boolean.FALSE.equals(userEntity.getAlive());
    }
}
