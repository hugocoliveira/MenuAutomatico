package com.lit.aplicacaomenuautomatico.data.remote

import com.google.gson.annotations.SerializedName

/**
 * Modelos de resposta do serviço OData SAP.
 * A resposta segue o envelope padrão OData v2: { "d": { "results": [...] } }
 *
 * Exemplo de resposta real (docs/dadosModelo.json):
 * {
 *   "d": {
 *     "results": [
 *       { "Lgnum": "001", "Mmenu": "MAIN", "Sequence": "1", ... }
 *     ]
 *   }
 * }
 */

/** Envelope externo da resposta OData — contém o objeto "d" */
data class ODataEnvelope(
    @SerializedName("d") val d: ODataResults
)

/** Objeto interno "d" — contém a lista de resultados */
data class ODataResults(
    @SerializedName("results") val results: List<MenuAppDto>
)

/**
 * DTO (Data Transfer Object) de cada item de menu retornado pelo SAP.
 * Campos com inicial maiúscula seguem a convenção de nomenclatura do SAP OData.
 * O campo [sequence] vem como String do SAP e é convertido para Int no repositório.
 *
 * Campos [deleteMc] e [updateMc] são flags de permissão do SAP — não utilizados
 * pelo app no MVP mas mantidos para fidelidade ao contrato da API.
 */
data class MenuAppDto(
    @SerializedName("Lgnum")      val lgnum: String,
    @SerializedName("Mmenu")      val mmenu: String,
    /** Sequence vem como String do SAP ("1", "4", "10") — converter para Int no mapper */
    @SerializedName("Sequence")   val sequence: String,
    @SerializedName("Componente") val componente: String,
    /** "1" = exibir submenu filho | "2" = lançar app externo */
    @SerializedName("Type")       val type: String,
    @SerializedName("Transacao")  val transacao: String,
    @SerializedName("Text")       val text: String,
    @SerializedName("SText")      val sText: String,
    @SerializedName("Delete_mc")  val deleteMc: Boolean = false,
    @SerializedName("Update_mc")  val updateMc: Boolean = false
)
