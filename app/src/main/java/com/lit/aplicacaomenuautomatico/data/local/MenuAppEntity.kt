package com.lit.aplicacaomenuautomatico.data.local

import androidx.room.Entity

/**
 * Entidade Room que representa um item de menu armazenado localmente no SQLite.
 * Chave primária composta por [lgnum] + [mmenu] + [sequence], espelhando a
 * chave do EntitySet OData no SAP.
 *
 * O campo [sequence] é armazenado como [Int] (e não String como vem do SAP)
 * para garantir ordenação numérica correta (1, 2, 4, 10 e não 1, 10, 2, 4).
 * A conversão String → Int ocorre no mapper do repositório.
 */
@Entity(
    tableName = "menu_app",
    primaryKeys = ["lgnum", "mmenu", "sequence"]
)
data class MenuAppEntity(
    /** Número do depósito SAP (ex.: "001") */
    val lgnum: String,
    /** Código do grupo de menu ao qual este item pertence (ex.: "MAIN", "INB00") */
    val mmenu: String,
    /** Posição do item dentro do grupo — armazenado como Int para ordenação correta */
    val sequence: Int,
    /** Package ID do app externo a lançar (relevante apenas quando type = "2") */
    val componente: String,
    /** "1" = submenu filho | "2" = lançar app externo */
    val type: String,
    /** Código do submenu destino (type=1) ou da transação SAP (type=2) */
    val transacao: String,
    /** Texto longo exibido como título do item */
    val text: String,
    /** Texto curto — usado em layouts compactos ou como subtítulo */
    val sText: String
)
