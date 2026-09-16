package jp.co.dbs.nanporo.polestar;

import org.springframework.context.annotation.Configuration;

@Configuration 
public class MessageConfig {

    /** 【エラー】メールアドレス未入力 */
    public static final String USERID_REQUIRED_MESSAGE = "【M001】メールアドレスを入力してください。";
    
}
