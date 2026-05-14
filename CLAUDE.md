# CLAUDE.md — aplicacaoMenuAutomatico

## O que é este projeto

Aplicativo Android que funciona como um **launcher controlado** para operadores de armazém SAP WM/EWM. Exibe menus de navegação construídos dinamicamente a partir de dados carregados via OData do SAP. É a única porta de entrada para um conjunto de apps Android instalados no dispositivo — o usuário não acessa esses apps diretamente.

Documento de requisitos completo: `docs/REQUIREMENTS.md`
Dados de exemplo do OData: `docs/dadosModelo.json`

---

## Stack

| Item | Valor |
|---|---|
| Linguagem | Kotlin (100% — nenhum arquivo Java) |
| UI | Jetpack Compose + Material3 |
| Package ID | `com.lit.aplicacaomenuautomatico` |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 36 |
| Build | Gradle KTS (`build.gradle.kts`) |
| Versões gerenciadas em | `gradle/libs.versions.toml` |

Dependências ainda a adicionar ao projeto (não estão no `build.gradle.kts`):
- **Room** — banco de dados SQLite local
- **Retrofit + OkHttp** — chamadas HTTP ao SAP OData
- **WorkManager** — sincronização em background a cada 60 minutos
- **Navigation Compose** — back stack de navegação entre menus
- **Hilt** — injeção de dependência
- **ViewModel + StateFlow** — gerenciamento de estado da UI

---

## Filosofia de desenvolvimento

### Kotlin first
- Todo o código deve ser escrito em **Kotlin puro**. Nenhum arquivo `.java`.
- Usar recursos idiomáticos: `data class`, `sealed class`, `object`, `extension functions`, `Flow`, `coroutines`, `scope functions` (`let`, `run`, `apply`, `also`).
- Preferir imutabilidade: `val` sobre `var`, `List` sobre `MutableList` nas interfaces públicas.
- Usar `when` em vez de `if/else` encadeado para múltiplos casos.

### Jetpack Compose first
- Toda a UI deve ser construída com **Jetpack Compose**. Nenhum XML de layout.
- Composables devem ser **stateless** sempre que possível — receber estado como parâmetro e emitir eventos via lambdas.
- Toda lógica de negócio e estado ficam no **ViewModel**, nunca dentro de Composables.
- Usar `remember`, `derivedStateOf` e `rememberUpdatedState` de forma consciente para evitar recomposições desnecessárias.
- Separar Composables pequenos e reutilizáveis. Um Composable não deve passar de ~80 linhas.

### Performance
- Usar `LazyColumn` para listas — nunca `Column` com `forEach` para listas dinâmicas.
- Usar `key` em itens de `LazyColumn` para evitar recomposições incorretas.
- Operações de I/O (banco, rede) **sempre** em `Dispatchers.IO`, nunca na Main thread.
- Usar `StateFlow` com `stateIn` no ViewModel para evitar coletas duplicadas.
- Evitar alocações desnecessárias dentro de Composables — não criar objetos dentro de funções `@Composable` sem `remember`.
- Imagens e ícones devem usar `VectorDrawable` — evitar bitmaps onde possível.

### Comentários no código
- **Todo arquivo, classe, função e bloco não trivial deve ter comentários explicando o que faz e por quê.**
- Comentários em **português brasileiro**.
- Seguir o padrão:
  - Classe/objeto: comentário acima explicando responsabilidade.
  - Função: comentário acima explicando parâmetros, retorno e efeitos colaterais relevantes.
  - Blocos de lógica complexa: comentário inline explicando a intenção.
- Exemplo esperado:
  ```kotlin
  /**
   * Repositório central de menus. Orquestra a busca remota (OData SAP)
   * e a persistência local (Room). Sempre que possível, serve dados do
   * banco local para garantir performance e funcionamento offline.
   */
  class MenuRepository @Inject constructor(
      private val menuDao: MenuDao,
      private val odataService: ODataService
  ) {
      /**
       * Carrega todos os itens de menu do SAP e substitui os dados locais.
       * Deve ser chamado apenas quando há conexão de rede disponível.
       * @param username usuário SAP para Basic Auth
       * @param password senha SAP para Basic Auth
       */
      suspend fun syncFromRemote(username: String, password: String) { ... }
  }
  ```

---

## Arquitetura

