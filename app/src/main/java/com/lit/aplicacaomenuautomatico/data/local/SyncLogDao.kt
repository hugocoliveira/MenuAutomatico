package com.lit.aplicacaomenuautomatico.data.local

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Room
// ─────────────────────────────────────────────────────────────────────────────
import androidx.room.Dao    // marca a interface como Data Access Object do Room
import androidx.room.Insert // anotação para operação de inserção (sem OnConflict — id é auto-gerado)
import androidx.room.Query  // anotação para SQL personalizado

// ─────────────────────────────────────────────────────────────────────────────
// IMPORT — Corrotinas / Flow
// ─────────────────────────────────────────────────────────────────────────────
import kotlinx.coroutines.flow.Flow // stream reativo — permite observação da lista de logs pela UI

// ═════════════════════════════════════════════════════════════════════════════
// DAO: SyncLogDao
// ═════════════════════════════════════════════════════════════════════════════

/**
 * DAO para a tabela sync_log.
 *
 * Registra o histórico de sincronizações com o SAP para auditoria e diagnóstico.
 * Interface gerada em tempo de compilação pelo Room, injetada via Hilt.
 *
 * Cada sincronização (manual via login ou automática via WorkManager) grava
 * um registro aqui, independente de sucesso ou falha.
 */
@Dao
interface SyncLogDao {

    /**
     * Insere um novo registro de log na tabela sync_log.
     *
     * Não usa OnConflict porque a chave primária é auto-gerada — não há conflito possível.
     * Operação suspendente — deve ser chamada em corrotina (Dispatchers.IO).
     *
     * @param log Entidade com os dados da sincronização (timestamp, status, contagem, erro)
     */
    @Insert
    suspend fun inserir(log: SyncLogEntity)

    /**
     * Retorna os 50 registros de log mais recentes em ordem decrescente de ID.
     *
     * Retorna [Flow] para observação reativa — útil se uma tela de histórico
     * for adicionada futuramente. O limite de 50 evita carregar o histórico completo.
     *
     * @return Flow que emite a lista de logs mais recentes sempre que houver inserção
     */
    @Query("SELECT * FROM sync_log ORDER BY id DESC LIMIT 50")
    fun getLogs(): Flow<List<SyncLogEntity>>
}
