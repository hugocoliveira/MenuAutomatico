package com.lit.aplicacaomenuautomatico.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Esquema de cores claro do app, mapeando a paleta #051FC7 para todos os
 * slots do Material3. Dynamic color está desabilitado — o app roda em
 * dispositivos industriais onde a identidade visual deve ser consistente.
 */
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    error = ErrorColor,
    onError = OnError,
    outline = Outline
)

/**
 * Tema principal do aplicativo. Aplica o esquema de cores, tipografia e
 * shapes do Material3. Sem suporte a dark mode (contexto de uso industrial).
 */
@Composable
fun AplicacaoMenuAutomaticoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