Seguir **MVVM** com camadas bem separadas e injeção de dependência via **Hilt**:

```
app/src/main/java/com/lit/aplicacaomenuautomatico/
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt          # Room Database (singleton via Hilt)
│   │   ├── MenuAppDao.kt           # DAO: consultas à tabela menu_app
│   │   ├── SyncLogDao.kt           # DAO: consultas à tabela sync_log
│   │   ├── MenuAppEntity.kt        # Entity Room da tabela menu_app
│   │   └── SyncLogEntity.kt        # Entity Room da tabela sync_log
│   ├── remote/
│   │   ├── ODataService.kt         # Interface Retrofit
│   │   ├── ODataResponse.kt        # Modelos de resposta JSON (d.results[])
│   │   └── NetworkModule.kt        # Hilt module: Retrofit, OkHttp, Basic Auth
│   └── repository/
│       └── MenuRepository.kt       # Orquestra local + remoto
├── domain/
│   ├── model/
│   │   ├── MenuApp.kt              # Modelo de domínio (desacoplado do Room)
│   │   └── SyncLog.kt              # Modelo de domínio do log
│   └── usecase/
│       ├── GetMenuItemsUseCase.kt  # Busca filhos de um menu no SQLite
│       ├── SyncMenuUseCase.kt      # Sincroniza SAP → SQLite
│       └── LoginUseCase.kt         # Autentica e carrega dados iniciais
├── ui/
│   ├── login/
│   │   ├── LoginScreen.kt          # Composable da tela de login
│   │   └── LoginViewModel.kt       # Estado e lógica do login
│   ├── menu/
│   │   ├── MenuScreen.kt           # Composable da tela de menu (todos os níveis)
│   │   ├── MenuViewModel.kt        # Estado, back stack e navegação
│   │   └── components/
│   │       ├── MenuItemCard.kt     # Card de item de menu
│   │       └── ExitConfirmDialog.kt # Diálogo de confirmação de saída
│   └── theme/
│       ├── Color.kt                # Paleta de cores do projeto
│       ├── Theme.kt                # MaterialTheme customizado
│       └── Type.kt                 # Tipografia
└── worker/
    └── SyncWorker.kt               # WorkManager: sync background a cada 60min
```

---

## Design System

### Identidade visual
O app deve ter aparência **moderna, limpa e profissional**. Interface fluida, sem elementos visuais desnecessários. Prioridade à legibilidade e à velocidade de operação — o usuário é um operador de armazém que precisa navegar rápido.

### Paleta de cores

| Token | Hex | Uso |
|---|---|---|
| `Primary` | `#051FC7` | Cor principal — botões, header, itens ativos, FAB |
| `PrimaryContainer` | `#D6DCFF` | Fundo de cards selecionados, chips |
| `OnPrimary` | `#FFFFFF` | Texto/ícone sobre fundo Primary |
| `Surface` | `#F8F9FF` | Fundo geral das telas (branco levemente azulado) |
| `SurfaceVariant` | `#E8EAFF` | Fundo de cards e itens de lista |
| `OnSurface` | `#0D0F1C` | Texto principal |
| `OnSurfaceVariant` | `#44475A` | Texto secundário, subtítulos |
| `Error` | `#BA1A1A` | Mensagens de erro |
| `Outline` | `#C4C6D0` | Bordas, divisores |

```kotlin
// Color.kt — referência de implementação
val Primary       = Color(0xFF051FC7)
val PrimaryContainer = Color(0xFFD6DCFF)
val OnPrimary     = Color(0xFFFFFFFF)
val Surface       = Color(0xFFF8F9FF)
val SurfaceVariant = Color(0xFFE8EAFF)
val OnSurface     = Color(0xFF0D0F1C)
val OnSurfaceVariant = Color(0xFF44475A)
val ErrorColor    = Color(0xFFBA1A1A)
val Outline       = Color(0xFFC4C6D0)
```

### Tipografia
- Fonte padrão do sistema (sem fonte customizada por ora).
- `headlineMedium` — título da tela / nome do menu atual.
- `titleMedium` — texto principal dos itens de menu (`Text`).
- `bodySmall` — texto secundário dos itens (`SText`).

### Componentes visuais

