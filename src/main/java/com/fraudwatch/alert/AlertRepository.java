package com.fraudwatch.alert;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<FraudAlert, Long> {

    List<FraudAlert> findByStatusOrderByCreatedAtDescIdDesc(AlertStatus status);

    List<FraudAlert> findAllByOrderByCreatedAtDescIdDesc();
}
