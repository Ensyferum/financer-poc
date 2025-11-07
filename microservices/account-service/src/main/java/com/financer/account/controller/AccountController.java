package com.financer.account.controller;

import com.financer.account.dto.*;
import com.financer.account.entity.Account.AccountStatus;
import com.financer.account.entity.Account.AccountType;
import com.financer.account.service.AccountService;
import com.financer.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        try {
            AccountResponse account = accountService.createAccount(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(account, "Account created successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to create account: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccountById(@PathVariable UUID id) {
        try {
            AccountResponse account = accountService.getAccountById(id);
            return ResponseEntity.ok(ApiResponse.success(account, "Account found successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Account not found: " + e.getMessage()));
        }
    }

    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccountByNumber(@PathVariable String accountNumber) {
        try {
            AccountResponse account = accountService.getAccountByNumber(accountNumber);
            return ResponseEntity.ok(ApiResponse.success(account, "Account found successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Account not found: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AccountResponse>>> getAllAccounts(@PageableDefault(size = 20) Pageable pageable) {
        try {
            Page<AccountResponse> accounts = accountService.getAllAccounts(pageable);
            return ResponseEntity.ok(ApiResponse.success(accounts, "Accounts retrieved successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve accounts: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountResponse>> updateAccount(@PathVariable UUID id, @Valid @RequestBody UpdateAccountRequest request) {
        try {
            AccountResponse account = accountService.updateAccount(id, request);
            return ResponseEntity.ok(ApiResponse.success(account, "Account updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to update account: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@PathVariable UUID id) {
        try {
            accountService.deleteAccount(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Account deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Failed to delete account: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<ApiResponse<BalanceOperationResponse>> deposit(@PathVariable UUID id, @Valid @RequestBody BalanceOperationRequest request) {
        try {
            BalanceOperationResponse response = accountService.deposit(id, request);
            return ResponseEntity.ok(ApiResponse.success(response, "Deposit successful"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Deposit failed: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<ApiResponse<BalanceOperationResponse>> withdraw(@PathVariable UUID id, @Valid @RequestBody BalanceOperationRequest request) {
        try {
            BalanceOperationResponse response = accountService.withdraw(id, request);
            return ResponseEntity.ok(ApiResponse.success(response, "Withdrawal successful"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Withdrawal failed: " + e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<AccountResponse>>> searchAccounts(
            @RequestParam(required = false) String ownerName,
            @RequestParam(required = false) String ownerDocument,
            @RequestParam(required = false) AccountType accountType,
            @RequestParam(required = false) AccountStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        try {
            Page<AccountResponse> accounts = accountService.searchAccounts(ownerName, ownerDocument, accountType, status, pageable);
            return ResponseEntity.ok(ApiResponse.success(accounts, "Search completed successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Search failed: " + e.getMessage()));
        }
    }
}