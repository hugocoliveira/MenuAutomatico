package com.lit.aplicacaomenuautomatico.domain.model

/**
 * Modelo de domínio de um registro de log de sincronização.
 * Espelha [SyncLogEntity] sem dependências de Room.
 */
data class SyncLog(
    val id: Long,
    /** Data e hora no formato ISO 8601 */
    val timestamp: String,
    /** "SUCCESS" ou "ERROR" */
    val status: String,
    /** Quantidade de registros atualizados na tabela menu_app */
    val registrosAtualizados: Int,
    /** Mensagem de erro — null quando status = "SUCCESS" */
    val mensagemErro: String?
)
