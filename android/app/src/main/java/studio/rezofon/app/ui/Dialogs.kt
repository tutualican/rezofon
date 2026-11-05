package studio.rezofon.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import studio.rezofon.app.ui.theme.LocalRzPalette

@Composable
fun NameDialog(
    title: String,
    hint: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val p = LocalRzPalette.current
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = p.surface,
        shape = RoundedCornerShape(28.dp),
        title = { Text(title, style = MaterialTheme.typography.titleLarge, color = p.text) },
        text = {
            Column {
                Text(hint, style = MaterialTheme.typography.bodySmall, color = p.textDim)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    placeholder = { Text("Profil adı", color = p.textDim) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text("Kaydet", color = p.lime, style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Vazgeç", color = p.textDim, style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}

@Composable
fun InfoNote(text: String, accent: androidx.compose.ui.graphics.Color? = null) {
    val p = LocalRzPalette.current
    val c = accent ?: p.cyan
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.copy(alpha = 0.08f))
            .border(1.dp, c.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = p.text.copy(alpha = 0.85f))
    }
}
