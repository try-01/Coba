package com.tvhanan.domain.model

/**
 * App ID Samsung Smart Hub. ID ini TIDAK resmi didokumentasikan Samsung
 * dan bisa berubah/berbeda per region & firmware — nilai di sini adalah
 * ID yang paling umum dilaporkan bekerja di TV Tizen 2019-2022 (termasuk
 * N-series 2020 seperti UA32N4300). Kalau tidak berfungsi di unit kamu,
 * app perlu request daftar app terinstall ke TV (event ed.installedApp.get)
 * untuk menemukan appId yang benar-benar cocok.
 */
enum class AppShortcut(val appId: String, val label: String) {
    NETFLIX("11101200001", "Netflix"),
    PRIME_VIDEO("3201512006785", "Prime Video"),
    YOUTUBE("111299001912", "YouTube")
}