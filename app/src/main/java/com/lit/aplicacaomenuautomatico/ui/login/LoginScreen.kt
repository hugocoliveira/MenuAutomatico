package com.lit.aplicacaomenuautomatico.ui.login

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lit.aplicacaomenuautomatico.ui.theme.OnSurface
import com.lit.aplicacaomenuautomatico.ui.theme.OnSurfaceVariant
import com.lit.aplicacaomenuautomatico.ui.theme.Primary
import com.lit.aplicacaomenuautomatico.ui.theme.PrimaryContainer
import com.lit.aplicacaomenuautomatico.ui.theme.Surface

/**
 * Tela de login do aplicativo.
 * É sempre a primeira tela exibida ao abrir o app (exceto modo offline com dados cached).
 *
 * Composable stateless — todo o estado vive no [LoginViewModel].
 * A tela coleta o [LoginUiState] via [collectAsStateWithLifecycle] para respeitar
 * o ciclo de vida e evitar coletas desnecessárias em background.
 *
 * @param viewModel ViewModel com a lógica de autenticação
 * @param onLoginSucesso Callback chamado após login bem-sucedido — navega para o Menu
 */
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSucesso: () -> Unit
) {
    // Coleta o estado reativo do ViewModel respeitando o ciclo de vida
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var usuario by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var senhaVisivel by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Sucesso) {
            keyboardController?.hide()
            focusManager.clearFocus()
            onLoginSucesso()
        }
    }

    // Diálogo de atualização obrigatória — bloqueia acesso ao menu
    if (uiState is LoginUiState.AtualizacaoObrigatoria) {
        val atualizacoes = (uiState as LoginUiState.AtualizacaoObrigatoria).atualizacoes
        DialogAtualizacaoObrigatoria(
            atualizacoes = atualizacoes,
            onAtualizarClick = {
                atualizacoes.forEach { resultado ->
                    val intent = Intent("com.updater.lib.DOWNLOAD_APK").apply {
                        setPackage(context.packageName)
                        putExtra("apk_url", resultado.updateInfo.apkUrl)
                        putExtra("version_name", resultado.updateInfo.versionName)
                        putExtra("version_code", resultado.updateInfo.versionCode)
                    }
                    context.sendBroadcast(intent)
                }
            }
        )
    }

    // Fundo com gradiente suave de Surface até PrimaryContainer para identidade visual
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Surface, PrimaryContainer),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    ) {
        Text(
            text = "LIT Solutions  •  v${com.lit.aplicacaomenuautomatico.BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
            color = OnSurfaceVariant,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Ícone representando o contexto de armazém/logística
            Icon(
                imageVector = Icons.Default.Warehouse,
                contentDescription = "Ícone do aplicativo",
                tint = Primary,
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Título do app
            Text(
                text = "Menu Automático",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Primary
            )

            Text(
                text = "SAP Warehouse Management",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Campo de usuário SAP
            OutlinedTextField(
                value = usuario,
                onValueChange = {
                    usuario = it
                    if (uiState is LoginUiState.Erro) viewModel.limparErro()
                },
                label = { Text("Usuário SAP") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Usuário",
                        tint = Primary
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                singleLine = true,
                enabled = uiState is LoginUiState.Ocioso || uiState is LoginUiState.Erro,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    focusedLabelColor = Primary,
                    cursorColor = Primary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo de senha com toggle de visibilidade
            OutlinedTextField(
                value = senha,
                onValueChange = {
                    senha = it
                    if (uiState is LoginUiState.Erro) viewModel.limparErro()
                },
                label = { Text("Senha") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Senha",
                        tint = Primary
                    )
                },
                trailingIcon = {
                    // Botão para alternar visibilidade da senha
                    IconButton(onClick = { senhaVisivel = !senhaVisivel }) {
                        Icon(
                            imageVector = if (senhaVisivel) Icons.Default.Visibility
                                          else Icons.Default.VisibilityOff,
                            contentDescription = if (senhaVisivel) "Ocultar senha" else "Mostrar senha",
                            tint = OnSurfaceVariant
                        )
                    }
                },
                visualTransformation = if (senhaVisivel) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    // Submete o formulário ao pressionar "Done" no teclado
                    onDone = {
                        focusManager.clearFocus()
                        viewModel.login(usuario, senha)
                    }
                ),
                singleLine = true,
                enabled = uiState is LoginUiState.Ocioso || uiState is LoginUiState.Erro,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    focusedLabelColor = Primary,
                    cursorColor = Primary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Mensagem de erro — visível apenas no estado Erro
            AnimatedVisibility(
                visible = uiState is LoginUiState.Erro,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = (uiState as? LoginUiState.Erro)?.mensagem ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Indicador de carregamento (autenticando ou verificando atualizações)
            if (uiState is LoginUiState.Carregando || uiState is LoginUiState.VerificandoAtualizacoes) {
                CircularProgressIndicator(
                    color = Primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (uiState is LoginUiState.VerificandoAtualizacoes)
                        "Verificando atualizações..."
                    else
                        "Autenticando...",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            } else {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.login(usuario, senha)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        contentColor = OnSurface.copy(alpha = 0f).copy(alpha = 1f)
                    )
                ) {
                    Text(
                        text = "Entrar",
                        style = MaterialTheme.typography.titleMedium,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }
            }
        }
    }
}

/**
 * Diálogo obrigatório exibido quando um ou mais apps têm atualização disponível.
 * Não possui botão de cancelar — o usuário deve instalar antes de acessar o menu.
 *
 * @param atualizacoes Lista de apps com atualização pendente
 * @param onAtualizarClick Callback acionado ao pressionar "Atualizar" — inicia os downloads
 */
@Composable
private fun DialogAtualizacaoObrigatoria(
    atualizacoes: List<AppUpdateResult>,
    onAtualizarClick: () -> Unit
) {
    var downloadIniciado by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { /* não permite fechar — atualização obrigatória */ },
        icon = {
            Icon(
                imageVector = Icons.Default.SystemUpdate,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Atualização obrigatória",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
        },
        text = {
            Column {
                Text(
                    text = "Os aplicativos abaixo precisam ser atualizados antes de continuar:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                atualizacoes.forEachIndexed { index, resultado ->
                    if (index > 0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = resultado.nomeApp,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = OnSurface
                            )
                            Text(
                                text = "Versão ${resultado.updateInfo.versionName} disponível",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }
                    }
                }

                if (downloadIniciado) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Download iniciado. Instale os apps e faça login novamente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!downloadIniciado) {
                        downloadIniciado = true
                        onAtualizarClick()
                    }
                },
                enabled = !downloadIniciado,
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (downloadIniciado) "Aguardando instalação..." else "Atualizar",
                    color = androidx.compose.ui.graphics.Color.White
                )
            }
        }
    )
}
