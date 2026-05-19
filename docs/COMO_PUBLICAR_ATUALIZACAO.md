# Como publicar uma atualização OTA

Este guia descreve o processo completo para publicar uma nova versão de qualquer um dos 3 aplicativos via GitHub Releases. O sistema OTA detecta automaticamente a nova versão na próxima vez que o usuário fizer login.

---

## Visão geral do fluxo

```
1. Alterar versionCode/versionName no build.gradle.kts
2. Gerar o APK release no Android Studio
3. Commit + push no Git
4. Criar Release no GitHub e subir o APK
5. Atualizar o version.json no GitHub
```

---

## Repositórios e branches

| Aplicativo | Repositório GitHub | Branch do código | Branch do version.json |
|---|---|---|---|
| Menu Automático | `hugocoliveira/MenuAutomatico` | `main_MenuAutomatico` | `main_MenuAutomatico` |
| Entrada Fornecimento | `hugocoliveira/EntradaFornecimento` | `main` | `main` |
| Entrada Transporte | `hugocoliveira/EntradaTransporte` | `master` | `master` |

---

## Passo 1 — Alterar a versão no Android Studio

Abra o arquivo `app/build.gradle.kts` do projeto que está sendo atualizado e localize o bloco `defaultConfig`:

```kotlin
defaultConfig {
    applicationId = "com.lit.aplicacaomenuautomatico"  // não alterar
    versionCode = 1        // ← incrementar em +1 a cada nova versão
    versionName = "1.0"    // ← alterar para o número de versão amigável
}
```

### Regras de versionamento

| Campo | O que é | Exemplo | Regra |
|---|---|---|---|
| `versionCode` | Número inteiro interno | `2`, `3`, `4` | **Sempre incrementar +1.** Nunca repetir ou diminuir. |
| `versionName` | Texto exibido ao usuário | `"1.1"`, `"2.0"` | Livre. Use o padrão `MAJOR.MINOR`. |

**Exemplo de atualização:**
```kotlin
// Antes
versionCode = 1
versionName = "1.0"

// Depois
versionCode = 2
versionName = "1.1"
```

### Localização dos arquivos por projeto

| Projeto | Caminho do arquivo |
|---|---|
| Menu Automático | `F:\LIT_ANDROID\aplicacaoMenuAutomatico\app\build.gradle.kts` |
| Entrada Fornecimento | `F:\LIT_ANDROID\EntradaFornecimento\app\build.gradle.kts` |
| Entrada Transporte | `F:\LIT_ANDROID\EntradaTransporte\app\build.gradle.kts` |

---

## Passo 2 — Gerar o APK release no Android Studio

1. Abra o projeto no Android Studio
2. No menu superior: **Build → Generate Signed App Bundle / APK**
3. Selecione **APK** e clique em **Next**
4. Selecione ou crie o **keystore** (arquivo `.jks`) de assinatura
   - Guarde o keystore em local seguro — sem ele não é possível atualizar o app
5. Selecione o **build variant**: `release`
6. Clique em **Finish** e aguarde a compilação
7. O APK gerado estará em:
   ```
   app/release/app-release.apk
   ```

> **Alternativa via terminal:**
> ```bash
> ./gradlew assembleRelease
> ```
> O APK será gerado em `app/build/outputs/apk/release/app-release.apk`

---

## Passo 3 — Commit e push no Git

No terminal, dentro da pasta do projeto:

```bash
# Adicionar apenas o arquivo modificado
git add app/build.gradle.kts

# Criar o commit
git commit -m "release: versionCode 2, versionName 1.1"

# Fazer o push para o branch correto
git push origin <branch>
```

Substitua `<branch>` conforme a tabela do início:
- Menu Automático → `main_MenuAutomatico`
- Entrada Fornecimento → `main`
- Entrada Transporte → `master`

---

## Passo 4 — Criar a Release no GitHub e subir o APK

1. Acesse o repositório no GitHub
2. No painel lateral direito, clique em **Releases → Create a new release**
3. Em **Choose a tag**, digite a tag da versão e clique em **Create new tag**:
   ```
   v1.1
   ```
   > A tag deve corresponder à `versionName` do app, com `v` na frente.

4. Em **Release title**, coloque o nome da versão:
   ```
   Versão 1.1
   ```
5. Em **Describe this release**, descreva o que mudou (será exibido no diálogo de atualização)
6. Em **Attach binaries**, arraste ou selecione o arquivo APK gerado no Passo 2
   - Renomeie o arquivo para seguir o padrão: `app-release.apk`
7. Clique em **Publish release**
8. Após publicar, clique com o botão direito no link do APK e copie o endereço:
   ```
   https://github.com/hugocoliveira/MenuAutomatico/releases/download/v1.1/app-release.apk
   ```
   > Guarde esse link — ele será usado no próximo passo.

---

## Passo 5 — Atualizar o version.json no GitHub

1. Acesse o repositório no GitHub
2. Clique no arquivo `version.json` na raiz do repositório
3. Clique no ícone de lápis (**Edit this file**)
4. Atualize o conteúdo com os novos valores:

```json
{
  "versionCode": 2,
  "versionName": "1.1",
  "apkUrl": "https://github.com/hugocoliveira/MenuAutomatico/releases/download/v1.1/app-release.apk",
  "releaseNotes": "Descrição do que mudou nesta versão"
}
```

> **Atenção:**
> - `versionCode` deve ser **igual** ao que foi definido no `build.gradle.kts`
> - `apkUrl` deve ser o link copiado no Passo 4 (com a tag e o nome do arquivo corretos)

5. Role a página e clique em **Commit changes**
6. Mensagem sugerida: `release: version.json v1.1`
7. Clique em **Commit changes**

---

## Resumo rápido (checklist)

```
[ ] 1. build.gradle.kts — incrementar versionCode, atualizar versionName
[ ] 2. Android Studio — gerar APK release assinado
[ ] 3. Git — commit + push do build.gradle.kts
[ ] 4. GitHub — criar Release com tag vX.Y, subir o APK como asset
[ ] 5. GitHub — editar version.json com novo versionCode, versionName e apkUrl
```

---

## Como o usuário recebe a atualização

Após o `version.json` ser atualizado no GitHub, na próxima vez que o operador fizer login no **Menu Automático**:

1. Após autenticação, aparece o spinner **"Verificando atualizações..."**
2. O sistema compara o `versionCode` instalado com o do `version.json`
3. Se o remoto for maior → aparece o diálogo **"Atualização obrigatória"** com o nome do app e a versão disponível
4. O operador toca **"Atualizar"** → o APK é baixado e o instalador do Android abre automaticamente
5. Após instalar, o operador faz login novamente e acessa o menu normalmente

---

## Observações importantes

- **Nunca diminua o `versionCode`** — o sistema compara apenas se o remoto é **maior** que o instalado
- **O keystore de assinatura é indispensável** — sem ele o Android não permite instalar atualizações sobre o app já instalado (assinatura incompatível)
- **O `apkUrl` deve apontar para o arquivo exato** — se o nome do arquivo ou a tag estiver errado, o download falhará com "arquivo não encontrado"
- **O `version.json` de cada app é independente** — atualizar um não afeta os outros
