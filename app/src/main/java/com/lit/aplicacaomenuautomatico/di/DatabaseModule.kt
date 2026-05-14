package com.lit.aplicacaomenuautomatico.di

import android.content.Context
import androidx.room.Room
import com.lit.aplicacaomenuautomatico.data.local.AppDatabase
import com.lit.aplicacaomenuautomatico.data.local.MenuAppDao
import com.lit.aplicacaomenuautomatico.data.local.SyncLogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo Hilt responsável por prover as dependências relacionadas ao banco de dados Room.
 * Instalado no [SingletonComponent] para garantir uma única instância durante toda a vida do app.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Cria e provê a instância singleton do banco de dados Room.
     * [fallbackToDestructiveMigration] é usado durante desenvolvimento (versão 1) —
     * ao incrementar a versão do banco, substituir por migrações explícitas.
     *
     * @param context Contexto da aplicação — injetado pelo Hilt
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "menu_automatico.db"
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    /**
     * Provê o DAO de itens de menu a partir da instância do banco.
     *
     * @param db Instância singleton do AppDatabase
     */
    @Provides
    @Singleton
    fun provideMenuAppDao(db: AppDatabase): MenuAppDao = db.menuAppDao()

    /**
     * Provê o DAO de logs de sincronização a partir da instância do banco.
     *
     * @param db Instância singleton do AppDatabase
     */
    @Provides
    @Singleton
    fun provideSyncLogDao(db: AppDatabase): SyncLogDao = db.syncLogDao()
}
