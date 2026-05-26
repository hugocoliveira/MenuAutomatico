package com.lit.aplicacaomenuautomatico.di

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Android / segurança
// ─────────────────────────────────────────────────────────────────────────────
import android.content.Context          // contexto da aplicação para criar a MasterKey e as prefs
import android.content.SharedPreferences // interface de acesso ao armazenamento de chave-valor
import androidx.security.crypto.EncryptedSharedPreferences // implementação criptografada das SharedPreferences
import androidx.security.crypto.MasterKey                  // chave mestra AES256-GCM no Android Keystore

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Rede (Retrofit + OkHttp)
// ─────────────────────────────────────────────────────────────────────────────
import com.lit.aplicacaomenuautomatico.data.remote.ODataService // interface do serviço OData SAP
import retrofit2.Retrofit                                        // cliente HTTP + parser de resposta
import retrofit2.converter.gson.GsonConverterFactory            // converte JSON em data classes Kotlin via Gson

import okhttp3.OkHttpClient                    // cliente HTTP de baixo nível (timeouts, interceptors)
import okhttp3.logging.HttpLoggingInterceptor  // loga chamadas HTTP no Logcat para diagnóstico

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Hilt (injeção de dependência)
// ─────────────────────────────────────────────────────────────────────────────
import dagger.Module                                      // marca a classe como módulo de injeção Hilt
import dagger.Provides                                    // marca um método como provedor de dependência
import dagger.hilt.InstallIn                              // define em qual componente Hilt este módulo é instalado
import dagger.hilt.android.qualifiers.ApplicationContext  // injeta o ApplicationContext (não o de Activity)
import dagger.hilt.components.SingletonComponent          // componente vivo durante toda a vida do app

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Java utilitários
// ─────────────────────────────────────────────────────────────────────────────
import java.util.concurrent.TimeUnit // unidade de tempo para os timeouts do OkHttp (SECONDS)
import javax.inject.Singleton        // garante que apenas uma instância seja criada (singleton)

// ═════════════════════════════════════════════════════════════════════════════
// MÓDULO HILT: NetworkModule
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Módulo Hilt responsável por prover as dependências de rede e segurança.
 *
 * Instalado no [SingletonComponent] — OkHttpClient, Retrofit, ODataService e
 * EncryptedSharedPreferences são singletons para evitar recriação desnecessária
 * de conexões e arquivos de preferências.
 *
 * Dependências providas:
 *  - [OkHttpClient]             → cliente HTTP com timeouts e logging
 *  - [Retrofit]                 → cliente HTTP de alto nível para o SAP OData
 *  - [ODataService]             → interface de acesso ao endpoint OData
 *  - [SharedPreferences]        → armazenamento seguro (criptografado) de credenciais SAP
 */
@Module
@InstallIn(SingletonComponent::class) // singleton: mesma instância durante toda a vida do processo
object NetworkModule {

    /** URL base do servidor SAP — todos os endpoints do ODataService são relativos a ela */
    private const val BASE_URL = "http://vm77.4hub.cloud:57700/"

    // ─── PROVEDOR: OkHttpClient ───────────────────────────────────────────────

    /**
     * Provê o cliente HTTP configurado com timeouts e interceptor de log.
     *
     * Timeouts de 30s para connect, read e write — adequado para o SAP em VPN.
     * [HttpLoggingInterceptor.Level.BASIC] loga apenas método, URL e código HTTP
     * (sem corpo da resposta) para evitar expor credenciais no Logcat.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)  // timeout para estabelecer a conexão TCP
            .readTimeout(30, TimeUnit.SECONDS)     // timeout para receber dados do servidor
            .writeTimeout(30, TimeUnit.SECONDS)    // timeout para enviar dados ao servidor
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    // BASIC: loga método + URL + código HTTP, sem corpo — não expõe senha no Logcat
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            .build()

    // ─── PROVEDOR: Retrofit ───────────────────────────────────────────────────

    /**
     * Provê a instância do Retrofit configurada para o endpoint SAP OData.
     *
     * [GsonConverterFactory] desserializa automaticamente o envelope OData
     * { "d": { "results": [] } } para as data classes [ODataEnvelope] e [MenuAppDto].
     *
     * @param okHttpClient Cliente HTTP singleton com timeouts e logging já configurados
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)                          // URL raiz do servidor SAP
            .client(okHttpClient)                       // usa o cliente configurado acima
            .addConverterFactory(GsonConverterFactory.create()) // JSON → data class via Gson
            .build()

    // ─── PROVEDOR: ODataService ───────────────────────────────────────────────

    /**
     * Provê a implementação gerada pelo Retrofit para a interface [ODataService].
     * Retrofit gera o código em tempo de compilação via reflection.
     *
     * @param retrofit Instância singleton do Retrofit já configurada
     */
    @Provides
    @Singleton
    fun provideODataService(retrofit: Retrofit): ODataService =
        retrofit.create(ODataService::class.java) // cria implementação dinâmica da interface

    // ─── PROVEDOR: EncryptedSharedPreferences ─────────────────────────────────

    /**
     * Provê o [EncryptedSharedPreferences] para armazenamento seguro das credenciais SAP.
     *
     * Criptografia em duas camadas:
     *  - Chaves criptografadas com AES256-SIV (determinístico — necessário para lookup por chave)
     *  - Valores criptografados com AES256-GCM (autenticado — integridade garantida)
     * A [MasterKey] fica no Android Keystore do dispositivo — nunca exposta em texto claro.
     *
     * A senha do operador SAP nunca é armazenada em texto claro no dispositivo.
     *
     * @param context Contexto da aplicação necessário para criar a MasterKey e o arquivo de prefs
     */
    @Provides
    @Singleton
    fun provideEncryptedSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        // MasterKey com AES256-GCM — armazenada no hardware Keystore do dispositivo
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM) // algoritmo de criptografia da chave mestra
            .build()

        return EncryptedSharedPreferences.create(
            context,
            "menu_automatico_secure_prefs",                             // nome do arquivo de prefs no disco
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,  // criptografia das chaves
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM  // criptografia dos valores
        )
    }
}
