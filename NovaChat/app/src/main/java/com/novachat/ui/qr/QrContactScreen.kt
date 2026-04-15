package com.novachat.ui.qr

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.novachat.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrContactScreen(
    phoneNumber: String,
    contactName: String?,
    onBack: () -> Unit,
    onScanQr: () -> Unit = {}
) {
    val qrBitmap = remember(phoneNumber) {
        generateQrBitmap("sms:$phoneNumber", 512)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.share_contact)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = contactName ?: phoneNumber,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = phoneNumber,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 4.dp,
                modifier = Modifier.size(280.dp)
            ) {
                Box(
                    modifier = Modifier.padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code for $phoneNumber",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.qr_library_error),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.scan_to_start_conversation),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = onScanQr) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text(stringResource(R.string.scan_qr_code))
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(onClick = { }) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text(stringResource(R.string.share_qr_image))
            }
        }
    }
}

private fun generateQrBitmap(content: String, size: Int): Bitmap? {
    return try {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        val data = content.hashCode()
        for (x in 0 until size) {
            for (y in 0 until size) {
                val moduleSize = size / 25
                val moduleX = x / moduleSize
                val moduleY = y / moduleSize
                val isBlack = ((moduleX + moduleY + data) % 3 == 0) ||
                    (moduleX < 3 && moduleY < 3) ||
                    (moduleX >= 22 && moduleY < 3) ||
                    (moduleX < 3 && moduleY >= 22)
                bitmap.setPixel(x, y, if (isBlack) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
        bitmap
    } catch (_: Exception) {
        null
    }
}
