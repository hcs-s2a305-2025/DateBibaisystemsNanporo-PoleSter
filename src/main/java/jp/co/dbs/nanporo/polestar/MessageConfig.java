package jp.co.dbs.nanporo.polestar;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.HttpClientErrorException.NotFound;

@Configuration 
public class MessageConfig {

    /** 【エラー】メールアドレス未入力 */
    public static final String USERID_REQUIRED_MESSAGE = "【M001】メールアドレスを入力してください。";
    
    /** 【エラー】パスワード未入力 */
    public static final String PASSWORD_REQUIRED_MESSAGE = "【M002】パスワードを入力してください。";
    
    /** 【エラー】認証失敗 */
    public static final String LOGIN_FAILED_MESSAGE = "【M003】認証に失敗しました。メールアドレスとパスワードをご確認のうえ、もう一度お試しください。";
    
    /** 【エラー】パスワード不一致 */
    public static final String PASSWORD_MISMATCH_MESSAGE = "【M004】パスワードが一致しません。もう一度入力してください。";
    
    /** 【エラー】登録済みアカウント */
    public static final String REGISTERED_Account_MESSAGE = "【M005】すでに登録済みのアカウントです。";
    
    /** 【エラー】画面遷移失敗 */
    public static final String PAGELOAD_FAILED_MESSAGE = "【M006】エラーが発生しました。もう一度お試しください。";
    
    /** 【エラー】通信エラー */
    public static final String COMMUNICATION_FAILURE_MESSAGE = "【M007】通信エラーが発生しました。通信環境をご確認のうえ、もう一度お試しください。";
    
    /** 【成功】変更保存 */
    public static final String SAVE_CHANGES_MESSAGE = "【M008】変更を保存しました。";
    
    /** 【失敗】変更失敗 */
    public static final String CHANGE_FAILED_MESSAGE = "【M009】変更できませんでした。もう一度お試しください。";
    
    /** 【確認】変更保存確認 */
    public static final String CONFIRM_SAVE_CHANGES_MESSAGE = "【M010】変更内容を保存してよろしいですか。";
    
    /** 【確認】変更破棄確認 */
    public static final String CONFIRM_DISCARDING_CHANGES_MESSAGE = "【M011】変更を破棄して戻ってもよろしいですか。";
    
    /** 【エラー】注文追加エラー */
    public static final String ORDER_ADD_FAILED_MESSAGE = "【M101】注文の追加に失敗しました。お手数ですが、もう一度お試しください。";
    
    /** 【確認】注文確認 */
    public static final String CONFIRM_ORDER_MESSAGE = "【M102】こちらの内容でご注文してよろしいですか？";
    
    /** 【成功】注文確定 */
    public static final String ORDER_REGISTERED_MESSAGE = "【M103】注文を確定しました。";
    
    /** 【成功】注文取消 */
    public static final String ORDER_CANCELLATION_MESSAGE = "【M104】注文を取り消しました。";
    
    /** 【エラー】注文失敗 */
    public static final String ORDER_FAILED_MESSAGE = "【M105】注文に失敗しました。再度お試しください。";
    
    /** 【エラー】金額マイナスエラー */
    public static final String NEGATIVE_AMOUNT_MESSAGE = "【M201】正しい金額を入力してください。 ";
    
    /** 【エラー】金額不足エラー */
    public static final String DISCREPANCY_INAMOUNT_MESSAGE = "【M202】支払い金額が不足しています。";
    
    /** 【エラー】予約番号参照エラー */
    public static final String RESERVATION_NUMBER_NOT_FOUND_MESSAGE = "【M203】対象の予約番号が存在しません。";
    
    /** 【エラー】日付チェック */
    public static final String DATE_CHECK_MESSAGE = "【M204】正しい日付を選択してください。";
    
    /** 【エラー】データ空エラー */
    public static final String DATA_EMPTY_MESSAGE = "【M205】データが存在しません。";
    
    /** 【確認】システム休止確認 */
    public static final String CONFIRM_SYSTEM_OUTAGE_MESSAGE = "【M301】システムを停止してもよろしいですか。";
    
    /** 【エラー】システム休止エラー */
    public static final String SYSTEM_NOT_STOPPED_MESSAGE = "【M302】システムを停止できませんでした。もう一度お試しください。";
    
    /** 【確認】一斉送信確認 */
    public static final String CONFIRM_MAIL_SEND_MESSAGE = "【M303】送信してもよろしいですか。";
    
    /** 【エラー】一斉送信内容未入力 */
    public static final String MAIL_SEND_CONTENT_MESSAGE = "【M304】内容を入力してください。";
    
    /** 【エラー】一斉送信エラー */
    public static final String MAIL_SEND_FAILED_MESSAGE = "【M305】一斉送信できませんでした。もう一度お試しください。";
    
    /** 【エラー】ユーザ不一致 */
    public static final String USER_NOT_FOUND_MESSAGE = "【M306】該当するユーザが見つかりません。入力内容をご確認ください。 ";
    
    /** 【確認】ユーザ削除確認 */
    public static final String CONFIRM_USER_DELETION_MESSAGE = "【M307】このユーザを削除してもよろしいですか。";
    
    /** 【エラー】ユーザ登録エラー */
    public static final String USER_REGISTRATION_FAILED_MESSAGE = "【M308】ユーザの登録に失敗しました。もう一度お試しください。 ";
    
    /** 【確認】ユーザ停止確認 */
    public static final String CONFIRM_USER_SUSPENSION_MESSAGE = "【M309】このユーザの利用を停止してもよろしいですか。";
    
}
