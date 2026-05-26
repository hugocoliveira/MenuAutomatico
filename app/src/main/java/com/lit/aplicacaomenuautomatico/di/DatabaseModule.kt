package com.lit.aplicacaomenuautomatico.di

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Android / Room
// ─────────────────────────────────────────────────────────────────────────────
import android.content.Context                                       // contexto para localizar o arquivo do banco
import androidx.room.Room                                            // builder de instâncias Room
import com.lit.aplicacaomenuautomatico.data.local.AppDatabase        // classe abstrata do banco de dados
import com.lit.aplicacaomenuautomatico.data.local.AplicativoDao      // DAO de apps externos
import com.lit.aplicacaomenuautomatico.data.local.MenuAppDao         // DAO de itens de menu
import com.lit.aplicacaomenuautomatico.data.local.SyncLogDao         // DAO de logs de sincronização

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Hilt (injeção de dependência)
// ─────────────────────────────────────────────────────────────────────────────
import dagger.Module                                      // marca a classe como módulo de injeção Hilt
import dagger.Provides                                    // marca um método como provedor de dependência
import dagger.hilt.InstallIn                              // define em qual componente Hilt este módulo é instalado
import dagger.hilt.android.qualifiers.ApplicationContext  // injeta o ApplicationContext (não o de Activity)
import dagger.hilt.components.SingletonComponent          // componente vivo durante toda a vida do processo

// ─────────────────────────────────────────────────────────────────────────────
// IMPORT — Singleton
// ─────────────────────────────────────────────────────────────────────────────
import javax.inject.Singleton // garante que apenas uma instância seja criada pelo contêiner Hilt

// ═════════════════════════════════════════════════════════════════════════════
// MÓDULO HILT: DatabaseModule
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Módulo Hilt responsável por prover as dependências do banco de dados Room.
 *
 * Instalado no [SingletonComponent] para garantir uma única instância do banco
 * durante toda a vida do processo do app — evita abrir e fechar o arquivo SQLite
 * a cada injeção.
 *
 * Dependências providas:
 *  - [AppDatabase]   → instância singleton do banco Room
 *  - [MenuAppDao]    → DAO para a tabela menu_app
 *  - [SyncLogDao]    → DAO para a tabela sync_log
 *  - [AplicativoDao] → DAO para a tabela aplicativos
 */
@Module
@InstallIn(SingletonComponent::class) // singleton: mesma instância enquanto o processo viver
object DatabaseModule {

    // ─── PROVEDOR: AppDatabase ────────────────────────────────────────────────

    /**
     * Cria e provê a instância singleton do banco de dados Room.
     *
     * Nome do arquivo SQLite: "menu_automatico.db" (em /data/data/<package>/databases/)
     *
     * [fallbackToDestructiveMigration] é usado durante o desenvolvimento:
     * ao incrementar a versão do schema sem fornecer uma Migration, o Room
     * descarta e recria o banco. Em produção, substituir por migrações explícitas
     * para não perder os dados dos usuários.
     *
     * @param context Contexto da aplicação injetado pelo Hilt
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,        // classe abstrata do banco
            "menu_automatico.db"            // nome do arquivo SQLite no dispositivo
        )
            .fallbackToDestructiveMigration(dropAllTables = true) // dev only: recria ao mudar versão
            .build()

    // ─── PROVEDORES DE DAOs ───────────────────────────────────────────────────
    // Cada DAO é obtido diretamente do AppDatabase singleton.
    // Hilt gerencia a ordem de criação: AppDatabase é criado antes dos DAOs.

    /**
     * Provê o DAO para operações na tabela menu_app.
     * @param db Instância singleton do AppDatabase
     */
    @Provides
    @Singleton
    fun provideMenuAppDao(db: AppDatabase): MenuAppDao = db.menuAppDao()

    /**
     * Provê o DAO para operações na tabela sync_log.
     * @param db Instância singleton do AppDatabase
     */
    @Provides
    @Singleton
    fun provideSyncLogDao(db: AppDatabase): SyncLogDao = db.syncLogDao()

    /**
     * Provê o DAO para operações na tabela aplicativos.
     * @param db Instância singleton do AppDatabase
     */
    @Provides
    @Singleton
    fun provideAplicativoDao(db: AppDatabase): AplicativoDao = db.aplicativoDao()
}
