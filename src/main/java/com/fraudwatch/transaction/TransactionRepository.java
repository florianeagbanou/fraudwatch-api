package com.fraudwatch.transaction;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<BankTransaction, Long> {

    /** Recent history used by the fraud rules (most recent first). */
    List<BankTransaction> findTop50ByAccountIdOrderByCreatedAtDescIdDesc(Long accountId);

    List<BankTransaction> findByAccountIdOrderByCreatedAtDescIdDesc(Long accountId);
}
