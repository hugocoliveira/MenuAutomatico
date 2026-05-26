package com.lit.aplicacaomenuautomatico.ui.theme

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Material3 / Compose
// ─────────────────────────────────────────────────────────────────────────────
import androidx.compose.material3.MaterialTheme    // provedor do tema: cores, tipografia e shapes
import androidx.compose.material3.lightColorScheme // constrói o esquema de cores claro do Material3
import androidx.compose.runtime.Composable         // marca a função como Composable (UI declarativa)

// ═════════════════════════════════════════════════════════════════════════════
// ESQUEMA DE CORES
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Esquema de cores claro do app, mapeando a paleta #051FC7 para todos os
 * slots do Material3.
 *
 * Dynamic Color está desabilitado intencionalmente — o app roda em dispositivos
 * industriais (coletores Zebra) onde a identidade visual deve ser consistente
 * independentemente da cor de papel de parede do usuário.
 *
 * Cores definidas em Color.kt e referenciadas aqui por nome semântico.
 */
private val LightColorScheme = lightColorScheme(
    primary            = Primary,            // azul escuro #051FC7 — botões, header, destaques
    onPrimary          = OnPrimary,          // branco — texto/ícone sobre Primary
    primaryContainer   = PrimaryContainer,   // azul claro #D6DCFF — cards selecionados
    onPrimaryContainer = OnPrimaryContainer, // azul muito escuro — texto sobre PrimaryContainer
    secondary          = Secondary,          // azul médio #3A4480 — elementos secundários
    onSecondary        = OnSecondary,        // branco — texto sobre Secondary
    background         = Background,         // branco azulado — fundo global da tela
    onBackground       = OnBackground,       // quase preto — texto sobre Background
    surface            = Surface,            // branco azulado — fundo de telas e sheets
    onSurface          = OnSurface,          // quase preto — texto principal sobre Surface
    surfaceVariant     = SurfaceVariant,     // azul muito claro — fundo de cards e listas
    onSurfaceVariant   = OnSurfaceVariant,   // cinza azulado — texto secundário/subtítulos
    error              = ErrorColor,         // vermelho #BA1A1A — mensagens e estados de erro
    onError            = OnError,            // branco — texto/ícone sobre fundo de erro
    outline            = Outline             // cinza claro — bordas, divisores e contornos
)

// ═════════════════════════════════════════════════════════════════════════════
// TEMA PRINCIPAL
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Tema principal do aplicativo.
 * Aplica o esquema de cores, a tipografia e os shapes do Material3 para toda
 * a árvore de Composables filhos.
 *
 * Dark mode não é suportado — contexto de uso industrial sem alternância de tema.
 * Usado no [MainActivity] via setContent e nos @Preview dos Composables.
 *
 * @param content Conteúdo Composable que herdará este tema
 */
@Composable
fun AplicacaoMenuAutomaticoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme, // aplica a paleta #051FC7 a todos os componentes Material3
        typography  = Typography,       // aplica os estilos de texto definidos em Type.kt
        content     = content           // renderiza os Composables filhos com o tema ativo
    )
}
