package com.lit.aplicacaomenuautomatico.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidade Room que representa um aplicativo externo referenciado no menu SAP.
 * Populada automaticamente durante a sincronização com o SAP OData — contém
 * os valores únicos do campo Componente dos itens de menu com Type="2".
 *
 * O campo [componente] é o package ID do app externo (ex.: "com.entrada.fornecimento").
 * A constraint de unicidade garante que cada app apareça apenas uma vez na tabela.
 */
@Entity(
    tableName = "aplicativos",
    indices = [Index(value = ["componente"], unique = true)]
)
data class AplicativoEntity(
    /** ID local gerado automaticamente — sem significado de negócio */
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    /** Package ID do app externo (ex.: "com.entrada.fornecimento") */
    val componente: String
)
