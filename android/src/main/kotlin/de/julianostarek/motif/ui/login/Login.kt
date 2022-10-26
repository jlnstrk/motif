package de.julianostarek.motif.ui.login

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import de.julianostarek.motif.client.auth.Service

@Composable
fun Login(
    viewModel: AndroidLoginViewModel
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        TextButton(onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(viewModel.loginUrl(Service.Spotify)))
            context.startActivity(intent)
        }) {
            Text("Log in with Spotify")
        }

        TextButton(onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(viewModel.loginUrl(Service.AppleMusic)))
            context.startActivity(intent)
        }) {
            Text("Log in with Apple")
        }
    }
}