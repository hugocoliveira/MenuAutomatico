package com.lit.aplicacaomenuautomatico.domain.model

/**
 * Modelo de domínio de um item de menu.
 * Desacoplado de qualquer framework (sem anotações Room ou Gson),
 * representa os dados de negócio puros que a UI e os UseCases manipulam.
 */
data class MenuApp(
    val lgnum: String,
    val mmenu: String,
    /** Posição numérica do item dentro do grupo de menu */
    val sequence: Int,
    /** Package ID do app externo — relevante apenas quando [isAcao] = true */
    val componente: String,
    val type: String,
    /** Submenu destino (quando [isSubmenu]) ou código de transação SAP (quando [isAcao]) */
    val transacao: String,
    /** Texto longo — título principal exibido no card do menu */
    val text: String,
    /** Texto curto — subtítulo ou texto alternativo em layouts compactos */
    val sText: String
) {
    /** True quando este item abre um submenu filho ao ser tocado */
    val isSubmenu: Boolean get() = type == "1"

    /** True quando este item lança um app externo ao ser tocado */
    val isAcao: Boolean get() = type == "2"
}
