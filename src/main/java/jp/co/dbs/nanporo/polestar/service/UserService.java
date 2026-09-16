package jp.co.dbs.nanporo.polestar.service;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import jp.co.dbs.nanporo.polestar.entity.UserEntity;
import jp.co.dbs.nanporo.polestar.repository.UserRepository;
import jp.co.dbs.nanporo.polestar.response.UserGetResponse;

@Service 
public class UserService {

    @Autowired 
    private UserRepository repository;

    public  UserGetResponse getStaffList(Pageable pageable) {
        // リポジトリに処理を依頼
        List<Map<String, Object>> resultSet = repository.getStaffList(pageable);
        
        // テーブル構成からエンティティクラスへ変換
        List<UserEntity> staffList = toResponse(resultSet);

        int totalCount = repository.countStaffList();
        int totalPages = (int) Math.ceil((double) totalCount / pageable.getPageSize());

        // レスポンスクラスを生成
        UserGetResponse response = new UserGetResponse();
        response.setUsers(staffList);
        response.setTotalPages(totalPages);
        return response;
    }

    private List<UserEntity> toResponse(List<Map<String, Object>> resultSet) {
        // 配列の初期化
        List<UserEntity> users = new ArrayList<UserEntity>();

        // テーブル行ごとの繰り返し
        for (Map<String, Object> row : resultSet) {
            UserEntity user = new UserEntity();
            user.setMail((String) row.get("mail"));
            user.setName((String) row.get("name"));
            user.setPassword((String) row.get("password"));
            user.setRole((String) row.get("role"));
            user.setMemberRank((String) row.get("memberRank"));
            user.setGender((String) row.get("gender"));
            user.setBirthday((Date) row.get("birthday"));
            user.setCancelCount((int) row.get("cancelCount"));
            user.setAlive((boolean) row.get("alive"));
            user.setPoint((int) row.get("point"));
            user.setPointCardComplete((int) row.get("pointCardComplete"));

            users.add(user);
        }
        return users;
    }
}
