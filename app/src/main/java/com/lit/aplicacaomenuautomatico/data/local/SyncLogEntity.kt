package com.lit.aplicacaomenuautomatico.data.local

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Room
// ─────────────────────────────────────────────────────────────────────────────
import androidx.room.Entity     // anotação que mapeia esta data class para uma tabela SQLite
import androidx.room.PrimaryKey // define a chave primária da tabela (auto-gerada aqui)

// ═════════════════════════════════════════════════════════════════════════════
// ENTIDADE ROOM: sync_log
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Entidade Room da tabela de log de sincronizações com o SAP.
 *
 * Tabela: sync_log
 * Cada registro representa uma tentativa de sincronização — bem-sucedida ou não.
 * Permite rastreamento histórico das atualizações do banco local para auditoria
 * e diagnóstico de problemas de conectividade ou autenticação.
 *
 * Gravada pelo MenuRepository após cada chamada ao endpoint OData,
 * independente do resultado (sucesso ou falha).
 */
@Entity(tableName = "sync_log") // nome da tabela no arquivo SQLite local
data class SyncLogEntity(

    /**
     * Identificador único do registro.
     * autoGenerate = true: o Room atribui automaticamente um Long sequencial crescente.
     * Valor padrão 0 — substituído pelo Room ao inserir (0 sinaliza "ainda não inserido").
     */
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Data e hora da sincronização no formato ISO 8601.
     * Exemplo: "2026-05-07T14:32:00"
     * Gerado com SimpleDateFormat no repositório (API 24+ compatível).
     */
    val timestamp: String,

    /**
     * Resultado da sincronização:
     *  "SUCCESS" = dados do SAP baixados e gravados no banco com sucesso
     *  "ERROR"   = falha na chamada OData, na autenticação ou na gravação local
     */
    val status: String,

    /**
     * Quantidade de registros inseridos ou substituídos na tabela menu_app.
     * Valor 0 quando status = "ERROR" (nenhum dado foi gravado).
     */
    val registrosAtualizados: Int,

    /**
     * Mensagem descritiva do erro capturado pela exceção.
     * null quando status = "SUCCESS" — não há erro para registrar.
     */
    val mensagemErro: String?
)
