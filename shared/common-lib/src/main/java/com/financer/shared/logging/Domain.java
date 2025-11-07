package com.financer.shared.logging;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Enum que define os domínios do sistema
 */
@Getter
@AllArgsConstructor
public enum Domain {
    ACCOUNT("ACCOUNT", "Gestão de Contas"),
    TRANSACTION("TRANSACTION", "Gestão de Transações"),
    CARD("CARD", "Gestão de Cartões"),
    ORCHESTRATION("ORCHESTRATION", "Orquestração de Serviços"),
    REQUEST("REQUEST", "Controle de Solicitações"),
    AUDIT("AUDIT", "Auditoria"),
    SECURITY("SECURITY", "Segurança"),
    INFRASTRUCTURE("INFRASTRUCTURE", "Infraestrutura");

    private final String code;
    private final String description;
}