package jp.co.dbs.nanporo.polestar.data;

import java.sql.Date;

import lombok.Data;

@Data 
public class UserData {
    
    // メールアドレス
    private String mail;

    // 名前
    private String name;

    // 暗号化済みパスワード
    private String password;

    // 権限
    private String role;

    // 会員ランク
    private String memberRank;

    // 性別
    private String gender;

    // 誕生日
    private Date birthday;

    // 迷惑行為件数
    private int cancelCount;

    // 状態
    private boolean alive;

    // ポイント
    private int point;

    // ポイントカード達成枚数
    private int pointCardComplete;
}
