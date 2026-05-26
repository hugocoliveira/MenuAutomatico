package com.lit.aplicacaomenuautomatico.data.local

// ─────────────────────────────────────────────────────────────────────────────
// IMPORT — Room
// ─────────────────────────────────────────────────────────────────────────────
import androidx.room.Entity // anotação que mapeia esta data class para uma tabela SQLite no Room

// ═════════════════════════════════════════════════════════════════════════════
// ENTIDADE ROOM: menu_app
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Entidade Room que representa um item de menu armazenado localmente no SQLite.
 *
 * Tabela: menu_app
 * Chave primária composta por [lgnum] + [mmenu] + [sequence], espelhando
 * a chave do EntitySet OData no SAP — garante unicidade sem ID artificial.
 *
 * Observação importante sobre [sequence]:
 * O SAP envia este campo como String ("1", "10", "4").
 * Armazená-lo como Int garante ordenação numérica correta (1, 4, 10) em vez
 * de lexicográfica (1, 10, 4). A conversão String → Int ocorre no mapper
 * do repositório (MenuRepository.toEntity()).
 *
 * Esta entidade nunca é usada diretamente pela UI — é convertida para [MenuApp]
 * (modelo de domínio) pelo repositório antes de chegar ao ViewModel.
 */
@Entity(
    tableName  = "menu_app",                          // nome da tabela no arquivo SQLite
    primaryKeys = ["lgnum", "mmenu", "sequence"]      // chave composta — combinação única por item
)
data class MenuAppEntity(
    /** Número do depósito SAP (ex.: "001") — parte da chave primária */
    val lgnum: String,

    /** Código do grupo de menu ao qual este item pertence (ex.: "MAIN", "INB00") — parte da chave */
    val mmenu: String,

    /**
     * Posição do item dentro do grupo de menu.
     * Armazenado como Int (e não String como vem do SAP) para ordenação numérica correta.
     * Parte da chave primária.
     */
    val sequence: Int,

    /**
     * Package ID do app externo a ser lançado (ex.: "com.entrada.fornecimento").
     * Relevante apenas quando type = "2". Ignorado em itens de submenu (type = "1").
     */
    val componente: String,

    /**
     * Tipo do item de menu:
     *  "1" = submenu — ao tocar, exibe filhos onde mmenu = transacao deste item
     *  "2" = ação    — ao tocar, lança app externo via Intent usando o package em [componente]
     */
    val type: String,

    /** Código de destino: submenu filho (type=1) ou transação SAP passada ao app externo (type=2) */
    val transacao: String,

    /** Texto longo — título principal do item, exibido em destaque no card */
    val text: String,

    /** Texto curto — subtítulo, título da TopAppBar ao entrar no submenu, ou fallback de [text] */
    val sText: String
)
