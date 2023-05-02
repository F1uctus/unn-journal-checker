package com.f1uctus.unnjournalchecker

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat.startActivity
import com.f1uctus.unnjournalchecker.ui.theme.UNNJournalCheckerTheme

fun componentIntent(pkg: String, cls: String) = Intent()
    .setComponent(ComponentName(pkg, cls))

private val POWER_MANAGER_INTENTS = arrayOf(
    Intent()
        .setClassName(
            "com.miui.powerkeeper",
            "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
        )
        .putExtra("package_name", MainActivity::class.java.`package`?.name)
        .putExtra("package_label", "UNN Journal Checker"),
    componentIntent(
        "com.miui.powercenter",
        "com.miui.powercenter.PowerSettings"
    ),
    componentIntent(
        "com.huawei.systemmanager",
        "com.huawei.systemmanager.optimize.process.ProtectActivity"
    ),
    componentIntent(
        "com.iqoo.secure",
        "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
    ),
    componentIntent(
        "com.samsung.android.lool",
        "com.samsung.android.sm.battery.ui.BatteryActivity"
    ),
    componentIntent(
        "com.samsung.android.lool",
        "com.samsung.android.sm.ui.battery.BatteryActivity"
    ),
)

@Composable
private fun ExcludeFromPowerSavingInternalDialog(
    intent: Intent?,
    onClose: () -> Unit
) {
    val ctx = LocalContext.current
    AlertDialog(
        onDismissRequest = onClose,
        text = {
            Text(stringResource(R.string.powerSavingExclusionPurpose))
        },
        dismissButton = {
            OutlinedButton(onClose) {
                Text(stringResource(R.string.hide))
            }
        },
        confirmButton = {
            Button({
                startActivity(ctx, intent!!, null)
                onClose()
            }) {
                Text(stringResource(R.string.goToSettings))
            }
        },
    )
}

@Composable
fun ExcludeFromPowerSavingDialog(onClose: () -> Unit) {
    for (intent in POWER_MANAGER_INTENTS) {
        if (LocalContext.current.packageManager.resolveActivity(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            ) != null
        ) {
            ExcludeFromPowerSavingInternalDialog(intent, onClose)
            break
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExcludeFromPowerSavingDialogPreview() {
    UNNJournalCheckerTheme {
        ExcludeFromPowerSavingInternalDialog(null) {}
    }
}
