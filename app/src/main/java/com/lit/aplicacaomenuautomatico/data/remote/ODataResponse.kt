package com.lit.aplicacaomenuautomatico.data.remote

// ─────────────────────────────────────────────────────────────────────────────
// IMPORT — Gson
// ─────────────────────────────────────────────────────────────────────────────
import com.google.gson.annotations.SerializedName // mapeia campo JSON com nome diferente do campo Kotlin

// ═════════════════════════════════════════════════════════════════════════════
// MODELOS DE RESPOSTA OData SAP
//
// O SAP retorna respostas no formato OData v2 com envelope padrão:
//   { "d": { "results": [ { "Lgnum": "001", "Mmenu": "MAIN", ... } ] } }
//
// Três data classes representam as três camadas do envelope:
//   ODataEnvelope → ODataResults → List<MenuAppDto>
//
// Referência de dados reais: docs/dadosModelo.json
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Envelope externo da resposta OData.
 * Nível raiz do JSON — contém apenas o objeto "d" obrigatório no protocolo OData v2.
 */
data class ODataEnvelope(
    @SerializedName("d") val d: ODataResults // "d" é o nome do campo no JSON (convenção OData v2)
)

/**
 * Objeto interno "d" da resposta OData.
 * Contém a lista de resultados em "results" — array com os itens de menu do SAP.
 */
data class ODataResults(
    @SerializedName("results") val results: List<MenuAppDto> // array de itens do menu
)

/**
 * DTO (Data Transfer Object) de cada item de menu retornado pelo SAP OData.
 *
 * Convenções do SAP:
 *  - Campos com inicial maiúscula (ex.: "Lgnum", "Mmenu") — padrão OData SAP
 *  - [sequence] vem como String do SAP ("1", "4", "10") — convertido para Int no repositório
 *  - [deleteMc] e [updateMc] são flags de permissão do SAP (não utilizados pelo app no MVP)
 *
 * Não é usado diretamente pela UI — convertido para [MenuAppEntity] pelo mapper
 * MenuRepository.toEntity() antes de ser gravado no SQLite.
 */
data class MenuAppDto(
    @SerializedName("Lgnum")      val lgnum: String,      // número do depósito (ex.: "001")
    @SerializedName("Mmenu")      val mmenu: String,      // código do grupo de menu (ex.: "MAIN")

    /**
     * Posição do item dentro do grupo — vem como String do SAP ("1", "10", "4").
     * Convertido para Int no mapper do repositório para ordenação numérica correta.
     */
    @SerializedName("Sequence")   val sequence: String,

    @SerializedName("Componente") val componente: String, // package ID do app externo (type=2) ou vazio (type=1)

    /**
     * Tipo do item:
     *  "1" = submenu — navega para filhos onde mmenu = Transacao deste item
     *  "2" = ação    — lança app externo via Intent usando [componente] como package ID
     */
    @SerializedName("Type")       val type: String,

    @SerializedName("Transacao")  val transacao: String,  // submenu destino (type=1) ou código SAP (type=2)
    @SerializedName("Text")       val text: String,       // texto longo — título principal do item
    @SerializedName("SText")      val sText: String,      // texto curto — subtítulo ou título da TopAppBar

    // Flags de permissão do SAP — não utilizadas no MVP, mantidas para fidelidade ao contrato da API
    @SerializedName("Delete_mc")  val deleteMc: Boolean = false, // permissão de exclusão no SAP
    @SerializedName("Update_mc")  val updateMc: Boolean = false  // permissão de atualização no SAP
)
