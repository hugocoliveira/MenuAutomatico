package com.lit.aplicacaomenuautomatico.data.local

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Room
// ─────────────────────────────────────────────────────────────────────────────
import androidx.room.Database    // anotação que configura o banco Room: entidades, versão e schema
import androidx.room.RoomDatabase // classe base abstrata do Room da qual AppDatabase herda

// ═════════════════════════════════════════════════════════════════════════════
// BANCO DE DADOS ROOM: AppDatabase
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Banco de dados Room principal do aplicativo.
 * Arquivo SQLite local: "menu_automatico.db"
 *
 * Contém três tabelas:
 *  - [MenuAppEntity]    (menu_app)   — itens de menu carregados do SAP
 *  - [SyncLogEntity]    (sync_log)   — histórico de sincronizações (auditoria)
 *  - [AplicativoEntity] (aplicativos) — apps externos identificados no menu (Type = "2")
 *
 * A instância singleton é criada e injetada pelo Hilt via [DatabaseModule].
 * Nunca instanciar diretamente — usar sempre via injeção.
 *
 * [exportSchema] = true gera arquivos JSON de schema em app/schemas/ para
 * controle de versão e auxílio na criação de migrações futuras.
 *
 * Política de migração:
 *  - Incrementar [version] ao alterar qualquer entidade ou adicionar tabelas
 *  - Fornecer uma Migration explícita para não perder dados em produção
 *  - [fallbackToDestructiveMigration] no DatabaseModule cobre apenas o período de desenvolvimento
 */
@Database(
    entities     = [MenuAppEntity::class, SyncLogEntity::class, AplicativoEntity::class],
    version      = 2,          // versão atual do schema — incrementar a cada mudança estrutural
    exportSchema = true        // gera app/schemas/com.lit.../AppDatabase/2.json para controle de versão
)
abstract class AppDatabase : RoomDatabase() {

    /** DAO para todas as operações na tabela menu_app (itens de menu do SAP) */
    abstract fun menuAppDao(): MenuAppDao

    /** DAO para todas as operações na tabela sync_log (histórico de sincronizações) */
    abstract fun syncLogDao(): SyncLogDao

    /** DAO para todas as operações na tabela aplicativos (apps externos referenciados no menu) */
    abstract fun aplicativoDao(): AplicativoDao
}
