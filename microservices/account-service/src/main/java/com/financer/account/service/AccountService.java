package com.financer.account.service;

import com.financer.account.dto.*;
import com.financer.account.entity.Account;
import com.financer.account.entity.Account.AccountStatus;
import com.financer.account.entity.Account.AccountType;
import com.financer.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountResponse createAccount(CreateAccountRequest request) {
        Account account = new Account();
        account.setAccountNumber(generateAccountNumber());
        account.setOwnerName(request.getOwnerName());
        account.setOwnerDocument(request.getOwnerDocument());
        account.setOwnerEmail(request.getOwnerEmail());
        account.setAccountType(request.getAccountType());
        account.setBalance(request.getInitialBalance() != null ? request.getInitialBalance() : BigDecimal.ZERO);
        account.setDescription(request.getDescription());
        account.setStatus(AccountStatus.ACTIVE);

        Account savedAccount = accountRepository.save(account);
        return convertToResponse(savedAccount);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountById(UUID id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + id));
        return convertToResponse(account);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found with number: " + accountNumber));
        return convertToResponse(account);
    }

    @Transactional(readOnly = true)
    public Page<AccountResponse> getAllAccounts(Pageable pageable) {
        return accountRepository.findAll(pageable)
                .map(this::convertToResponse);
    }

    public AccountResponse updateAccount(UUID id, UpdateAccountRequest request) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + id));

        if (request.getOwnerName() != null) {
            account.setOwnerName(request.getOwnerName());
        }
        if (request.getOwnerEmail() != null) {
            account.setOwnerEmail(request.getOwnerEmail());
        }
        if (request.getDescription() != null) {
            account.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            account.setStatus(request.getStatus());
        }

        Account savedAccount = accountRepository.save(account);
        return convertToResponse(savedAccount);
    }

    public void deleteAccount(UUID id) {
        if (!accountRepository.existsById(id)) {
            throw new RuntimeException("Account not found with id: " + id);
        }
        accountRepository.deleteById(id);
    }

    public BalanceOperationResponse deposit(UUID accountId, BalanceOperationRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + accountId));

        if (!account.isActive()) {
            throw new RuntimeException("Account is not active");
        }

        BigDecimal previousBalance = account.getBalance();
        account.addBalance(request.getAmount());
        Account savedAccount = accountRepository.save(account);

        BalanceOperationResponse response = new BalanceOperationResponse();
        response.setAccountId(savedAccount.getId());
        response.setAccountNumber(savedAccount.getAccountNumber());
        response.setOperationType("DEPOSIT");
        response.setOperationAmount(request.getAmount());
        response.setPreviousBalance(previousBalance);
        response.setCurrentBalance(savedAccount.getBalance());
        response.setOperationTime(LocalDateTime.now());
        response.setMessage("Deposit successful");
        response.setSuccess(true);
        return response;
    }

    public BalanceOperationResponse withdraw(UUID accountId, BalanceOperationRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + accountId));

        if (!account.isActive()) {
            throw new RuntimeException("Account is not active");
        }

        if (!account.hasSufficientBalance(request.getAmount())) {
            throw new RuntimeException("Insufficient balance");
        }

        BigDecimal previousBalance = account.getBalance();
        account.subtractBalance(request.getAmount());
        Account savedAccount = accountRepository.save(account);

        BalanceOperationResponse response = new BalanceOperationResponse();
        response.setAccountId(savedAccount.getId());
        response.setAccountNumber(savedAccount.getAccountNumber());
        response.setOperationType("WITHDRAWAL");
        response.setOperationAmount(request.getAmount());
        response.setPreviousBalance(previousBalance);
        response.setCurrentBalance(savedAccount.getBalance());
        response.setOperationTime(LocalDateTime.now());
        response.setMessage("Withdrawal successful");
        response.setSuccess(true);
        return response;
    }

    @Transactional(readOnly = true)
    public Page<AccountResponse> searchAccounts(String ownerName, String ownerDocument, 
                                              AccountType accountType, AccountStatus status, 
                                              Pageable pageable) {
        Page<Account> accounts;
        
        if (ownerDocument != null) {
            accounts = accountRepository.findByOwnerDocumentContainingIgnoreCase(ownerDocument, pageable);
        } else if (ownerName != null) {
            accounts = accountRepository.findByOwnerNameContainingIgnoreCase(ownerName, pageable);
        } else if (accountType != null && status != null) {
            accounts = accountRepository.findByAccountTypeAndStatus(accountType, status, pageable);
        } else if (accountType != null) {
            accounts = accountRepository.findByAccountType(accountType, pageable);
        } else if (status != null) {
            accounts = accountRepository.findByStatus(status, pageable);
        } else {
            accounts = accountRepository.findAll(pageable);
        }
        
        return accounts.map(this::convertToResponse);
    }

    private AccountResponse convertToResponse(Account account) {
        AccountResponse response = new AccountResponse();
        response.setId(account.getId());
        response.setAccountNumber(account.getAccountNumber());
        response.setOwnerName(account.getOwnerName());
        response.setOwnerDocument(account.getOwnerDocument());
        response.setOwnerEmail(account.getOwnerEmail());
        response.setAccountType(account.getAccountType());
        response.setBalance(account.getBalance());
        response.setStatus(account.getStatus());
        response.setDescription(account.getDescription());
        response.setCreatedAt(account.getCreatedAt());
        response.setUpdatedAt(account.getUpdatedAt());
        // Os campos não mapeados da Account ficarão como null por padrão
        return response;
    }

    private String generateAccountNumber() {
        // Gera um número de conta único baseado no timestamp
        return "ACC" + System.currentTimeMillis();
    }
}