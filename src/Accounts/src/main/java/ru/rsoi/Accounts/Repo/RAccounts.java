package ru.rsoi.Accounts.Repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.rsoi.Accounts.Model.MAccount;

import java.util.List;
import java.util.UUID;

@Repository
public interface RAccounts extends JpaRepository<MAccount, Integer> {

    @Query("SELECT c FROM MAccount c where c.accuid = ?1")
    public List<MAccount> findAccByUid(UUID uid);

    @Query("SELECT c FROM MAccount c where c.username = ?1")
    public List<MAccount> findAccByUName(String uname);

    @Query("SELECT c FROM MAccount c where (c.username = ?1 and c.password = ?2)")
    public List<MAccount> findAcc(String uname, String password);

}
