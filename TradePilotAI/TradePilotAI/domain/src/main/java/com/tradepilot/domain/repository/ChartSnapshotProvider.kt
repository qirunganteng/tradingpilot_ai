package com.tradepilot.domain.repository

interface ChartSnapshotProvider {
    suspend fun captureCurrentChart(): ByteArray?
}
