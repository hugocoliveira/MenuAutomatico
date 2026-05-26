package com.lit.aplicacaomenuautomatico.data.remote

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Retrofit (definição da interface de serviço HTTP)
// ─────────────────────────────────────────────────────────────────────────────
import retrofit2.http.GET    // anotação para requisição HTTP GET
import retrofit2.http.Header // anotação para parâmetro de cabeçalho HTTP (ex.: Authorization)
import retrofit2.http.Query  // anotação para parâmetro de query string na URL (ex.: ?sap-client=050)

// ═════════════════════════════════════════════════════════════════════════════
// INTERFACE DE SERVIÇO: ODataService
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Interface Retrofit para o serviço OData do SAP.
 *
 * URL base configurada no [NetworkModule]: http://vm77.4hub.cloud:57700/
 * URL completa do endpoint: http://vm77.4hub.cloud:57700/sap/opu/odata/sap/zlit_menu_app_ui/MenuApp
 *
 * Autenticação: Basic Auth via cabeçalho Authorization.
 * O header é montado pelo repositório no formato: "Basic " + Base64("usuario:senha")
 * e passado como parâmetro [authorization] para cada chamada.
 *
 * O Retrofit gera automaticamente a implementação desta interface em tempo de compilação.
 * Injetada via Hilt pelo [NetworkModule].
 */
interface ODataService {

    /**
     * Busca todos os itens de menu do depósito do usuário autenticado.
     *
     * O SAP filtra automaticamente os menus conforme as permissões do usuário —
     * não é necessário filtrar por Lgnum na query; o backend SAP já faz isso.
     *
     * Parâmetros de query enviados na URL:
     *  ?sap-client=050&$format=json&sap-language=PT
     *
     * @param sapClient    Mandante SAP — "050" é o padrão desta instalação
     * @param format       Força resposta em JSON; sem este parâmetro o SAP retorna XML por padrão
     * @param sapLanguage  Idioma dos textos (Text/SText); sem este o SAP usa inglês como fallback
     * @param authorization Cabeçalho Basic Auth no formato "Basic <base64(user:pass)>"
     * @return Envelope OData com a lista de itens de menu (d.results[])
     */
    @GET("sap/opu/odata/sap/zlit_menu_app_ui/MenuApp")
    suspend fun getMenuApp(
        @Query("sap-client")   sapClient: String = "050",  // mandante SAP desta instalação
        @Query("\$format")     format: String = "json",     // força JSON (SAP default é XML)
        @Query("sap-language") sapLanguage: String = "PT",  // retorna Text/SText em português
        @Header("Authorization") authorization: String      // Basic Auth: "Basic <base64>"
    ): ODataEnvelope
}
