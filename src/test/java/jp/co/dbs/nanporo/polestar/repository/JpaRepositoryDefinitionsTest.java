package jp.co.dbs.nanporo.polestar.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jp.co.dbs.nanporo.polestar.entity.OrderDetailEntity;
import jp.co.dbs.nanporo.polestar.entity.OrderDetailKey;
import jp.co.dbs.nanporo.polestar.entity.OrderEntity;
import jp.co.dbs.nanporo.polestar.entity.TransactionDetailEntity;
import jp.co.dbs.nanporo.polestar.entity.TransactionDetailKey;
import jp.co.dbs.nanporo.polestar.entity.TransactionEntity;

class JpaRepositoryDefinitionsTest {

    @Test
    @DisplayName("Spring Data RepositoryのEntity型とID型を確認する")
    void testJpaRepositoryTypes() {
        assertJpaRepository(OrderDetailRepository.class, OrderDetailEntity.class, OrderDetailKey.class);
        assertJpaRepository(OrderTRepository.class, OrderEntity.class, Integer.class);
        assertJpaRepository(TransactionDetailRepository.class,
                TransactionDetailEntity.class, TransactionDetailKey.class);
        assertJpaRepository(TransactionRepository.class, TransactionEntity.class, Integer.class);
    }

    @Test
    @DisplayName("注文IDによる派生検索の戻り値型を確認する")
    void testOrderDetailsDerivedFinder() throws NoSuchMethodException {
        Method finder = OrderDetailRepository.class.getMethod("findByOrderId", Integer.class);

        assertThat(finder.getReturnType()).isEqualTo(List.class);
        assertThat(((ParameterizedType) finder.getGenericReturnType()).getActualTypeArguments()[0])
                .isEqualTo(OrderDetailEntity.class);
    }

    @Test
    @DisplayName("当日の注文番号検索JPQLと名前付き引数を確認する")
    void testTodayOrderQueryDefinition() throws NoSuchMethodException {
        Method finder = OrderTRepository.class.getMethod("findTodayOrderByNumber",
                String.class, LocalDateTime.class, LocalDateTime.class);
        Query query = finder.getAnnotation(Query.class);

        assertThat(query).isNotNull();
        assertThat(query.value()).contains("FROM OrderEntity o", "o.orderNumber = :orderNumber",
                "o.getTime >= :startOfDay", "o.getTime <= :endOfDay");
        assertThat(finder.getReturnType()).isEqualTo(Optional.class);
        assertThat(finder.getParameters()[0].getAnnotation(Param.class).value()).isEqualTo("orderNumber");
        assertThat(finder.getParameters()[1].getAnnotation(Param.class).value()).isEqualTo("startOfDay");
        assertThat(finder.getParameters()[2].getAnnotation(Param.class).value()).isEqualTo("endOfDay");
    }

    private void assertJpaRepository(Class<?> repositoryType, Class<?> entityType, Class<?> idType) {
        ParameterizedType declaration = (ParameterizedType) repositoryType.getGenericInterfaces()[0];

        assertThat(declaration.getRawType()).isEqualTo(JpaRepository.class);
        assertThat(declaration.getActualTypeArguments()).containsExactly(entityType, idType);
    }
}