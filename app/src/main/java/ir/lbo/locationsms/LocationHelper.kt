package ir.lbo.locationsms

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine

object LocationHelper {

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? {
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) return null

        val client = LocationServices.getFusedLocationProviderClient(context)

        return suspendCancellableCoroutine { cont ->
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        if (cont.isActive) cont.resumeWith(Result.success(location))
                    } else {
                        // Fall back to last known location if a fresh fix isn't available
                        client.lastLocation
                            .addOnSuccessListener { last ->
                                if (cont.isActive) cont.resumeWith(Result.success(last))
                            }
                            .addOnFailureListener {
                                if (cont.isActive) cont.resumeWith(Result.success(null))
                            }
                    }
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resumeWith(Result.success(null))
                }
        }
    }
}
