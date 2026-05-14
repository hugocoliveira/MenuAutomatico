package com.lit.aplicacaomenuautomatico.di

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.lit.aplicacaomenuautomatico.data.remote.ODataService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Módulo Hilt responsável por prover as dependências de rede e segurança.
 * Instalado no [SingletonComponent] — OkHttpClient, Retrofit e SharedPreferences
 * são singletons para evitar recriação desnecessária de conexões.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /** URL base do servidor SAP — todos os endpoints são relativos a ela */
    private const val BASE_URL = "http://vm77.4hub.cloud:57700/"
//http://vm77.4hub.cloud:57700/sap/opu/odata/sap/zlit_menu_app_ui/MenuApp?sap-client=050
    /**
     * Provê o cliente HTTP configurado com timeout de 30 segundos.
     * O interceptor de log facilita o diagnóstico de chamadas ao SAP em desenvolvimento.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    // BASIC loga apenas método, URL e código de resposta (sem corpo)
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            .build()

    /**
     * Provê a instância do Retrofit configurada para o endpoint SAP OData.
     * Usa GsonConverterFactory para deserializar o envelope OData { "d": { "results": [] } }.
     *
     * @param okHttpClient Cliente HTTP configurado com timeouts e logging
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    /**
     * Provê a interface de serviço OData gerada pelo Retrofit.
     *
     * @param retrofit Instância configurada do Retrofit
     */
    @Provides
    @Singleton
    fun provideODataService(retrofit: Retrofit): ODataService =
        retrofit.create(ODataService::class.java)

    /**
     * Provê o EncryptedSharedPreferences para armazenamento seguro de credenciais SAP.
     * Usa AES256-GCM para criptografia de chaves e valores — a senha nunca é armazenada
     * em texto claro no dispositivo.
     *
     * @param context Contexto da aplicação para criação da MasterKey e do arquivo de prefs
     */
    @Provides
    @Singleton
    fun provideEncryptedSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        // MasterKey usa AES256-GCM armazenada no Android Keystore do dispositivo
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            "menu_automatico_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
