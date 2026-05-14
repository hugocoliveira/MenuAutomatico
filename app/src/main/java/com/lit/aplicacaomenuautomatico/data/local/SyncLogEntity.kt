package com.lit.aplicacaomenuautomatico.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidade Room da tabela de log de sincronizações com o SAP.
 * Cada registro representa uma tentativa de sincronização (bem-sucedida ou falha),
 * permitindo rastreamento histórico das atualizações do banco local.
 */
@Entity(tableName = "sync_log")
data class SyncLogEntity(
    /** Identificador único auto-gerado pelo Room */
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Data e hora da sincronização no formato ISO 8601 (ex.: "2026-05-07T14:32:00") */
    val timestamp: String,
    /** Resultado da sincronização: "SUCCESS" ou "ERROR" */
    val status: String,
    /** Quantidade de registros inseridos/atualizados na tabela menu_app */
    val registrosAtualizados: Int,
    /** Mensagem descritiva em caso de falha — null quando status = "SUCCESS" */
    val mensagemErro: String?
)
