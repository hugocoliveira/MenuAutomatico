# REQUIREMENTS — Aplicação Menu Automático (Android / SAP WM)

## 1. Visão Geral

**aplicacaoMenuAutomatico** é um launcher controlado para operadores de armazém. O app constrói e exibe menus de navegação dinamicamente, carregando a estrutura de menus e sub-menus diretamente do SAP via OData com as credenciais do usuário autenticado. O SAP retorna apenas os menus aos quais aquele usuário tem acesso — o controle de permissão é delegado inteiramente ao backend SAP.

Os dados recebidos são armazenados em um banco de dados SQLite interno. O menu é servido a partir do SQLite para garantir navegação fluida.

Esta aplicação é a **única porta de entrada** para um conjunto de aplicativos Android instalados no dispositivo. Os apps-alvo não são acessíveis diretamente pelo usuário fora do `aplicacaoMenuAutomatico`.

---

## 2. Contexto do Sistema

| Item | Valor |
|---|---|
| Backend | SAP EWM / WM |
| Serviço OData | `zlit_menu_app_ui` |
| EntitySet | `MenuApp` |
| Endpoint | `http://vm77.4hub.cloud:57700/sap/opu/odata/sap/zlit_menu_app_ui/MenuApp?sap-client=050` |
| Autenticação | Basic Auth (usuário e senha SAP) |
| SAP Client | `050` |
| Chave composta | `Lgnum` + `Mmenu` + `Sequence` |
| Namespace CDS | `cds_zlit_sd_menu_app.MenuAppType` |
| Depósito padrão | `001` |
| Persistência local | SQLite (banco interno do app) |

---

## 3. Modelo de Dados

### 3.1 Entidade `MenuApp`

| Campo | Tipo | Descrição |
|---|---|---|
| `Lgnum` | String(3) | Número do depósito (ex.: `001`) |
| `Mmenu` | String | Código do menu pai (ex.: `MAIN`, `INB00`, `GR00`) |
| `Sequence` | String | Ordem de exibição do item dentro do menu |
| `Componente` | String | Package ID do app Android externo a ser lançado. **Usado apenas quando `Type=2`**; ignorado quando `Type=1`. |
| `Type` | String | `"1"` = exibir sub-menu filho (navega internamente)  \|  `"2"` = lançar app externo instalado no dispositivo |
| `Transacao` | String | Quando `Type=1`: código do menu filho a exibir (valor de `Mmenu` dos filhos). Quando `Type=2`: código da transação SAP passado via Intent ao app externo. |
| `Text` | String | Texto longo exibido no item de menu |
| `SText` | String | Texto curto (para telas pequenas ou listas compactas) |
| `Delete_mc` | Boolean | Flag de permissão de exclusão (informativo, fora de escopo do MVP) |
| `Update_mc` | Boolean | Flag de permissão de atualização (informativo, fora de escopo do MVP) |

### 3.2 Estrutura de Menu (exemplo carregado)

```
MAIN
├── [Seq 1] Processo de entrada de mercadorias  →  INB00
│   ├── [Seq 1] Entrada de mercadorias          →  GR00
│   │   ├── [Seq 1] EM para o fornecimento      →  LM71  (lança: com.entrada.fornecimento)
│   │   └── [Seq 4] EM p/transporte             →  LM73  (lança: com.entrada.transporte)
│   └── [Seq 2] Descarregar                     →  UNLD00
│       ├── [Seq 1] Descarregar para transporte →  LM33  (lança: com.descarregar.transporte)
│       └── [Seq 2] Descarregar para forn.      →  LM34  (lança: com.descarregar.fornecimento)
└── [Seq 2] Processo de saída de mercadorias    →  OUT00
```

### 3.3 Schema SQLite local

Tabela `menu_app`:

| Coluna | Tipo | Descrição |
|---|---|---|
| `lgnum` | TEXT | Número do depósito |
| `mmenu` | TEXT | Código do menu pai |
| `sequence` | TEXT | Ordem do item |
| `componente` | TEXT | Package ID do app externo |
| `type` | TEXT | Tipo do item (`1` ou `2`) |
| `transacao` | TEXT | Código de transação ou sub-menu destino |
| `text` | TEXT | Texto longo |
| `stext` | TEXT | Texto curto |
| PRIMARY KEY | — | (`lgnum`, `mmenu`, `sequence`) |

Tabela `sync_log`:

