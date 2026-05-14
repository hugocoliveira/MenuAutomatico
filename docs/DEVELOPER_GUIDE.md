# Guia do Desenvolvedor — aplicacaoMenuAutomatico

> Documento de manutenção destinado a desenvolvedores que entrarão no projeto.
> Cobre arquitetura, todas as classes/métodos, e procedimentos práticos
> ("como mudo X?") para tarefas comuns.

---

## Sumário

1. [Visão geral](#1-visão-geral)
2. [Stack e versões](#2-stack-e-versões)
3. [Estrutura de pastas](#3-estrutura-de-pastas)
4. [Ciclo de vida do app — o que acontece ao abrir](#4-ciclo-de-vida-do-app--o-que-acontece-ao-abrir)
5. [Camada `data` — persistência e rede](#5-camada-data--persistência-e-rede)
6. [Camada `domain` — modelos de negócio](#6-camada-domain--modelos-de-negócio)
7. [Camada `di` — injeção de dependência (Hilt)](#7-camada-di--injeção-de-dependência-hilt)
8. [Camada `ui` — telas e ViewModels](#8-camada-ui--telas-e-viewmodels)
9. [Camada `navigation`](#9-camada-navigation)
10. [Camada `worker` — sincronização em background](#10-camada-worker--sincronização-em-background)
11. [Application e Activity raiz](#11-application-e-activity-raiz)
12. [Recursos (`res/`) — strings, cores, tema, ícones](#12-recursos-res--strings-cores-tema-ícones)
13. [Fluxo de dados completo (passo a passo)](#13-fluxo-de-dados-completo-passo-a-passo)
14. [Como fazer (FAQ de manutenção)](#14-como-fazer-faq-de-manutenção)
15. [Build, debug e troubleshooting](#15-build-debug-e-troubleshooting)

---

## 1. Visão geral

### O que o app faz

`aplicacaoMenuAutomatico` é um **launcher controlado** para operadores de armazém que usam SAP WM/EWM. Ele é a **única porta de entrada** para um conjunto de aplicativos Android instalados no coletor (PDA/smartphone). Os apps individuais não aparecem no launcher do sistema — só são acionados a partir deste menu.

O menu não é fixo. Ele é construído dinamicamente a partir de uma chamada **OData** ao SAP, que retorna a hierarquia de menus filtrada pelas permissões do usuário autenticado.

### Como funciona em uma frase

> Login com usuário SAP → app baixa via OData a árvore de menus que aquele usuário pode ver → salva no SQLite local → exibe os menus a partir do nó raiz `MAIN` → ao tocar num item, ou navega para o submenu filho (`Type=1`) ou lança um app externo via Intent (`Type=2`).

### Princípios

- **Offline-first**: depois do primeiro login, o app funciona sem rede usando o cache SQLite.
- **Sync silenciosa**: o WorkManager refaz a sincronização a cada 60 minutos quando há rede, sem incomodar o usuário.
- **Segurança**: senha é guardada com `EncryptedSharedPreferences` (AES256-GCM via Android Keystore), nunca em texto claro.
- **Stateless UI**: toda lógica fica no `ViewModel`; Composables só renderizam estado.

---

## 2. Stack e versões

Definido em `gradle/libs.versions.toml`:

| Tecnologia | Versão | Para quê |
|---|---|---|
| Kotlin | 2.2.10 | Linguagem |
| AGP (Android Gradle Plugin) | 9.1.1 | Build |
| Compose BOM | 2026.02.01 | UI declarativa |
| Material3 | (do BOM) | Design system |
| Hilt | 2.59.2 | Injeção de dependência |
| Room | 2.8.4 | SQLite ORM |
| Retrofit | 2.11.0 | Cliente HTTP |
| OkHttp | 4.12.0 | Engine HTTP |
| Navigation Compose | 2.9.8 | Navegação |
| WorkManager | 2.11.2 | Tarefas em background |
| Security Crypto | 1.1.0-alpha06 | EncryptedSharedPreferences |
| Coroutines | 1.10.2 | Concorrência |
| KSP | 2.2.10-2.0.2 | Code-gen (Room, Hilt) |
| Min SDK | 24 | Android 7.0 |
| Target SDK | 36 | Android moderno |
| Java target | 11 | JVM bytecode |

> **Como atualizar uma dependência**: edite a versão em `gradle/libs.versions.toml` e nada mais. As referências em `app/build.gradle.kts` usam `libs.xxx` — não há versões hardcoded fora do TOML.

---

## 3. Estrutura de pastas

```
aplicacaoMenuAutomatico/
├── CLAUDE.md                       Diretrizes de estilo do projeto
├── docs/
│   ├── REQUIREMENTS.md             Requisitos funcionais completos
│   ├── DEVELOPER_GUIDE.md          (este arquivo)
│   └── dadosModelo.json            Exemplo de resposta OData do SAP
├── build.gradle.kts                Build raiz (apenas declara plugins)
├── settings.gradle.kts             Inclui o módulo :app
├── gradle/libs.versions.toml       Catálogo central de versões
└── app/
    ├── build.gradle.kts            Dependências e config do módulo
    └── src/main/
        ├── AndroidManifest.xml     Permissões, Application, Activity
        ├── res/                    Recursos (strings, cores, ícones, tema XML)
        └── java/com/lit/aplicacaomenuautomatico/
            ├── MenuAutoApp.kt              Application (Hilt + WorkManager)
            ├── MainActivity.kt             Activity única (host do Compose)
            ├── data/
            │   ├── local/                  Room (banco SQLite)
            │   ├── remote/                 Retrofit (SAP OData)
            │   └── repository/             Orquestrador local + remoto
            ├── domain/
            │   └── model/                  Modelos de domínio
            ├── di/                         Módulos Hilt
            ├── ui/
            │   ├── theme/                  Cores, tipografia, MaterialTheme
            │   ├── login/                  LoginScreen + LoginViewModel
            │   └── menu/                   MenuScreen + MenuViewModel
            ├── navigation/                 NavGraph
            └── worker/                     SyncWorker (background)
```

### Padrão de arquitetura

**Clean Architecture simplificada em três camadas**:

```
   ui ──────► domain ◄────── data
  (Compose)  (modelos)      (Room + Retrofit)
       │                          ▲
       └──────────────────────────┘
                via Repository
```

- A UI **nunca** importa Room ou Retrofit. Sempre usa `MenuApp` (modelo de domínio).
- O **Repositório** é a única classe que conhece as duas pontas (Room + OData) e faz a tradução.
- Não há camada `usecase` separada porque o app é simples — o repositório expõe diretamente os métodos consumidos pela UI. Se a complexidade crescer, criar `domain/usecase/`.

---

## 4. Ciclo de vida do app — o que acontece ao abrir

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. Sistema lança MenuAutoApp (Application)                      │
│    @HiltAndroidApp gera o componente Hilt                       │
│    Configuration.Provider configura WorkManager com Hilt        │
└─────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. MainActivity.onCreate                                         │
│    Hilt injeta MenuRepository                                   │
│    Compose monta AplicacaoMenuAutomaticoTheme                   │
└─────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. produceState calcula a rota inicial:                         │
│    bancoVazio = menuRepository.isBancoVazio()                   │
│    temRede    = verificarConectividade()                        │
│                                                                  │
│    bancoVazio          → Login (rede obrigatória)               │
│    !bancoVazio && !rede → Menu  (offline com cache)             │
│    !bancoVazio &&  rede → Login (login pra sincronizar)         │
└─────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│ 4. NavGraph cria a tela inicial                                 │
│    Login: digita credenciais → ViewModel chama OData →          │
│           sucesso salva credenciais e agenda WorkManager →      │
│           navega pro Menu                                        │
│    Menu:  ViewModel.init carrega "MAIN" do SQLite via Flow      │
│           usuário toca item → submenu (Type=1) ou Intent (Type=2)│
└─────────────────────────────────────────────────────────────────┘
```

---

## 5. Camada `data` — persistência e rede

### 5.1. `data/local/` — Room (SQLite)

#### `AppDatabase.kt`
**O que é**: classe abstrata Room que agrega as duas tabelas. É instanciada uma única vez via Hilt em `DatabaseModule`.

| Membro | Descrição |
|---|---|
| `@Database(entities = [...], version = 1, exportSchema = true)` | Lista as entidades e fixa a versão. `exportSchema=true` gera JSON em `app/schemas/` para diff de migrações futuras. |
| `abstract fun menuAppDao(): MenuAppDao` | Provê o DAO de itens de menu. |
| `abstract fun syncLogDao(): SyncLogDao` | Provê o DAO de logs. |

**Quando incrementar `version`**: sempre que mudar qualquer coluna/tabela. Ao incrementar, **substituir** `fallbackToDestructiveMigration` no `DatabaseModule` por uma `Migration` real, senão os dados do operador serão apagados em produção.

#### `MenuAppEntity.kt`
Tabela `menu_app`. Chave primária composta = `(lgnum, mmenu, sequence)` espelhando o EntitySet OData.

| Coluna | Tipo | Descrição |
|---|---|---|
| `lgnum` | String | Depósito SAP (ex: "001") |
| `mmenu` | String | Grupo de menu pai (ex: "MAIN", "INB00") |
| `sequence` | **Int** | Posição numérica. **Conversão importante**: vem como String do SAP, é convertida para Int no `toEntity()` do repositório para ordenação correta (1, 2, 4, 10 — não 1, 10, 2, 4). |
| `componente` | String | Package ID do app externo (só relevante se `type=2`) |
| `type` | String | `"1"` = submenu / `"2"` = lançar app |
| `transacao` | String | Se `type=1`: código do submenu filho. Se `type=2`: código de transação SAP repassado como extra da Intent. |
| `text` | String | Título principal exibido no card |
| `sText` | String | Texto curto (fallback quando `text` está vazio) |

#### `SyncLogEntity.kt`
Tabela `sync_log`. Histórico das tentativas de sincronização.

| Coluna | Tipo | Descrição |
|---|---|---|
| `id` | Long (autogen) | PK |
| `timestamp` | String | ISO 8601 (`yyyy-MM-dd'T'HH:mm:ss`) |
| `status` | String | `"SUCCESS"` ou `"ERROR"` |
| `registrosAtualizados` | Int | Quantidade inserida |
| `mensagemErro` | String? | null em sucesso |

#### `MenuAppDao.kt`

| Método | Tipo | Descrição |
|---|---|---|
| `getItensPorMenu(mmenu): Flow<List<MenuAppEntity>>` | reativo | Lista os itens de um grupo, ordenados por `sequence ASC`. **Flow** porque a UI deve se atualizar sozinha após cada sync. |
| `inserirTodos(itens): suspend` | escrita | Insere/substitui em lote (`OnConflictStrategy.REPLACE`). |
| `deletarTodos(): suspend` | escrita | Limpa a tabela antes de cada sync para evitar registros órfãos. |
| `contarItens(): suspend Int` | leitura | Usado para detectar primeiro acesso (banco vazio). |

#### `SyncLogDao.kt`

| Método | Descrição |
|---|---|
| `inserir(log): suspend` | Grava um registro de sync. |
| `getLogs(): Flow<List<SyncLogEntity>>` | Últimos 50 logs em ordem desc. (não consumido pela UI atual, reservado para tela de diagnóstico futura). |

### 5.2. `data/remote/` — Retrofit (SAP OData)

#### `ODataResponse.kt`
DTOs (objetos que mapeiam o JSON da resposta SAP).

```
ODataEnvelope
   └── d: ODataResults
          └── results: List<MenuAppDto>
```

`MenuAppDto` — campos com nomes capitalizados (convenção SAP) mapeados via `@SerializedName`. **Atenção**:
- `Sequence` vem como **String** ("1", "4", "10"). É convertido para Int em `MenuRepository.toEntity()`.
- `Delete_mc` e `Update_mc` são flags de permissão do SAP, atualmente ignoradas pela UI mas mantidas no DTO para fidelidade ao contrato.

#### `ODataService.kt`
Interface Retrofit. Único endpoint:

```kotlin
@GET("sap/opu/odata/sap/zlit_menu_app_ui/MenuApp")
suspend fun getMenuApp(
    @Query("sap-client") sapClient: String = "050",
    @Query("\$format") format: String = "json",       // SAP retorna XML por padrão
    @Query("sap-language") sapLanguage: String = "PT", // textos em português
    @Header("Authorization") authorization: String     // "Basic <base64>"
): ODataEnvelope
```

**Onde mudar a URL base**: `di/NetworkModule.kt`, constante `BASE_URL`.
**Onde mudar o cliente SAP, idioma, formato**: aqui mesmo, nos defaults dos parâmetros.

### 5.3. `data/repository/MenuRepository.kt`

**Singleton** (anotado `@Singleton`). É a única classe que conhece simultaneamente Room, Retrofit e SharedPreferences. **Toda a lógica de coordenação local↔remoto vive aqui.**

| Método | Tipo | Descrição |
|---|---|---|
| `sincronizarDoServidor(username, password): Result<Int>` | suspend | Faz a chamada OData com Basic Auth, deleta tudo no Room, insere os novos registros, grava no `sync_log`. **Retorna `Result.success(qtd)` ou `Result.failure(exception)`** — nunca lança. |
| `getItensPorMenu(mmenu): Flow<List<MenuApp>>` | reativo | Busca itens no SQLite e mapeia Entity→Domain. A UI consome este Flow. |
| `isBancoVazio(): suspend Boolean` | leitura | True se `count == 0`. Usado no `MainActivity` para decidir rota inicial. |
| `salvarCredenciais(username, password)` | escrita | Persiste no `EncryptedSharedPreferences` (AES256). |
| `getCredenciais(): Pair<String,String>?` | leitura | Recupera credenciais salvas (usado pelo `SyncWorker`). |
| `montarBasicAuth(user, pass)` privada | utilitário | Monta `"Basic " + Base64(user:pass)`. |
| `gravarLog(status, registros, erro)` privada | escrita | Insere registro em `sync_log` com timestamp atual. **Usa `SimpleDateFormat`** porque `LocalDateTime` precisaria de API 26 (minSdk é 24). |

**Extension functions privadas** no fim do arquivo:
- `MenuAppDto.toEntity()` — DTO da rede → Entidade do Room. Faz `sequence.toIntOrNull() ?: 0`.
- `MenuAppEntity.toDomain()` — Entidade do Room → Modelo de domínio (`MenuApp`).

---

## 6. Camada `domain` — modelos de negócio

Modelos puros, sem dependência de framework. A UI consome apenas estes — nunca importa Entities ou DTOs.

### `MenuApp.kt`
Espelho de `MenuAppEntity` sem anotações Room. Tem dois helpers:

```kotlin
val isSubmenu: Boolean get() = type == "1"
val isAcao: Boolean get() = type == "2"
```

Use sempre `item.isSubmenu` / `item.isAcao` na UI — não compare a string `"1"` direto.

### `SyncLog.kt`
Espelho de `SyncLogEntity`. Reservado para uma futura tela de diagnóstico.

---

## 7. Camada `di` — injeção de dependência (Hilt)

Dois módulos, ambos `@InstallIn(SingletonComponent::class)` (instâncias únicas durante toda a vida do app).

### `DatabaseModule.kt`

| Provider | Provê | Notas |
|---|---|---|
| `provideAppDatabase(context)` | `AppDatabase` | Cria com `Room.databaseBuilder` e arquivo `menu_automatico.db`. Usa `fallbackToDestructiveMigration(dropAllTables=true)` — **adequado para v1**. Trocar por `Migration` ao incrementar versão. |
| `provideMenuAppDao(db)` | `MenuAppDao` | Apenas delega `db.menuAppDao()`. |
| `provideSyncLogDao(db)` | `SyncLogDao` | Apenas delega `db.syncLogDao()`. |

### `NetworkModule.kt`

| Provider | Provê | Notas |
|---|---|---|
| `provideOkHttpClient()` | `OkHttpClient` | Timeouts de 30s; logger nível `BASIC` (apenas método/URL/status). Para debug detalhado mude para `BODY`, mas **nunca em release** — vaza credenciais no logcat. |
| `provideRetrofit(client)` | `Retrofit` | `BASE_URL = "http://vm77.4hub.cloud:57700/"`. Usa `GsonConverterFactory`. |
| `provideODataService(retrofit)` | `ODataService` | `retrofit.create(...)`. |
| `provideEncryptedSharedPreferences(context)` | `SharedPreferences` | Cria com `MasterKey` AES256-GCM no Android Keystore. Arquivo `menu_automatico_secure_prefs`. Esquemas: chave `AES256_SIV`, valor `AES256_GCM`. |

> A `MenuRepository` recebe diretamente `SharedPreferences` (e não um wrapper). Se a complexidade crescer (cripto adicional, múltiplos prefs), criar uma classe `SecureCredentialsStore` injetada no lugar.

---

## 8. Camada `ui` — telas e ViewModels

### 8.1. `ui/theme/`

#### `Color.kt`
Tokens de cor. **Modifique aqui** quando quiser ajustar a paleta.

| Token | Valor | Uso |
|---|---|---|
| `Primary` | `#051FC7` | TopAppBar, botões, ícones de destaque |
| `PrimaryContainer` | `#D6DCFF` | Fundo de chips/cards selecionados, gradiente do login |
| `OnPrimary` | `#FFFFFF` | Texto/ícone sobre Primary |
| `Secondary` | `#3A4480` | Reserva, ainda pouco usado |
| `Surface`, `Background` | `#F8F9FF` | Fundo geral |
| `SurfaceVariant` | `#E8EAFF` | Fundo dos cards de menu |
| `OnSurface` | `#0D0F1C` | Texto principal |
| `OnSurfaceVariant` | `#44475A` | Texto secundário |
| `ErrorColor` | `#BA1A1A` | Mensagens de erro, botão "Sair" |
| `Outline` | `#C4C6D0` | Bordas e divisores |

#### `Theme.kt`
- `LightColorScheme` mapeia os tokens acima para slots Material3.
- `AplicacaoMenuAutomaticoTheme(content)` é o composable raiz aplicado em `MainActivity`.
- **Sem dark mode no momento** (decisão deliberada: contexto industrial, identidade visual fixa).

#### `Type.kt`
Tipografia em `FontFamily.Default`. Estilos customizados: `headlineMedium`, `titleLarge`, `titleMedium`, `bodyLarge`, `bodySmall`, `labelMedium`. Para trocar a fonte, mude `fontFamily` em todos eles ou adicione um `FontFamily` carregado de `res/font/`.

### 8.2. `ui/login/`

#### `LoginScreen.kt`
Composable da tela de login. Stateless — só recebe `viewModel` e o callback `onLoginSucesso`.

**Componentes visuais**:
- Fundo com `Brush.verticalGradient(Surface → PrimaryContainer)`.
- Ícone `Icons.Default.Warehouse` (72dp) — *é aqui que se troca a "imagem da tela principal"*; veja seção 14.
- Título "Menu Automático" + subtítulo "SAP Warehouse Management".
- 2 `OutlinedTextField`: usuário e senha.
- Senha com `PasswordVisualTransformation` e botão de olho (`Visibility`/`VisibilityOff`).
- Mensagem de erro animada com `AnimatedVisibility(fadeIn/fadeOut)`.
- Botão "Entrar" 52dp, ou `CircularProgressIndicator` durante loading.
- Footer "LIT Solutions" no `Alignment.BottomCenter`.

**Estado local** (em `remember`): apenas os 3 campos `usuario`, `senha`, `senhaVisivel`. Resto fica no ViewModel.

**Detalhe importante** sobre fechar teclado antes de navegar (linhas 91–97): se o IME estiver aberto na transição, o primeiro toque em "Voltar" no Menu seria consumido para fechar o teclado, e não pelo `OnBackPressedCallback`. Por isso `keyboardController?.hide()` + `focusManager.clearFocus()` antes de `onLoginSucesso()`.

#### `LoginViewModel.kt`

`sealed class LoginUiState`:
| Estado | Significado |
|---|---|
| `Ocioso` | Estado inicial, sem ação. |
| `Carregando` | Autenticando — exibe loader, desabilita campos. |
| `Sucesso` | UI deve navegar para o Menu. |
| `Erro(mensagem)` | Exibe a mensagem ao usuário. |

| Método | Descrição |
|---|---|
| `login(username, password)` | Valida que campos não estão vazios, dispara `sincronizarDoServidor` em `Dispatchers.IO`. Em sucesso: `salvarCredenciais()` + `agendarSyncPeriodico()` + estado `Sucesso`. Em falha: estado `Erro(traduzirErro(e))`. |
| `limparErro()` | Volta para `Ocioso`. Chamado quando o usuário começa a digitar de novo. |
| `agendarSyncPeriodico()` privada | Cria um `PeriodicWorkRequest<SyncWorker>` de 60 minutos com `NetworkType.CONNECTED`. Usa `enqueueUniquePeriodicWork("sync_menu_automatico", KEEP, ...)` — **KEEP** para não recriar se já existe (importante: trocar para `REPLACE` se algum dia o intervalo precisar mudar dinamicamente). |
| `traduzirErro(throwable)` privada | Converte erros HTTP/IO em mensagens amigáveis. Trata `401`, falhas de DNS/timeout/connect, e fallback genérico. |

### 8.3. `ui/menu/`

#### `MenuScreen.kt`
Reaproveitada para **todos os níveis** da hierarquia (MAIN, submenus). Identifica o nível apenas pelo título.

Estrutura:
```
Scaffold
├── topBar:    TopAppBar(Primary)
│              ├── title: uiState.tituloAtual
│              └── navigationIcon: ArrowBack (oculto se tituloAtual=="MAIN")
├── snackbarHost: SnackbarHost(state)        ← erros ao lançar app externo
├── bottomBar: footer "LIT Solutions"
└── content:
     └── if (uiState.carregando) CircularProgressIndicator
        else AnimatedContent(targetState = uiState.tituloAtual)
              transitionSpec: slide-in-da-direita + slide-out-para-esquerda (300ms, fade)
              └── LazyColumn
                    └── items(uiState.itens, key = "${mmenu}_${sequence}")
                         └── MenuItemCard(item, onClick)
```

**Interceptação do botão Voltar** (linhas 85–96): em vez do `BackHandler` do Compose, registra `OnBackPressedCallback` direto no `Activity.onBackPressedDispatcher`. Razão: em versões recentes do Navigation Compose, o `BackHandler` é escopado pelo `NavHost` e não captura corretamente. O `DisposableEffect` garante remoção do callback ao sair da tela.

**Lógica do voltar**:
1. `viewModel.voltarMenuAnterior()` retorna `false` se já está no MAIN (back stack vazio).
2. Nesse caso, abre `ExitConfirmDialog`.
3. Confirmar saída chama `activity?.finish()`.

#### `MenuViewModel.kt`

`data class MenuUiState`:
| Campo | Default | Descrição |
|---|---|---|
| `tituloAtual` | `"MAIN"` | Grupo de menu sendo exibido. |
| `itens` | `emptyList()` | Itens carregados do SQLite (já mapeados para `MenuApp`). |
| `carregando` | `true` | Loader enquanto Flow não emite primeira lista. |
| `mostrarDialogSaida` | `false` | Controla `ExitConfirmDialog`. |
| `erroLancarApp` | `null` | Mensagem para o Snackbar. |

**Estado privado**:
- `backStackMenus: ArrayDeque<String>` — pilha dos menus visitados. **Não usar `mutableListOf().removeLast()`**: em Kotlin 2.x esse método compila para `java.util.List.removeLast`, que só existe no Java 21 / Android 35. `ArrayDeque` tem método concreto compatível com qualquer API.
- `jobColeta: Job?` — guarda a coleta atual do Flow, cancelada ao trocar de menu para evitar coletas paralelas.

| Método | Descrição |
|---|---|
| `init` | Chama `carregarMenu("MAIN")`. |
| `carregarMenu(mmenu)` | Cancela `jobColeta`, marca `carregando=true`, abre nova coleta de `repository.getItensPorMenu(mmenu)`. |
| `navegarParaSubmenu(item)` | Empilha o título atual em `backStackMenus`, chama `carregarMenu(item.transacao)`. |
| `lancarAppExterno(context, item)` | `packageManager.getLaunchIntentForPackage(item.componente)`. Se null → estado de erro. Senão: `putExtra("transacao", item.transacao)` + `FLAG_ACTIVITY_NEW_TASK` + `startActivity`. |
| `voltarMenuAnterior(): Boolean` | Desempilha e carrega. Retorna `false` se a pilha já estava vazia (sinal para mostrar diálogo de saída). |
| `mostrarDialogSaida` / `ocultarDialogSaida` | Controlam `mostrarDialogSaida` no estado. |
| `limparErroLancarApp()` | Zera `erroLancarApp` após o Snackbar ser exibido. |

#### `ui/menu/components/MenuItemCard.kt`
Card de um único item. Stateless.
- `Card(shape=12dp, color=SurfaceVariant, elevation=2dp)`.
- Texto: `item.text.ifBlank { item.sText }`.
- Ícone à direita: `ArrowForwardIos` se `isSubmenu`, `OpenInNew` se `isAcao`.

#### `ui/menu/components/ExitConfirmDialog.kt`
`AlertDialog` simples. Botão "Sair" em `ErrorColor` (vermelho), "Cancelar" em `Primary`.

---

## 9. Camada `navigation`

### `NavGraph.kt`

Apenas duas rotas. A navegação **dentro** dos menus (MAIN → submenu → submenu...) **não** usa rotas Compose — fica inteiramente no `MenuViewModel.backStackMenus`.

```kotlin
sealed class Rota(val caminho: String) {
    object Login : Rota("login")
    object Menu  : Rota("menu")
}
```

`NavGraph(navController, startDestination)`:
- `composable("login")` → `LoginScreen` com callback que faz:
  ```kotlin
  navController.navigate(Rota.Menu.caminho) {
      popUpTo(Rota.Login.caminho) { inclusive = true }
  }
  ```
  O `popUpTo inclusive` remove o Login do back stack (o operador não pode "voltar para o Login" do Menu).
- `composable("menu")` → `MenuScreen`.

---

## 10. Camada `worker` — sincronização em background

### `SyncWorker.kt`

`@HiltWorker` + `@AssistedInject` — necessário porque `Context` e `WorkerParameters` são providos pelo WorkManager (não pelo Hilt). O `MenuRepository` é injetado normalmente.

**Para o Hilt funcionar nos Workers**:
1. `MenuAutoApp` implementa `Configuration.Provider` e expõe um `HiltWorkerFactory`.
2. `AndroidManifest.xml` desabilita a inicialização automática do WorkManager (`<meta-data ... tools:node="remove" />` dentro do `InitializationProvider` do `androidx.startup`).

`override suspend fun doWork(): Result`:
1. `verificarConectividade()` → sem rede, `Result.retry()`.
2. `menuRepository.getCredenciais()` → null, `Result.failure()`.
3. `menuRepository.sincronizarDoServidor(user, pass)` → `success`/`retry`.
4. O `sync_log` é gravado pelo próprio repositório em todos os casos.

> **Quando o Worker dispara**: o agendamento é feito em `LoginViewModel.agendarSyncPeriodico()`, com período de 60 minutos. O Android pode atrasar conforme Doze Mode/App Standby — esperar até ~15 minutos de variação é normal.

---

## 11. Application e Activity raiz

### `MenuAutoApp.kt`
- `@HiltAndroidApp` — gera o componente raiz do Hilt.
- Implementa `Configuration.Provider` para integrar `HiltWorkerFactory` com WorkManager.

### `MainActivity.kt`
- `@AndroidEntryPoint` — habilita injeção.
- `@Inject lateinit var menuRepository: MenuRepository`.
- `enableEdgeToEdge()` — usa toda a tela, incluindo área das barras de status/navegação.
- Decide rota inicial em `produceState` (suspende para `isBancoVazio()` + `verificarConectividade()`).
- Enquanto a rota não foi resolvida: exibe `CircularProgressIndicator` central.
- `verificarConectividade()` privada — usa `ConnectivityManager.getNetworkCapabilities` (compatível com API 24+).

---

## 12. Recursos (`res/`) — strings, cores, tema, ícones

### `AndroidManifest.xml`
Permissões: `INTERNET`, `ACCESS_NETWORK_STATE`. `usesCleartextTraffic="true"` é necessário porque o servidor SAP usa HTTP (não HTTPS).

A Activity tem `intent-filter MAIN/LAUNCHER` — o app aparece no launcher do sistema operacional (é o único app visível pro operador).

### `res/values/strings.xml`
Strings traduzíveis: `app_name`, textos da tela de login, da tela de menu e do diálogo de saída.

> **Atenção**: nem todos os Composables usam `stringResource` — vários textos estão hardcoded em Kotlin (`"Menu Automático"`, `"Usuário SAP"`, `"Entrar"`, etc.). Para internacionalizar, substitua por `stringResource(R.string.xxx)`. As chaves já existem.

### `res/values/themes.xml`
Tema XML mínimo `Theme.AplicacaoMenuAutomatico` herdando `Theme.Material.Light.NoActionBar`. Existe só para o tema de inicialização do sistema (splash) — a UI real usa o `MaterialTheme` do Compose, configurado em `ui/theme/Theme.kt`.

### `res/values/colors.xml`
Cores legacy da template original do Android Studio (`purple_500` etc.). **Não são usadas** pelo app — manter ou remover é indiferente.

### Ícones do app (launcher)
- `mipmap-anydpi-v26/ic_launcher.xml` e `ic_launcher_round.xml` — adaptive icons que combinam:
  - `drawable/ic_launcher_background.xml` — fundo
  - `drawable/ic_launcher_foreground.xml` — primeiro plano

Para trocar o ícone do app, ver seção 14.

---

## 13. Fluxo de dados completo (passo a passo)

### Cenário A — primeiro login (online)
```
1. Operador abre o app.
2. MainActivity → produceState: bancoVazio=true → rota=Login.
3. Operador digita user/senha → Botão "Entrar".
4. LoginViewModel.login()
   ├─ uiState = Carregando
   └─ menuRepository.sincronizarDoServidor(user, pass)
       ├─ Authorization header montado.
       ├─ ODataService.getMenuApp() — Retrofit faz GET no SAP.
       ├─ Resposta deserializada por Gson em ODataEnvelope.
       ├─ MenuAppDao.deletarTodos()
       ├─ MenuAppDao.inserirTodos(entidades)
       └─ SyncLogDao.inserir(SUCCESS, count, null)
   ├─ menuRepository.salvarCredenciais(user, pass)  [EncryptedSharedPrefs]
   ├─ agendarSyncPeriodico() → WorkManager
   └─ uiState = Sucesso
5. LoginScreen.LaunchedEffect detecta Sucesso → onLoginSucesso().
6. NavGraph navega para "menu" e remove "login" do back stack.
7. MenuViewModel.init → carregarMenu("MAIN").
8. MenuRepository.getItensPorMenu("MAIN") → Flow do Room emite a lista.
9. MenuScreen renderiza LazyColumn com MenuItemCards.
```

### Cenário B — primeiro acesso sem rede
```
1. MainActivity: bancoVazio=true → rota=Login.
2. Operador tenta logar.
3. LoginViewModel.login() → sincronizarDoServidor() falha em IO.
4. traduzirErro() → "Não foi possível conectar ao servidor SAP..."
5. uiState = Erro(mensagem) → AnimatedVisibility exibe o erro.
```
> O app **bloqueia** o primeiro acesso sem rede por design (requisito).

### Cenário C — usuário recorrente sem rede
```
1. MainActivity: bancoVazio=false, temRede=false → rota=Menu (direto).
2. MenuViewModel carrega "MAIN" do SQLite (cache da última sync).
3. Operador navega normalmente.
4. WorkManager tenta sincronizar a cada 60 min — sem rede, Result.retry().
```

### Cenário D — toque em item Type=2 (lançar app)
```
1. MenuItemCard.onClick → MenuViewModel.lancarAppExterno(context, item).
2. packageManager.getLaunchIntentForPackage(item.componente).
3a. App não instalado → erroLancarApp = "Aplicativo 'X' não encontrado..."
    → MenuScreen.LaunchedEffect → snackbarHostState.showSnackbar()
3b. App instalado → intent.putExtra("transacao", item.transacao)
    → intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
    → context.startActivity(intent).
4. O app externo lê getIntent().getStringExtra("transacao") e processa.
```

### Cenário E — Voltar pressionado
```
1. OnBackPressedCallback registrado em MenuScreen captura o evento.
2. viewModel.voltarMenuAnterior():
   - Pilha vazia (MAIN) → return false.
   - Pilha com algo → desempilha, carregarMenu(menuAnterior), return true.
3. Se retornou false → viewModel.mostrarDialogSaida() → AlertDialog.
4. "Sair" → activity.finish(). "Cancelar" → ocultarDialogSaida().
```

---

## 14. Como fazer (FAQ de manutenção)

### 14.1. Como mudo o ícone/imagem da tela de login (ícone do "armazém")?

A imagem central da tela de login é um **vector do Material Icons**: `Icons.Default.Warehouse`.

**Arquivo**: `ui/login/LoginScreen.kt`, linhas ~128–133.

```kotlin
Icon(
    imageVector = Icons.Default.Warehouse,   // ← TROCAR AQUI
    contentDescription = "Ícone do aplicativo",
    tint = Primary,
    modifier = Modifier.size(72.dp)          // tamanho
)
```

**Opção 1 — outro ícone do Material**: troque para qualquer outro de `Icons.Default.*`, `Icons.Outlined.*`, etc. Lista completa em [fonts.google.com/icons](https://fonts.google.com/icons). Exemplos comuns:
- `Icons.Default.Inventory2`
- `Icons.Default.LocalShipping`
- `Icons.Outlined.QrCodeScanner`

**Opção 2 — logo customizada (PNG/SVG)**:
1. Coloque a imagem em `app/src/main/res/drawable/logo_app.png` (ou `.xml` se for vector).
2. Substitua o `Icon` por:
   ```kotlin
   Image(
       painter = painterResource(R.drawable.logo_app),
       contentDescription = "Logo LIT",
       modifier = Modifier.size(96.dp)
   )
   ```
3. Adicione o import `androidx.compose.foundation.Image` e `androidx.compose.ui.res.painterResource`.

### 14.2. Como mudo o ícone do app (que aparece no launcher do Android)?

1. Substitua os arquivos:
   - `app/src/main/res/drawable/ic_launcher_foreground.xml` (figura)
   - `app/src/main/res/drawable/ic_launcher_background.xml` (cor de fundo)
2. Ou, mais fácil: clique-direito em `app/src/main/res` no Android Studio → **New → Image Asset** → Launcher Icons → escolha PNG/SVG.

Se quiser mudar o **nome** que aparece sob o ícone: `res/values/strings.xml`, `<string name="app_name">`.

### 14.3. Como mudo a cor primária (azul) do app?

`ui/theme/Color.kt`:
```kotlin
val Primary = Color(0xFF051FC7)   // troque o hex
```

Recompile. Como a UI inteira referencia o token `Primary` (e não cores hardcoded), basta este ponto. Se mudar o tom drasticamente, ajuste também `OnPrimary` (texto sobre o primário) para manter contraste.

### 14.4. Como mudo o intervalo da sincronização em background?

`ui/login/LoginViewModel.kt`, método `agendarSyncPeriodico()`:
```kotlin
val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(60, TimeUnit.MINUTES) // ←
```
**Mínimo possível**: 15 minutos (limite imposto pelo WorkManager).

⚠️ Como o `enqueueUniquePeriodicWork` usa `ExistingPeriodicWorkPolicy.KEEP`, mudanças no intervalo só valem para novas instalações ou após desinstalar/reinstalar. Para que valha imediatamente, troque para `REPLACE`.

### 14.5. Como mudo a URL do SAP / mandante / idioma?

- **URL base**: `di/NetworkModule.kt`, constante `BASE_URL`.
- **Mandante (sap-client)**: `data/remote/ODataService.kt`, default do parâmetro `sapClient`.
- **Idioma**: `data/remote/ODataService.kt`, default do parâmetro `sapLanguage` (PT, EN, DE…).

### 14.6. Como adiciono um novo tipo de item (além de submenu e ação)?

Hoje só existem `Type=1` e `Type=2`. Para um `Type=3` (ex: abrir URL no browser):

1. **Domain** — `domain/model/MenuApp.kt`: adicione um helper:
   ```kotlin
   val isUrl: Boolean get() = type == "3"
   ```
2. **UI** — `ui/menu/components/MenuItemCard.kt`: estenda o `if/else` do ícone, ou troque por `when (item.type)`.
3. **ViewModel** — `ui/menu/MenuViewModel.kt`: adicione `fun abrirUrl(context, item)`.
4. **Tela** — `ui/menu/MenuScreen.kt`: no `onClick` do `MenuItemCard`, adicione:
   ```kotlin
   when {
       item.isSubmenu -> viewModel.navegarParaSubmenu(item)
       item.isAcao    -> viewModel.lancarAppExterno(context, item)
       item.isUrl     -> viewModel.abrirUrl(context, item)
   }
   ```
5. **Dado vindo do SAP**: o backend precisa retornar `Type="3"` com a URL no campo `Componente` ou `Transacao`.

### 14.7. Como adiciono uma nova tela (ex: configurações)?

1. Crie `ui/configuracoes/ConfiguracoesScreen.kt` e `ConfiguracoesViewModel.kt` (este último com `@HiltViewModel`).
2. Em `navigation/NavGraph.kt`: adicione `object Configuracoes : Rota("configuracoes")`.
3. Adicione um `composable(Rota.Configuracoes.caminho) { ... }`.
4. Para navegar de outra tela: receba o `navController` (ou um lambda `onAbrirConfiguracoes`) e chame `navController.navigate(Rota.Configuracoes.caminho)`.

### 14.8. Como adiciono uma nova coluna na tabela `menu_app`?

1. Adicione o campo na entidade `MenuAppEntity.kt`.
2. Adicione no DTO `MenuAppDto` (com `@SerializedName` correspondente ao SAP).
3. Adicione no modelo `MenuApp` e nas extension functions `toEntity()`/`toDomain()` em `MenuRepository.kt`.
4. **Incremente** `version = 2` em `AppDatabase.kt`.
5. Substitua `fallbackToDestructiveMigration` por uma `Migration(1, 2)` real:
   ```kotlin
   val MIGRATION_1_2 = object : Migration(1, 2) {
       override fun migrate(db: SupportSQLiteDatabase) {
           db.execSQL("ALTER TABLE menu_app ADD COLUMN nova_coluna TEXT NOT NULL DEFAULT ''")
       }
   }
   ```
   E em `DatabaseModule.kt`:
   ```kotlin
   .addMigrations(MIGRATION_1_2)
   ```

### 14.9. Como vejo o que está acontecendo com a sincronização?

Não há tela de logs ainda, mas o `SyncLogDao.getLogs()` já existe. Os caminhos:
- **Logcat**: filtro `OkHttp` mostra todas as chamadas (nível BASIC).
- **Banco**: abrir `menu_automatico.db` com Database Inspector do Android Studio (View → Tool Windows → App Inspection → Database Inspector) e consultar `SELECT * FROM sync_log ORDER BY id DESC`.
- **WorkManager Inspector** (mesma janela do Database Inspector): mostra tentativas e estados de `sync_menu_automatico`.

### 14.10. Como migro para HTTPS quando o SAP suportar?

1. Atualize `BASE_URL` em `NetworkModule.kt` para `https://...`.
2. Remova `android:usesCleartextTraffic="true"` do `AndroidManifest.xml`.
3. Se o servidor usar certificado auto-assinado, configure um `OkHttpClient` com `TrustManager` customizado — **mas** prefira convencer a infra a usar um certificado válido.

### 14.11. Como faço logout / forçar relogin?

Hoje não há logout explícito. Para implementar:
1. Adicione `fun limparCredenciais()` em `MenuRepository`:
   ```kotlin
   fun limparCredenciais() {
       securePrefs.edit().clear().apply()
   }
   ```
2. Adicione `fun limparBanco()` chamando `menuAppDao.deletarTodos()`.
3. Cancele o WorkManager: `WorkManager.getInstance(ctx).cancelUniqueWork("sync_menu_automatico")`.
4. Navegue para `Rota.Login` com `popUpTo(Rota.Menu) { inclusive = true }`.

---

## 15. Build, debug e troubleshooting

### Comandos

```bash
# build debug
./gradlew assembleDebug

# build release (sem signing config configurado — precisa adicionar)
./gradlew assembleRelease

# instalar no dispositivo conectado
./gradlew installDebug

# rodar testes unitários
./gradlew test

# rodar testes instrumentados (precisa device/emulador)
./gradlew connectedAndroidTest

# limpar
./gradlew clean
```

### Erros comuns

| Sintoma | Causa provável | Solução |
|---|---|---|
| `IllegalStateException: WorkerFactory was not registered` | `Configuration.Provider` não está sendo chamado | Confirmar que o `provider InitializationProvider` no Manifest tem `tools:node="remove"` para `WorkManagerInitializer` |
| `401 Unauthorized` no login com credenciais corretas | Mandante errado, idioma errado, ou senha SAP expirada | Verificar `sap-client` em `ODataService.kt` |
| Tela em branco depois de logar | Banco com itens cujo `mmenu` ≠ "MAIN" | O OData precisa retornar pelo menos uma linha com `Mmenu = "MAIN"` |
| `Sequence` aparece desordenado (1, 10, 2) | Em algum lugar a comparação está sendo feita como String | Confirmar que `MenuAppEntity.sequence` é `Int` e que o mapper faz `toIntOrNull()` |
| Senha vaza no logcat | `HttpLoggingInterceptor.Level.BODY` em produção | Sempre usar `BASIC` em release; logar BODY só em debug local |
| Crash ao remover último item da lista no Android < 35 | Uso de `mutableListOf().removeLast()` em vez de `ArrayDeque` | Usar `ArrayDeque` (já é o caso em `MenuViewModel.backStackMenus`) |
| Botão Voltar não funciona após login | Teclado ainda aberto consumiu o evento | Manter `keyboardController?.hide()` antes de `onLoginSucesso()` em `LoginScreen` |

### Boas práticas ao mexer no código

1. **Toda lógica em ViewModel** — Composable só renderiza estado.
2. **Toda I/O em `Dispatchers.IO`** — nunca acessar Room ou Retrofit na Main thread.
3. **Strings em Kotlin é dívida técnica** — mover para `strings.xml` quando puder.
4. **Não importar Room nem Retrofit na UI** — sempre passar pelo Repositório.
5. **Comentários em PT-BR** seguindo padrão CLAUDE.md.
6. **Não suba versão do banco sem migration** — apaga dados do operador.
7. **Não comite `local.properties`** (já está no `.gitignore` por padrão).

---

## Apêndice — Referências cruzadas rápidas

| Quero mexer em… | Vá para… |
|---|---|
| Cor primária | `ui/theme/Color.kt` |
| Tipografia | `ui/theme/Type.kt` |
| URL do SAP | `di/NetworkModule.kt` (`BASE_URL`) |
| Ícone da tela de login | `ui/login/LoginScreen.kt` (Icon `Warehouse`) |
| Ícone do app no launcher | `res/drawable/ic_launcher_*.xml` |
| Strings da UI | `res/values/strings.xml` |
| Período do sync background | `ui/login/LoginViewModel.kt` (`agendarSyncPeriodico`) |
| Rota inicial (login vs menu) | `MainActivity.kt` (bloco `produceState`) |
| Mensagens de erro de login | `ui/login/LoginViewModel.kt` (`traduzirErro`) |
| Lógica do botão Voltar | `ui/menu/MenuScreen.kt` + `MenuViewModel.voltarMenuAnterior` |
| Estrutura do banco | `data/local/MenuAppEntity.kt` e `SyncLogEntity.kt` |
| Esquema da resposta OData | `data/remote/ODataResponse.kt` |
| Onde os dados vão pra UI | `MenuRepository.getItensPorMenu` |
| Onde o app externo é lançado | `MenuViewModel.lancarAppExterno` |

---

*Última atualização: 2026-05-07.*
