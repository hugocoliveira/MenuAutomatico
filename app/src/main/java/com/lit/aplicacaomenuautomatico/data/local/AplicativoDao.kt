package com.lit.aplicacaomenuautomatico.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO para operações na tabela [AplicativoEntity] (aplicativos).
 * Usado pelo repositório para manter atualizada a lista de apps externos
 * referenciados no menu SAP após cada sincronização.
 */
@Dao
interface AplicativoDao {

    /**
     * Insere uma lista de aplicativos.
     * [OnConflictStrategy.REPLACE] atualiza caso o componente já exista (index único).
     *
     * @param aplicativos Lista de entidades a inserir
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirTodos(aplicativos: List<AplicativoEntity>)

    /**
     * Remove todos os registros da tabela.
     * Chamado antes de inserir dados frescos do SAP para evitar registros obsoletos.
     */
    @Query("DELETE FROM aplicativos")
    suspend fun deletarTodos()

    /**
     * Retorna todos os aplicativos cadastrados.
     * Usado pelo ViewModel para verificar instalação e atualizações após login.
     *
     * @return Lista de entidades com id e componente (package ID)
     */
    @Query("SELECT * FROM aplicativos")
    suspend fun getAll(): List<AplicativoEntity>
}
