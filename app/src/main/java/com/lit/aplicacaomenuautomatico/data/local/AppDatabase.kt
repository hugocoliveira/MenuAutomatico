package com.lit.aplicacaomenuautomatico.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Banco de dados Room do aplicativo.
 * Contém três tabelas:
 *  - [MenuAppEntity] (menu_app): itens de menu carregados do SAP
 *  - [SyncLogEntity] (sync_log): histórico de sincronizações
 *  - [AplicativoEntity] (aplicativos): apps externos identificados no menu (Componente, Type=2)
 *
 * A instância singleton é criada e gerenciada pelo Hilt em [DatabaseModule].
 * [exportSchema] = true gera arquivos JSON de schema em app/schemas/ para
 * controle de versão e geração de migrações futuras.
 *
 * Em caso de mudança de schema, incrementar [version] e fornecer uma [Migration].
 * O [fallbackToDestructiveMigration] no DatabaseModule cobre o período de desenvolvimento.
 */
@Database(
    entities = [MenuAppEntity::class, SyncLogEntity::class, AplicativoEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    /** DAO para operações na tabela menu_app */
    abstract fun menuAppDao(): MenuAppDao

    /** DAO para operações na tabela sync_log */
    abstract fun syncLogDao(): SyncLogDao

    /** DAO para operações na tabela aplicativos */
    abstract fun aplicativoDao(): AplicativoDao
}
