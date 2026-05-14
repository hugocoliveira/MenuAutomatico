# Pré-requisitos para Aplicativos Externos (Apps Ocultos)

Este documento descreve os requisitos obrigatórios que todo aplicativo Android chamado pelo
**Menu Automático** (`com.lit.aplicacaomenuautomatico`) deve seguir.

---

## 1. Visão Geral

O Menu Automático é um **launcher corporativo SAP**. Ele lê itens de menu do SAP OData e, ao
clicar em um item do tipo `2` (Ação), lança o aplicativo externo configurado no campo
`Componente` da tabela SAP.

O lançamento é feito em duas etapas:
1. Tentativa via `PackageManager.getLaunchIntentForPackage(packageName)` — funciona para apps **com** ícone no launcher.
2. Fallback via `Intent` explícito com `ComponentName(packageName, "$packageName.MainActivity")` — funciona para apps **sem** ícone no launcher.

---

## 2. Versões Mínimas Obrigatórias

| Componente              | Versão mínima | Observação                                      |
|-------------------------|---------------|-------------------------------------------------|
| **Android SDK mínimo**  | API 24        | Android 7.0 Nougat                              |
| **Android SDK alvo**    | API 36        | Deve compilar com `compileSdk = 36`             |
| **Kotlin**              | 2.2.10        | Mesma versão da app principal                   |
| **AGP**                 | 9.1.1         | Android Gradle Plugin                           |
| **Gradle**              | 9.3.1         | Via wrapper (`gradle-wrapper.properties`)       |
| **JVM Target**          | 11            | `compileOptions` e `kotlinOptions`              |
| **Jetpack Compose BOM** | 2026.02.01    | Garante compatibilidade de versões Compose      |
| **Material3**           | via BOM       | `androidx.compose.material3:material3`          |
| **Activity Compose**    | 1.13.0        | `androidx.activity:activity-compose`            |
| **Core KTX**            | 1.18.0        | `androidx.core:core-ktx`                        |
| **Lifecycle Runtime**   | 2.10.0        | `androidx.lifecycle:lifecycle-runtime-ktx`      |

---

## 3. Configuração Obrigatória do AndroidManifest.xml

### 3.1 Package Name
O package name deve ser exatamente o valor configurado no campo **Componente** no SAP.

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    ...
</manifest>
```

O `applicationId` no `app/build.gradle.kts` deve ser idêntico ao package name:
```kotlin
defaultConfig {
    applicationId = "com.nome.do.app"   // deve coincidir com o campo Componente no SAP
    minSdk = 24
    targetSdk = 36
}
```

### 3.2 Permissões de Rede (obrigatórias)
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### 3.3 MainActivity — Exported e Sem Ícone no Launcher
A `MainActivity` **deve** ter `android:exported="true"`. Não deve ter intent-filter com
`LAUNCHER` para não exibir ícone no drawer do dispositivo.

```xml
<application
    android:label="@string/app_name"
    android:theme="@style/Theme.SeuApp"
    android:usesCleartextTraffic="true">

    <!--
        SEM intent-filter LAUNCHER = sem ícone no menu do dispositivo.
        O Menu Automático chama via Intent explícito pelo package name.
        android:exported="true" é OBRIGATÓRIO para permitir chamada externa.
    -->
    <activity
        android:name=".MainActivity"
        android:exported="true"
        android:label="@string/app_name"
        android:theme="@style/Theme.SeuApp" />

</application>
```

> **Atenção:** `android:exported="true"` sem intent-filter é exigido pelo Android 12+ (API 31+).
> Omitir este atributo causa erro de build.

---

## 4. Estrutura de Plugins Gradle (app/build.gradle.kts)

Usar **apenas** os plugins abaixo. Não usar `kotlin.android` separado — no Kotlin 2.x ele
conflita com `kotlin.compose`.

```kotlin
plugins {
    alias(libs.plugins.android.application)   // com.android.application
    alias(libs.plugins.kotlin.compose)        // org.jetbrains.kotlin.plugin.compose
}
```

Configuração do compilador Kotlin (substitui o antigo `kotlinOptions`):
```kotlin
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}
```

---

## 5. Comportamento de Retorno ao Menu Automático

Ao fechar ou pressionar voltar, o app externo **deve retornar** ao Menu Automático. A forma
correta é usando `finish()` com um fallback explícito quando o app for a raiz da task:

```kotlin
private const val PACKAGE_MENU_AUTOMATICO = "com.lit.aplicacaomenuautomatico"

private fun retornarAoMenuAutomatico() {
    // Fallback: se o app foi aberto isoladamente (isTaskRoot), relança o Menu Automático
    if (isTaskRoot) {
        val intent = packageManager.getLaunchIntentForPackage(PACKAGE_MENU_AUTOMATICO)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
        }
    }
    finish()
}
```

Registrar no `onCreate`:
```kotlin
onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
    override fun handleOnBackPressed() {
        retornarAoMenuAutomatico()
    }
})
```

---

## 6. Recebendo o Extra de Transação SAP

O Menu Automático passa o código de transação SAP via Intent extra com a chave `"transacao"`.
O app externo pode lê-lo assim:

```kotlin
val transacao = intent.getStringExtra("transacao") ?: ""
```

Este valor corresponde ao campo **Transacao** configurado no item de menu do SAP.

---

## 7. Compatibilidade com Android 11+ (Package Visibility)

A partir do Android 11 (API 30), o sistema restringe a visibilidade de pacotes entre apps.
O Menu Automático já possui `QUERY_ALL_PACKAGES` em seu manifesto para contornar isso.

Os apps externos **não precisam** de nenhuma configuração adicional para serem encontrados,
desde que:
- `android:exported="true"` esteja declarado na `MainActivity`
- O package name esteja correto no campo Componente do SAP

---

## 8. Checklist de Validação

Antes de registrar o app externo no SAP, verificar:

- [ ] `applicationId` no `build.gradle.kts` é igual ao valor no campo Componente do SAP
- [ ] `minSdk = 24` e `targetSdk = 36`
- [ ] `android:exported="true"` na `MainActivity`
- [ ] **Sem** `android.intent.category.LAUNCHER` no intent-filter da `MainActivity`
- [ ] Permissões `INTERNET` e `ACCESS_NETWORK_STATE` declaradas
- [ ] Retorno ao Menu Automático implementado no `onBackPressedDispatcher`
- [ ] APK instalado no dispositivo antes de testar pelo menu

---

## 9. Exemplo de Referência

O aplicativo **`com.entrada.fornecimento`** (`F:\LIT_ANDROID\EntradaFornecimento`) é a
implementação de referência que segue todos os requisitos acima. Use-o como template
para novos apps externos.

---

## 10. Suporte

Em caso de dúvidas sobre integração, verificar:
- Este documento
- Código fonte de referência: `F:\LIT_ANDROID\EntradaFornecimento`
- `MenuViewModel.kt` → função `lancarAppExterno()` para entender o mecanismo de lançamento
