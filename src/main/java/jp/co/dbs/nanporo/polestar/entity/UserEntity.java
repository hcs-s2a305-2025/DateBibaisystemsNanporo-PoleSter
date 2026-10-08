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
    private Integer cancelCount;

    // 状態　BOOLEAN型
    private Boolean alive;

    // ポイント　INTEGER型
    private Integer point;

    // ポイントカード達成枚数　INTEGER型
    private  Integer pointCardComplete;

    // 未使用のスタンプカード割引券枚数　INTEGER型
    private Integer stampCoupon;

    // スタンプカード割引の提示中フラグ　BOOLEAN型
    private Boolean stampCouponActive;

    // アイコン　VARCHAR型
    private String icon;
}