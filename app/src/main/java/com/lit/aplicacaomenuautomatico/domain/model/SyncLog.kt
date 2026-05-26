package com.lit.aplicacaomenuautomatico.domain.model

// Sem imports externos — modelo de domínio puro, sem dependências de Room ou Gson.

// ═════════════════════════════════════════════════════════════════════════════
// MODELO DE DOMÍNIO: SyncLog
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Modelo de domínio de um registro de log de sincronização com o SAP.
 * Espelha [SyncLogEntity] sem nenhuma dependência do Room — desacopla a UI da camada de dados.
 *
 * Cada sincronização (manual ou via WorkManager) gera um registro aqui,
 * permitindo rastreamento histórico de quando e com qual resultado os dados foram atualizados.
 */
data class SyncLog(
    /** Identificador único gerado automaticamente pelo Room ao inserir o registro */
    val id: Long,

    /** Data e hora da sincronização no formato ISO 8601 (ex.: "2026-05-07T14:32:00") */
    val timestamp: String,

    /**
     * Resultado da sincronização:
     *  "SUCCESS" = dados do SAP baixados e persistidos com sucesso
     *  "ERROR"   = falha na chamada OData ou na gravação no banco
     */
    val status: String,

    /** Quantidade de registros inseridos ou atualizados na tabela menu_app nesta sincronização */
    val registrosAtualizados: Int,

    /**
     * Descrição do erro caso status = "ERROR".
     * null quando status = "SUCCESS" — não há erro para descrever.
     */
    val mensagemErro: String?
)
