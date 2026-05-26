package com.lit.aplicacaomenuautomatico.data.local

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Room
// ─────────────────────────────────────────────────────────────────────────────
import androidx.room.Entity     // anotação que mapeia esta data class para uma tabela SQLite
import androidx.room.Index      // define índice na tabela (aqui: índice único em "componente")
import androidx.room.PrimaryKey // define a chave primária da tabela (auto-gerada aqui)

// ═════════════════════════════════════════════════════════════════════════════
// ENTIDADE ROOM: aplicativos
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Entidade Room que representa um aplicativo externo referenciado no menu SAP.
 *
 * Tabela: aplicativos
 * Populada automaticamente durante cada sincronização com o SAP OData —
 * contém os valores únicos do campo Componente dos itens de menu com Type = "2".
 *
 * O campo [componente] é o package ID do app externo (ex.: "com.entrada.fornecimento").
 * O índice único garante que cada package ID apareça apenas uma vez na tabela,
 * evitando duplicatas mesmo que o OData retorne o mesmo app em múltiplos itens de menu.
 *
 * Usada pelo LoginViewModel para obter a lista de apps a verificar (instalação/atualização)
 * após o login bem-sucedido.
 */
@Entity(
    tableName = "aplicativos",                                  // nome da tabela no SQLite
    indices   = [Index(value = ["componente"], unique = true)]  // impede duplicatas de package ID
)
data class AplicativoEntity(

    /**
     * ID local gerado automaticamente pelo Room.
     * Não tem significado de negócio — serve apenas como chave primária técnica.
     * Valor padrão 0 — substituído pelo Room ao inserir.
     */
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    /** Package ID do app externo (ex.: "com.entrada.fornecimento", "com.entrada.transporte") */
    val componente: String
)
