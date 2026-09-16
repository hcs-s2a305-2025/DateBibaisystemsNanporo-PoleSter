package jp.co.dbs.nanporo.polestar.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import jp.co.dbs.nanporo.polestar.data.UserData;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Repository 
public class UserRepository {

    // Javaでデータベース操作を行うライブラリ
    @Autowired 
    private NamedParameterJdbcTemplate jdbc;

    // 権限が店員・店長のユーザ一覧を取得
    private static final String SELECT_STAFF_LIST = 
        "SELECT mail, role, name, alive FROM user_m WHERE role = '店員' OR role = '店長' LIMIT :limit OFFSET :offset";

    private static final String COUNT_STAFF_LIST = 
        "SELECT COUNT(*) FROM user_m WHERE role = '店員' OR role = '店長'";

    public List<Map<String, Object>> getStaffList(Pageable pageable) {
        // クエリのパラメータを設定するマップ
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("limit", pageable.getPageSize());
        params.put("offset", pageable.getOffset());

        // SELECT_STAFF_LISTクエリを実行し、結果を取得
        return jdbc.queryForList(SELECT_STAFF_LIST, params);
    }
    
    // 総件数を取得
    public int countStaffList() {
        return jdbc.queryForObject(COUNT_STAFF_LIST, new HashMap<>(), Integer.class);
    }
}
