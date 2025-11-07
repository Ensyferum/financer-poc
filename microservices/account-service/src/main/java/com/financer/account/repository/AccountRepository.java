package com.financer.account.repository;

import com.financer.account.entity.Account;
import com.financer.account.entity.Account.AccountStatus;
import com.financer.account.entity.Account.AccountType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Account Repository
 * 
 * Data access layer for Account entities with custom query methods
 * for account management operations.
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    /**
     * Find account by account number
     */
    Optional<Account> findByAccountNumber(String accountNumber);

    /**
     * Check if account number exists
     */
    boolean existsByAccountNumber(String accountNumber);

    /**
     * Find accounts by owner document
     */
    List<Account> findByOwnerDocument(String ownerDocument);

    /**
     * Find accounts by owner document (case-insensitive)
     */
    Page<Account> findByOwnerDocumentContainingIgnoreCase(String ownerDocument, Pageable pageable);

    /**
     * Find accounts by owner name (case-insensitive)
     */
    Page<Account> findByOwnerNameContainingIgnoreCase(String ownerName, Pageable pageable);

    /**
     * Find accounts by status
     */
    Page<Account> findByStatus(AccountStatus status, Pageable pageable);

    /**
     * Find accounts by account type
     */
    Page<Account> findByAccountType(AccountType accountType, Pageable pageable);

    /**
     * Find accounts by account type and status
     */
    Page<Account> findByAccountTypeAndStatus(AccountType accountType, AccountStatus status, Pageable pageable);

    /**
     * Find accounts by status and type
     */
    Page<Account> findByStatusAndAccountType(AccountStatus status, AccountType accountType, Pageable pageable);

    /**
     * Find accounts with balance greater than specified amount
     */
    @Query("SELECT a FROM Account a WHERE a.balance > :minBalance AND a.status = :status")
    List<Account> findAccountsWithBalanceGreaterThan(@Param("minBalance") BigDecimal minBalance, 
                                                     @Param("status") AccountStatus status);

    /**
     * Find accounts with balance between specified amounts
     */
    @Query("SELECT a FROM Account a WHERE a.balance BETWEEN :minBalance AND :maxBalance AND a.status = :status")
    Page<Account> findAccountsWithBalanceBetween(@Param("minBalance") BigDecimal minBalance,
                                                 @Param("maxBalance") BigDecimal maxBalance,
                                                 @Param("status") AccountStatus status,
                                                 Pageable pageable);

    /**
     * Count accounts by status
     */
    long countByStatus(AccountStatus status);

    /**
     * Count accounts by account type
     */
    long countByAccountType(AccountType accountType);

    /**
     * Find active accounts by owner document
     */
    @Query("SELECT a FROM Account a WHERE a.ownerDocument = :ownerDocument AND a.status = 'ACTIVE'")
    List<Account> findActiveAccountsByOwnerDocument(@Param("ownerDocument") String ownerDocument);

    /**
     * Get total balance by owner document
     */
    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a WHERE a.ownerDocument = :ownerDocument AND a.status = 'ACTIVE'")
    BigDecimal getTotalBalanceByOwnerDocument(@Param("ownerDocument") String ownerDocument);

    /**
     * Find accounts by branch code
     */
    List<Account> findByBranchCode(String branchCode);

    /**
     * Find accounts by bank code
     */
    List<Account> findByBankCode(String bankCode);

    /**
     * Search accounts by multiple criteria
     */
    @Query("SELECT a FROM Account a WHERE " +
           "(:ownerName IS NULL OR LOWER(a.ownerName) LIKE LOWER(CONCAT('%', :ownerName, '%'))) AND " +
           "(:accountType IS NULL OR a.accountType = :accountType) AND " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:branchCode IS NULL OR a.branchCode = :branchCode)")
    Page<Account> searchAccounts(@Param("ownerName") String ownerName,
                                @Param("accountType") AccountType accountType,
                                @Param("status") AccountStatus status,
                                @Param("branchCode") String branchCode,
                                Pageable pageable);
}