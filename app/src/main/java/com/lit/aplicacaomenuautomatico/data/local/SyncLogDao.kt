package com.lit.aplicacaomenuautomatico.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO para a tabela sync_log.
 * Registra o histórico de sincronizações com o SAP para auditoria e diagnóstico.
 */
@Dao
interface SyncLogDao {

    /**
     * Insere um novo registro de log de sincronização.
     *
     * @param log Entidade com os dados da sincronização (timestamp, status, contagem, erro)
     */
    @Insert
    suspend fun inserir(log: SyncLogEntity)

    /**
     * Retorna os 50 registros de log mais recentes, em ordem decrescente.
     * Retorna um Flow para observação reativa (útil se exibir logs na UI futuramente).
     */
    @Query("SELECT * FROM sync_log ORDER BY id DESC LIMIT 50")
    fun getLogs(): Flow<List<SyncLogEntity>>
}
