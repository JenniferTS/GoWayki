# Emulador Android en el servidor del grupo

## Limitación

El host es una **VM sin virtualización anidada** (`emulator-check accel` → sin KVM/vmx/svm).  
Emulator 37+ exige aceleración por hardware **o** la variable:

```bash
export ANDROID_I_WANT_MY_TCG=yes
```

## Arranque TCG (smoke)

AVD: `gowayki_api35` (API 35, x86_64, Pixel 6). RAM del AVD ~1024 MB.

```bash
export ANDROID_HOME=/home/agentes/android-sdk
cd ~/workspace/gowayki-kmp
./tool/run-emulator-tcg.sh
./tool/install-apk.sh
```

Log: `/tmp/emu-gowayki.log`.

En pruebas 2026-11-22 el proceso llegó a `adb offline` y **no completó boot en ~3 min** con TCG. Puede llegar a `device` si se espera mucho más, pero **no es práctico** para desarrollo diario.

## Alternativa recomendada: teléfono

1. En el teléfono: Opciones de desarrollador → Depuración USB (o depuración sin cable).  
2. `adb devices` debe listar `device`.  
3. `./tool/install-apk.sh`

## Por qué no “arreglar” KVM aquí

Hace falta que el hipervisor del proveedor habilite nested virtualization y que `/dev/kvm` exista para el usuario `agentes`. Sin eso, TCG (smoke lento) o un dispositivo físico son las únicas vías.
