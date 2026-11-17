package com.fraudwatch.account;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByOwnerUsernameOrderByIdAsc(String username);

    boolean existsByNumber(String number);
}
