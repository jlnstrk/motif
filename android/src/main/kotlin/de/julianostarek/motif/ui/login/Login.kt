package de.julianostarek.motif.ui.login

import android.content.Intent
import android.net.Uri
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun Login(
    viewModel: AndroidLoginViewModel
) {
    val context = LocalContext.current

    TextButton(onClick = {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(viewModel.loginUrl())))
    }) {
        Text("Log in with Spotify")
    }
}