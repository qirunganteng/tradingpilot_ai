#!/bin/bash
# Verifikasi cepat logika domain layer TANPA perlu Android SDK/Gradle penuh.
# Dipakai di CI (lihat .github/workflows/) sebagai gerbang cepat sebelum
# build Android yang jauh lebih berat & lama dijalankan.
set -e

KOTLIN_VERSION="2.0.20"
WORKDIR="$(mktemp -d)"
trap 'rm -rf "$WORKDIR"' EXIT

if ! command -v kotlinc &> /dev/null; then
  echo "kotlinc tidak ditemukan, mengunduh Kotlin compiler ${KOTLIN_VERSION}..."
  curl -sL -o "$WORKDIR/kotlin-compiler.zip" \
    "https://github.com/JetBrains/kotlin/releases/download/v${KOTLIN_VERSION}/kotlin-compiler-${KOTLIN_VERSION}.zip"
  unzip -q "$WORKDIR/kotlin-compiler.zip" -d "$WORKDIR"
  export PATH="$WORKDIR/kotlinc/bin:$PATH"
fi

STUB_DIR="$WORKDIR/stubs"
mkdir -p "$STUB_DIR/javax/inject" "$STUB_DIR/kotlinx/coroutines/flow"

cat > "$STUB_DIR/javax/inject/Inject.kt" << 'STUBEOF'
package javax.inject
@Target(AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION, AnnotationTarget.FIELD)
annotation class Inject
@Target(AnnotationTarget.CLASS)
annotation class Singleton
STUBEOF

cat > "$STUB_DIR/kotlinx/coroutines/flow/Flow.kt" << 'STUBEOF'
package kotlinx.coroutines.flow
interface Flow<out T> { suspend fun collect(collector: FlowCollector<T>) }
interface FlowCollector<in T> { suspend fun emit(value: T) }
interface StateFlow<out T> : Flow<T> { val value: T }
interface MutableStateFlow<T> : StateFlow<T> { override var value: T }
interface SharedFlow<out T> : Flow<T>
interface MutableSharedFlow<T> : SharedFlow<T> { suspend fun emit(value: T) }
fun <T> MutableStateFlow(value: T): MutableStateFlow<T> = throw NotImplementedError()
STUBEOF

OUT_DIR="$WORKDIR/out"
mkdir -p "$OUT_DIR"

echo "Compiling domain module + verify script..."
kotlinc $(find domain/src/main -name "*.kt") $(find "$STUB_DIR" -name "*.kt") scripts/verify-domain-logic.kt -d "$OUT_DIR"

echo "Running..."
KOTLIN_STDLIB=$(find "${KOTLINC_HOME:-$WORKDIR/kotlinc}" -name "kotlin-stdlib.jar" | head -1)
java -cp "$OUT_DIR:$KOTLIN_STDLIB" RunTestsKt
