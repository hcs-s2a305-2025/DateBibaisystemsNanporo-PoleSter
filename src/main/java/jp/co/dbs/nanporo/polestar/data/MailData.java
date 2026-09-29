package jp.co.dbs.nanporo.polestar.data;

import lombok.Data;

/**
 * メール送信APIへ渡すメール情報
 */
@Data
public class MailData {

    /** 送信先メールアドレス */
    private String to;

    /** 送信元メールアドレス */
    private String from;

    /** メール件名 */
    private String subject;

    /** メール本文 */
    private String body;
}