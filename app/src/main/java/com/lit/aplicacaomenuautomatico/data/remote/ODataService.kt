package com.lit.aplicacaomenuautomatico.data.remote

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * Interface Retrofit para o serviço OData SAP.
 * A URL base (http://vm77.4hub.cloud:57700/) é configurada no [NetworkModule].
 *
 * Autenticação via Basic Auth: o header Authorization é montado pelo repositório
 * no formato "Basic " + Base64("usuario:senha") e passado como parâmetro.
 */
interface ODataService {

    /**
     * Busca todos os itens de menu do depósito autenticado.
     * O SAP filtra automaticamente os menus conforme as permissões do usuário —
     * não é necessário filtrar por Lgnum na query, o backend já faz isso.
     *
     * @param sapClient Mandante SAP (padrão: "050")
     * @param authorization Header Basic Auth no formato "Basic <base64>"
     * @return Envelope OData com a lista de itens de menu
     */
    @GET("sap/opu/odata/sap/zlit_menu_app_ui/MenuApp")
    suspend fun getMenuApp(
        @Query("sap-client") sapClient: String = "050",
        /** Força resposta em JSON — SAP OData retorna XML por padrão sem este parâmetro */
        @Query("\$format") format: String = "json",
        /** Idioma dos textos retornados — sem este parâmetro o SAP usa inglês como padrão */
        @Query("sap-language") sapLanguage: String = "PT",
        @Header("Authorization") authorization: String
    ): ODataEnvelope
}
