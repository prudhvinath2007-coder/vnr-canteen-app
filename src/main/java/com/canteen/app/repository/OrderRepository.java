package com.canteen.app.repository;

import com.canteen.app.model.Order;
import com.canteen.app.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByOrderStatus(OrderStatus status);

    List<Order> findByOrderStatusInOrderByOrderTimeAsc(List<OrderStatus> statuses);

    List<Order> findByStudentRollNoOrderByOrderTimeDesc(String studentRollNo);

    List<Order> findAllByOrderByOrderTimeDesc();

    Optional<Order> findTopByOrderByTokenNumberDesc();

    long countByOrderStatus(OrderStatus status);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderTime >= :since")
    long countOrdersSince(@Param("since") LocalDateTime since);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.orderStatus = 'DELIVERED'")
    BigDecimal calculateTotalRevenue();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.orderStatus = 'DELIVERED' AND o.orderTime >= :since")
    BigDecimal calculateRevenueSince(@Param("since") LocalDateTime since);
}