**Tela de Login**
- Fundo com gradiente sutil: `Surface → PrimaryContainer`.
- Logo/ícone do app centralizado no topo.
- Campos de usuário e senha com `OutlinedTextField` estilo Material3.
- Botão "Entrar" com cor `Primary`, largura máxima, bordas arredondadas (`shape = RoundedCornerShape(12.dp)`).
- Indicador de loading (`CircularProgressIndicator`) ao autenticar.

**Tela de Menu**
- `TopAppBar` com cor `Primary`, título com nome do menu atual, botão Voltar quando não estiver na raiz.
- Lista de itens com `LazyColumn`.
- Cada item: `Card` com elevação sutil, `SurfaceVariant` de fundo, ícone à esquerda diferenciando `Type=1` (seta/submenu) de `Type=2` (ação/lançar app), texto principal em `titleMedium` e subtítulo em `bodySmall`.
- Animação de clique (`ripple`) e transição suave entre telas.
- Divider sutil entre itens.

### Animações e transições
- Usar `AnimatedVisibility` e `AnimatedContent` do Compose para transições entre telas de menu.
- Transição de entrada: slide da direita para a esquerda ao avançar no menu.
- Transição de saída: slide da esquerda para a direita ao pressionar Voltar.
- Duração: 300ms com `EaseInOut`.

---

## Regras de negócio principais

### Autenticação
- Tela de login é sempre a primeira tela.
- Credenciais usadas em **Basic Auth** na chamada OData.
- **Primeiro acesso**: rede obrigatória. Sem rede, bloqueia com mensagem.
- **Acessos seguintes**: sem rede, usa dados do SQLite da última sessão sem pedir login.
- Senha **nunca** armazenada em texto claro.

### OData SAP
- Endpoint: `http://vm77.4hub.cloud:57700/sap/opu/odata/sap/zlit_menu_app_ui/MenuApp?sap-client=050`
- Resposta envelopada em `d.results[]`.
- O SAP filtra os registros conforme as permissões do usuário autenticado — não há filtro adicional no app.

### SQLite (Room)
Duas tabelas:
- `menu_app` — dados do menu (chave: `lgnum` + `mmenu` + `sequence`)
- `sync_log` — log de cada sincronização (id, data/hora ISO 8601, status SUCCESS/ERROR, registros atualizados, mensagem de erro)

A cada login bem-sucedido os dados do `menu_app` são **substituídos** pelos dados do SAP.

### Navegação de menus
- `Type = "1"` → exibir filhos: buscar itens onde `Mmenu = Transacao` do item tocado. Sem chamada ao SAP. Campo `Componente` ignorado.
- `Type = "2"` → lançar app externo via **Intent explícita** usando o package ID em `Componente`. Passar `Transacao` como extra da Intent.
- Itens ordenados por `Sequence` convertido para **inteiro** (não string).
- Back stack mantém histórico completo de menus visitados. Voltar no menu raiz (`MAIN`) exibe diálogo de confirmação antes de encerrar.

### Apps externos
- Instalados no dispositivo sem `intent-filter` de launcher — não aparecem para o usuário.
- Chamados exclusivamente via Intent pelo package ID (`Componente`).
- Se o app-alvo não estiver instalado: exibir `Snackbar` com mensagem de erro.

### Sincronização em background
- **WorkManager** sincroniza a cada 60 minutos enquanto houver rede.
- Silenciosa — sem notificação ou indicação visual ao usuário.
- Cada execução (sucesso ou falha) grava um registro em `sync_log`.

---

## Convenções de código

- Kotlin idiomático — `data class`, `sealed class`, `Flow`, corrotinas, extension functions.
- Sem lógica de negócio em Composables — tudo no ViewModel.
- Strings visíveis ao usuário em `res/values/strings.xml`, nunca hardcoded.
- **Comentários obrigatórios** em português em todas as classes, funções e blocos relevantes.
- Não adicionar dependências sem atualizar `gradle/libs.versions.toml`.
- Usar `Result<T>` ou `sealed class` para modelar estados de sucesso/erro nas camadas de dados.
- Erros de rede e banco sempre capturados e propagados como estado — nunca deixar crashar silenciosamente.

---

## Comandos úteis

```bash
# Build debug
./gradlew assembleDebug

# Rodar testes unitários
./gradlew test

# Rodar testes instrumentados (requer dispositivo/emulador)
./gradlew connectedAndroidTest

# Limpar build
./gradlew clean
```
