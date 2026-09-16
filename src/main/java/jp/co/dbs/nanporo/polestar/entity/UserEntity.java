package jp.co.dbs.nanporo.polestar.entity;

import java.sql.Date;

import lombok.Data;

@Data 
public class UserEntity {

    // メールアドレス　VARCHAR型
    private String mail;

    // 名前　VARCHAR型
    private String name;

    // 暗号化済みパスワード　VARCHAR型
    private String password;

    // 権限　VARCHAR型
    private String role;

    // 会員ランク　VARCHAR型
    private  String memberRank;

    // 性別　VARCHAR型
    private  String gender;

    // 誕生日　DATE型
    private Date birthday;

    // 迷惑行為件数　INTEGER型
    private int cancelCount;

    // 状態　BOOLEAN型
    private boolean alive;

    // ポイント　INTEGER型
    private int point;

    // ポイントカード達成枚数　INTEGER型
    private  int pointCardComplete;
}