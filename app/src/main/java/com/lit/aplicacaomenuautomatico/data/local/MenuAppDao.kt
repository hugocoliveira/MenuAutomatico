package com.lit.aplicacaomenuautomatico.data.local

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Room (acesso ao banco SQLite)
// ─────────────────────────────────────────────────────────────────────────────
import androidx.room.Dao              // marca a interface como Data Access Object do Room
import androidx.room.Insert           // anotação para operação de inserção
import androidx.room.OnConflictStrategy // define comportamento ao inserir registro com chave duplicada
import androidx.room.Query            // anotação para SQL personalizado

// ─────────────────────────────────────────────────────────────────────────────
// IMPORT — Corrotinas / Flow
// ─────────────────────────────────────────────────────────────────────────────
import kotlinx.coroutines.flow.Flow   // stream reativo — emite nova lista quando os dados mudam no banco

// ═════════════════════════════════════════════════════════════════════════════
// DAO: MenuAppDao
// ═════════════════════════════════════════════════════════════════════════════

/**
 * DAO (Data Access Object) para a tabela menu_app.
 *
 * Interface gerada em tempo de compilação pelo Room — nunca instanciada diretamente,
 * sempre injetada via Hilt (provida pelo DatabaseModule).
 *
 * Convenções de tipo de retorno:
 *  - [Flow] → observação reativa; a UI recebe atualizações automáticas após sync em background
 *  - suspend → função suspendente; deve ser chamada em corrotina (Dispatchers.IO)
 */
@Dao
interface MenuAppDao {

    /**
     * Busca todos os itens de um grupo de menu, ordenados numericamente pela sequence.
     *
     * Retorna [Flow] para que a UI reaja automaticamente a qualquer mudança no banco
     * (ex.: após sincronização em background pelo WorkManager).
     * A query ordena por sequence ASC — funciona corretamente pois sequence é Int.
     *
     * @param mmenu Código do grupo de menu (ex.: "MAIN", "INB00", "GR00")
     * @return Flow que emite nova lista sempre que os dados do grupo mudarem
     */
    @Query("SELECT * FROM menu_app WHERE mmenu = :mmenu ORDER BY sequence ASC")
    fun getItensPorMenu(mmenu: String): Flow<List<MenuAppEntity>>

    /**
     * Insere ou substitui uma lista de itens na tabela.
     *
     * [OnConflictStrategy.REPLACE] garante que sincronizações subsequentes
     * atualizem registros existentes (mesma chave primária) em vez de falhar.
     * Equivale a DELETE + INSERT na chave conflitante.
     *
     * @param itens Lista de entidades a inserir ou substituir
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirTodos(itens: List<MenuAppEntity>)

    /**
     * Remove todos os registros da tabela menu_app.
     *
     * Chamado antes de cada sincronização completa do SAP para garantir
     * que dados obsoletos (itens removidos do SAP) não permaneçam no banco.
     */
    @Query("DELETE FROM menu_app")
    suspend fun deletarTodos()

    /**
     * Retorna a quantidade total de registros na tabela.
     *
     * Usado para detectar o primeiro acesso ao app:
     *  - 0 registros → banco nunca foi sincronizado → rede obrigatória no login
     *  - > 0 registros → há dados em cache → modo offline disponível
     */
    @Query("SELECT COUNT(*) FROM menu_app")
    suspend fun contarItens(): Int
}
