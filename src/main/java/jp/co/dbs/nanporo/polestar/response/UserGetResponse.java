package jp.co.dbs.nanporo.polestar.response;

import java.util.List;

import jp.co.dbs.nanporo.polestar.entity.UserEntity;
import lombok.Data;

@Data
public class UserGetResponse {
    
    // ユーザ一覧
    private List<UserEntity> users;

    // 総ページ数
    private int totalPages;
}
