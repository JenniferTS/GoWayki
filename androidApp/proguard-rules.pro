# Keep entry points referenced by baseline-prof.txt
-keep class com.myapplication.MainActivity { *; }
-keep class com.myapplication.GowaykiApp { *; }
-keep class pe.edu.tecsup.gowayki.domain.planner.** { *; }
-keep class pe.edu.tecsup.gowayki.domain.tracking.TripTracker { *; }

# Credential Manager: el proveedor de Play Services se instancia por reflexión.
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# MapLibre: clases y callbacks invocados desde código nativo (JNI) y GeoJSON vía Gson.
-keep class org.maplibre.android.** { *; }
-keep class org.maplibre.geojson.** { *; }
-dontwarn org.maplibre.**

# Firebase/Firestore leen y escriben mapas (sin POJOs propios); sus AAR traen sus reglas.
-dontwarn com.google.firebase.**
