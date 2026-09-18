package jp.co.dbs.nanporo.polestar.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import jp.co.dbs.nanporo.polestar.data.UserData;
import jp.co.dbs.nanporo.polestar.entity.UserEntity;
import jp.co.dbs.nanporo.polestar.service.UserService;

@Service
public class LoginUserDetailsService implements UserDetailsService {

	
	@Autowired
	@Lazy
	private UserService service;
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		// データへ変換
		UserData userData = new UserData();
		userData.setMail(username);
		// ユーザ情報の取得
		UserEntity userEntity = service.getUser(userData);
		// セッションへ格納
		return new LoginUserDetails(userEntity);
		
	}

}