| Coluna | Tipo | Descrição |
|---|---|---|
| `id` | INTEGER | Chave primária auto-incremento |
| `sync_date` | TEXT | Data e hora da sincronização (formato ISO 8601: `YYYY-MM-DD HH:MM:SS`) |
| `status` | TEXT | Resultado: `SUCCESS` ou `ERROR` |
| `records_updated` | INTEGER | Quantidade de registros atualizados na tabela `menu_app` |
| `error_message` | TEXT | Mensagem de erro em caso de falha (null se sucesso) |
| PRIMARY KEY | — | (`id`) |

---

## 4. Requisitos Funcionais

### RF-01 — Tela de login
- A primeira tela exibida ao abrir o app deve ser a tela de autenticação (usuário e senha).
- O app não deve exibir nenhum menu antes de uma autenticação bem-sucedida.
- As credenciais devem ser usadas em Basic Auth na chamada OData.

### RF-02 — Autenticação e carregamento de dados do SAP
- Após login, o app chama o endpoint OData usando as credenciais informadas:
  ```
  GET http://vm77.4hub.cloud:57700/sap/opu/odata/sap/zlit_menu_app_ui/MenuApp?sap-client=050
  ```
- O SAP retorna apenas os menus que o usuário autenticado tem permissão de ver.
- Em caso de credenciais inválidas (HTTP 401), exibir mensagem de erro e manter o usuário na tela de login.

### RF-03 — Persistência em SQLite
- Os dados retornados pelo OData devem ser gravados na tabela `menu_app` do SQLite interno.
- A cada login bem-sucedido, os dados do SQLite devem ser substituídos pelos dados mais recentes do SAP.
- Os itens devem ser agrupados por `Mmenu` e ordenados por `Sequence` ao serem lidos do SQLite.

### RF-04 — Exibição do menu dinâmico
- O menu exibido inicialmente deve ser o grupo `MAIN` (itens onde `Mmenu = 'MAIN'`).
- Os itens devem ser ordenados pelo valor numérico de `Sequence` (ordenação numérica, não alfabética — ex.: `1, 2, 4, 10` e não `1, 10, 2, 4`).
- Cada item de menu exibe o campo `Text` como texto principal e `SText` como texto secundário ou alternativo.

### RF-05 — Navegação para sub-menu filho (Type = "1")
- Ao tocar em um item com `Type = "1"`, o app exibe a lista de itens cujo `Mmenu` seja igual ao campo `Transacao` do item tocado (leitura do SQLite local, sem nova chamada ao SAP).
- O campo `Componente` de itens `Type=1` é ignorado — nenhum app externo é chamado.
- A navegação deve suportar múltiplos níveis de profundidade (hierarquia irrestrita).

### RF-05a — Back stack de navegação (botão Voltar)
- O app deve manter um histórico (back stack) de todos os menus visitados na sessão.
- Ao pressionar o botão Voltar (físico ou virtual), o app retorna ao menu imediatamente anterior, restaurando a lista e a posição de rolagem daquele nível.
- Exemplo de fluxo:
  ```
  MAIN → [toca "Entrada de mercadorias"] → INB00
       → [toca "Entrada de mercadorias"] → GR00
       → [pressiona Voltar]             → INB00  (restaurado)
       → [pressiona Voltar]             → MAIN   (restaurado)
  ```
- Ao pressionar Voltar estando no menu `MAIN` (raiz), o app deve exibir uma confirmação antes de encerrar ("Deseja sair?").
- A navegação Voltar **não** realiza nova chamada ao SAP — todos os dados já estão no SQLite.

### RF-06 — Lançamento de app externo (Type = "2")
- Ao tocar em um item com `Type = "2"`, o app lança o aplicativo Android cujo package ID está no campo `Componente` (ex.: `com.entrada.fornecimento`).
- O lançamento é feito via Intent explícita usando o package ID — nenhuma navegação de menu ocorre.
- O código da transação (`Transacao`) deve ser passado como extra da Intent para o app-alvo.
- Caso o app-alvo não esteja instalado no dispositivo, exibir mensagem de erro descritiva.

### RF-07 — Controle de acesso aos apps externos
- Os aplicativos externos (ex.: `com.entrada.fornecimento`) são instalados no dispositivo sem ícone no launcher — o usuário não consegue abrí-los diretamente.
- O lançamento ocorre exclusivamente via Intent explícita pelo namespace (package ID) do app-alvo, disparada por este app após autenticação.

