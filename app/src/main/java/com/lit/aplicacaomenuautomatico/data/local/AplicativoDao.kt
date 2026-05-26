package com.lit.aplicacaomenuautomatico.data.local

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Room
// ─────────────────────────────────────────────────────────────────────────────
import androidx.room.Dao              // marca a interface como Data Access Object do Room
import androidx.room.Insert           // anotação para operação de inserção
import androidx.room.OnConflictStrategy // estratégia ao inserir registro com índice único duplicado
import androidx.room.Query            // anotação para SQL personalizado

// ═════════════════════════════════════════════════════════════════════════════
// DAO: AplicativoDao
// ═════════════════════════════════════════════════════════════════════════════

/**
 * DAO para operações na tabela [AplicativoEntity] (aplicativos).
 *
 * Interface gerada em tempo de compilação pelo Room, injetada via Hilt.
 * Mantém atualizada a lista de apps externos referenciados no menu SAP
 * após cada sincronização com o OData.
 *
 * Usada pelo MenuRepository para popular a tabela durante o sync e pelo
 * LoginViewModel para obter os package IDs a verificar.
 */
@Dao
interface AplicativoDao {

    /**
     * Insere uma lista de aplicativos na tabela.
     *
     * [OnConflictStrategy.REPLACE] garante que se um package ID já existir
     * (violaria o índice único), o registro antigo é substituído pelo novo.
     * Operação suspendente — deve ser chamada em corrotina (Dispatchers.IO).
     *
     * @param aplicativos Lista de entidades com package IDs a inserir
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirTodos(aplicativos: List<AplicativoEntity>)

    /**
     * Remove todos os registros da tabela aplicativos.
     *
     * Chamado antes de inserir dados frescos do SAP para evitar registros
     * obsoletos de apps que foram removidos do menu no SAP.
     * Operação suspendente — deve ser chamada em corrotina (Dispatchers.IO).
     */
    @Query("DELETE FROM aplicativos")
    suspend fun deletarTodos()

    /**
     * Retorna todos os aplicativos cadastrados na tabela.
     *
     * Usado pelo [MenuRepository.getAplicativos] para fornecer ao LoginViewModel
     * a lista de package IDs que precisam ser verificados (instalação/atualização).
     * Operação suspendente — deve ser chamada em corrotina (Dispatchers.IO).
     *
     * @return Lista de todas as entidades com id e componente (package ID)
     */
    @Query("SELECT * FROM aplicativos")
    suspend fun getAll(): List<AplicativoEntity>
}
