package com.lit.aplicacaomenuautomatico.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) para a tabela menu_app.
 * Todas as operações de banco são suspendentes (corrotinas) exceto [getItensPorMenu],
 * que retorna um Flow para que a UI reaja automaticamente a mudanças nos dados
 * (ex.: após uma sincronização em background).
 */
@Dao
interface MenuAppDao {

    /**
     * Busca todos os itens de um grupo de menu, ordenados numericamente pela sequence.
     * Retorna um [Flow] que emite nova lista sempre que os dados mudarem no banco.
     *
     * @param mmenu Código do grupo de menu (ex.: "MAIN", "INB00", "GR00")
     */
    @Query("SELECT * FROM menu_app WHERE mmenu = :mmenu ORDER BY sequence ASC")
    fun getItensPorMenu(mmenu: String): Flow<List<MenuAppEntity>>

    /**
     * Insere ou substitui uma lista de itens na tabela.
     * Usa REPLACE para que sincronizações subsequentes atualizem registros existentes.
     *
     * @param itens Lista de entidades a inserir
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirTodos(itens: List<MenuAppEntity>)

    /**
     * Remove todos os registros da tabela menu_app.
     * Chamado antes de cada sincronização completa para garantir dados frescos do SAP.
     */
    @Query("DELETE FROM menu_app")
    suspend fun deletarTodos()

    /**
     * Retorna a quantidade de registros na tabela.
     * Usado para detectar primeiro acesso: banco vazio = nunca sincronizou = rede obrigatória.
     */
    @Query("SELECT COUNT(*) FROM menu_app")
    suspend fun contarItens(): Int
}