### RF-08 — Comportamento offline
- **Primeiro acesso**: obrigatoriamente requer conexão de rede. Sem rede, o app exibe mensagem informando que a conexão é necessária e não avança.
- **Acessos subsequentes**: se não houver rede disponível, o app dispensa a tela de login e exibe o menu usando os dados do SQLite da última sessão.
- O app deve detectar a disponibilidade de rede ao iniciar para decidir o fluxo correto.

### RF-09 — Sincronização em background
- Enquanto o usuário estiver com o app aberto e houver rede disponível, o app deve sincronizar os dados do SAP em background a cada **60 minutos**, sem interromper ou exibir qualquer indicação visual ao usuário.
- A cada sincronização (bem-sucedida ou com erro), um registro deve ser inserido na tabela de log `sync_log` do SQLite.
- O SQLite deve ser atualizado com os novos dados imediatamente após cada sincronização bem-sucedida.

### RF-10 — Seleção de depósito (`Lgnum`)
- O depósito (`Lgnum`) deve ser configurável, não hardcoded.
- O valor padrão inicial é `001`.

---

## 5. Requisitos Não Funcionais

### RNF-01 — Conectividade
- O app deve tratar falhas de rede exibindo mensagem amigável ao usuário com opção de retry.
- Implementar timeout configurável para chamadas OData (padrão sugerido: 30 segundos).

### RNF-02 — Autenticação SAP
- Utilizar Basic Auth com as credenciais informadas na tela de login.
- Não armazenar a senha em texto claro no dispositivo.

### RNF-03 — Performance
- A lista de menus deve ser renderizada em menos de 1 segundo após o carregamento dos dados do SQLite.

### RNF-04 — Compatibilidade
- Android mínimo: API 21 (Android 5.0).
- Suporte a telas de tamanho mdpi a xxxhdpi.

---

## 6. Endpoint OData

```
GET http://vm77.4hub.cloud:57700/sap/opu/odata/sap/zlit_menu_app_ui/MenuApp?sap-client=050
```

- Autenticação: Basic Auth (usuário/senha SAP).
- Resposta envelopada em `d.results[]`.
- O SAP filtra os registros retornados conforme as permissões do usuário autenticado.

---

## 7. Critérios de Aceite

| ID | Critério |
|---|---|
| CA-01 | Ao abrir o app, a tela de login é exibida antes de qualquer menu. |
| CA-02 | Credenciais inválidas exibem mensagem de erro e mantêm o usuário na tela de login. |
| CA-03 | Após login bem-sucedido, os dados do SAP são gravados no SQLite e o menu `MAIN` é exibido. |
| CA-04 | Dois usuários com permissões diferentes visualizam menus diferentes após autenticar. |
| CA-05 | Tocar em item `Type=1` exibe o sub-menu correspondente (lido do SQLite, sem nova chamada SAP). |
| CA-06 | Tocar em item `Type=2` lança o app externo correto passando o código da transação via Intent. |
| CA-07 | Tocar em `Type=2` quando o app-alvo não está instalado exibe mensagem de erro descritiva. |
| CA-08 | O botão Voltar em qualquer sub-menu retorna ao menu pai imediatamente anterior, sem recarregar do SAP. |
| CA-08a | Navegar MAIN → INB00 → GR00 e pressionar Voltar duas vezes retorna corretamente ao MAIN. |
| CA-08b | Pressionar Voltar no menu MAIN exibe confirmação de saída antes de encerrar o app. |
| CA-09 | Os apps externos não aparecem como opção de abertura direta fora do `aplicacaoMenuAutomatico`. |
| CA-10 | Em caso de erro HTTP do SAP no login, o app exibe mensagem descritiva e oferece retry. |
| CA-11 | No primeiro acesso sem rede, o app bloqueia e exibe mensagem de que a conexão é obrigatória. |
| CA-12 | No segundo acesso sem rede, o app exibe o menu do SQLite sem pedir login. |
| CA-13 | A cada 60 minutos com rede disponível, o SQLite é atualizado em background sem notificação ao usuário. |
| CA-14 | Cada sincronização (com sucesso ou falha) gera um registro na tabela `sync_log` com data, hora, status e quantidade de registros atualizados. |

---

---

## 9. Fora de Escopo (MVP)

- Criação/edição de itens de menu pelo app (os flags `Delete_mc`/`Update_mc` são apenas informativos).
- Suporte a múltiplos depósitos simultâneos.
- Modo escuro.
