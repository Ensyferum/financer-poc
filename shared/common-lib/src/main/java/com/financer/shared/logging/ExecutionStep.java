package com.financer.shared.logging;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Enum que define as etapas de execução
 */
@Getter
@AllArgsConstructor
public enum ExecutionStep {
    START("START", "Início da execução"),
    VALIDATION("VALIDATION", "Validação de dados"),
    PROCESSING("PROCESSING", "Processamento"),
    PERSISTENCE("PERSISTENCE", "Persistência de dados"),
    INTEGRATION("INTEGRATION", "Integração com serviços"),
    FINISH("FINISH", "Finalização"),
    ERROR("ERROR", "Erro na execução"),
    ROLLBACK("ROLLBACK", "Rollback de transação");

    private final String code;
    private final String description;
}