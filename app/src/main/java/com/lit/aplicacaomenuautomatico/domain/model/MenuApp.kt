package com.lit.aplicacaomenuautomatico.domain.model

// Sem imports externos — modelo de domínio puro, sem dependências de framework.
// Isso garante que a camada de UI nunca precise importar Room, Gson ou Retrofit.

// ═════════════════════════════════════════════════════════════════════════════
// MODELO DE DOMÍNIO: MenuApp
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Modelo de domínio de um item de menu do SAP WM/EWM.
 * Desacoplado de qualquer framework — sem anotações Room ou Gson.
 * Representa os dados de negócio puros que a UI e os UseCases manipulam.
 *
 * Criado a partir de [MenuAppEntity] pelo mapper em MenuRepository.
 * A UI nunca acessa entidades Room diretamente — sempre usa este modelo.
 */
data class MenuApp(
    /** Número do depósito SAP (ex.: "001") */
    val lgnum: String,

    /** Código do grupo de menu ao qual este item pertence (ex.: "MAIN", "INB00") */
    val mmenu: String,

    /** Posição numérica do item dentro do grupo — já convertida de String para Int no repositório */
    val sequence: Int,

    /**
     * Package ID do app externo a lançar (ex.: "com.entrada.fornecimento").
     * Relevante apenas quando [isAcao] = true (type = "2").
     * Ignorado em itens de submenu (type = "1").
     */
    val componente: String,

    /**
     * Tipo do item — define o comportamento ao tocar:
     *  "1" = exibir submenu filho (navegar para o próximo nível)
     *  "2" = lançar app externo via Intent explícita
     */
    val type: String,

    /**
     * Código de destino do item:
     *  - type = "1": código do submenu filho (usado como mmenu na próxima tela)
     *  - type = "2": código de transação SAP passado como extra para o app externo
     */
    val transacao: String,

    /** Texto longo — título principal exibido no card do menu (campo Text do OData) */
    val text: String,

    /** Texto curto — subtítulo, título da TopAppBar ao abrir submenu, ou fallback quando text vazio */
    val sText: String

) {
    // ─── PROPRIEDADES COMPUTADAS ──────────────────────────────────────────────
    // Encapsulam a lógica de tipo para que a UI não precise comparar strings "1"/"2" diretamente.

    /** True quando este item deve abrir um submenu filho ao ser tocado (type = "1") */
    val isSubmenu: Boolean get() = type == "1"

    /** True quando este item deve lançar um app externo ao ser tocado (type = "2") */
    val isAcao: Boolean get() = type == "2"
}
