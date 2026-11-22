package pe.edu.tecsup.gowayki.domain.planner

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

internal actual fun wallClockEpochMillis(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()
