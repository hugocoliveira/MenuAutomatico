package com.lit.aplicacaomenuautomatico.ui.login

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.res.painterResource
import com.lit.aplicacaomenuautomatico.R
import androidx.compose.foundation.Image
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

    // TODO: remover valores padrão antes do deploy em produção
    var usuario by remember { mutableStateOf("ANDROID_API") }
    var senha by remember { mutableStateOf("Lit@2026") }
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

    // Diálogo de atualização/instalação obrigatória — bloqueia acesso ao menu
    if (uiState is LoginUiState.AtualizacaoObrigatoria) {
        val atualizacoes = (uiState as LoginUiState.AtualizacaoObrigatoria).atualizacoes

        // Monitora instalações de pacotes enquanto o diálogo estiver visível.
        // Ao detectar que um dos apps pendentes foi instalado/atualizado, dispara
        // re-verificação no ViewModel — se tudo estiver ok, o estado muda para Sucesso
        // e a navegação ao menu ocorre automaticamente.
        DisposableEffect(Unit) {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: android.content.Context, intent: Intent) {
                    val packageInstalado = intent.data?.schemeSpecificPart ?: return
                    if (atualizacoes.any { it.packageId == packageInstalado }) {
                        viewModel.reVerificarAposInstalacao()
                    }
                }
            }
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addDataScheme("package")
            }
            context.registerReceiver(receiver, filter)
            onDispose { context.unregisterReceiver(receiver) }
        }

        DialogAtualizacaoObrigatoria(
            atualizacoes = atualizacoes,
            onAtualizarClick = {
                atualizacoes.forEach { resultado ->
                    val intent = Intent("com.updater.lib.DOWNLOAD_APK").apply {
                        setPackage(context.packageName)
                        putExtra("apk_url", resultado.updateInfo.apkUrl)
                        putExtra("version_name", resultado.updateInfo.versionName)
                        putExtra("version_code", resultado.updateInfo.versionCode)
                        // Informa o nome do app para que o receiver gere um arquivo
                        // com nome único e exiba a notificação com o app correto.
                        putExtra("app_name", resultado.nomeApp)
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
                .padding(bottom = 12.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo da empresa — tamanho reduzido para caber na tela do MC3300 (4")
            Image(
                painter = painterResource(id = R.drawable.lit),
                contentDescription = "Logo LIT Solutions",
                modifier = Modifier.size(90.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

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

            Spacer(modifier = Modifier.height(20.dp))

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

            Spacer(modifier = Modifier.height(10.dp))

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

            Spacer(modifier = Modifier.height(4.dp))

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

            Spacer(modifier = Modifier.height(14.dp))

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
 * Diálogo obrigatório exibido quando um ou mais apps precisam ser instalados ou atualizados.
 * Não possui botão de cancelar — o operador deve resolver todas as pendências antes de
 * acessar o menu.
 *
 * Diferencia visualmente dois casos:
 *  - App instalado com atualização disponível → ícone SystemUpdate
 *  - App não instalado → ícone Download com texto "Não instalado"
 *
 * @param atualizacoes Lista de apps com pendências (atualização ou instalação)
 * @param onAtualizarClick Callback acionado ao pressionar o botão — inicia todos os downloads
 */
@Composable
private fun DialogAtualizacaoObrigatoria(
    atualizacoes: List<AppUpdateResult>,
    onAtualizarClick: () -> Unit
) {
    var downloadIniciado by remember { mutableStateOf(false) }

    // Define título e subtítulo conforme o tipo de pendência
    val temNaoInstalado = atualizacoes.any { !it.estaInstalado }
    val temAtualizacao  = atualizacoes.any {  it.estaInstalado }
    val titulo = when {
        temNaoInstalado && temAtualizacao -> "Apps pendentes"
        temNaoInstalado                  -> "Instalação necessária"
        else                             -> "Atualização obrigatória"
    }
    val subtitulo = when {
        temNaoInstalado && temAtualizacao ->
            "Os aplicativos abaixo precisam ser instalados ou atualizados antes de continuar:"
        temNaoInstalado ->
            "Os aplicativos abaixo não estão instalados. Baixe-os para continuar:"
        else ->
            "Os aplicativos abaixo precisam ser atualizados antes de continuar:"
    }

    AlertDialog(
        onDismissRequest = { /* não permite fechar — ação obrigatória */ },
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
                text = titulo,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
        },
        text = {
            Column {
                Text(
                    text = subtitulo,
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
                        // Ícone diferente conforme o app está ou não instalado
                        Icon(
                            imageVector = if (resultado.estaInstalado)
                                Icons.Default.SystemUpdate
                            else
                                Icons.Default.Download,
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
                                text = if (resultado.estaInstalado)
                                    "Atualização ${resultado.updateInfo.versionName} disponível"
                                else
                                    "Não instalado — versão ${resultado.updateInfo.versionName} disponível",
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
                    text = if (downloadIniciado) "Aguardando instalação..." else "Baixar e instalar",
                    color = androidx.compose.ui.graphics.Color.White
                )
            }
        }
    )
}
