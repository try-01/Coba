package com.samsungremote

/**
 * Complete set of Samsung Tizen remote control key codes.
 * Maps human-readable key names to the protocol-level string codes
 * that the TV's WSS endpoint expects in `DataOfCmd`.
 */
enum class SamsungRemoteKey(val code: String) {

    // ── Power ────────────────────────────────────────────────
    POWER("KEY_POWER"),

    // ── Volume ───────────────────────────────────────────────
    VOLUME_UP("KEY_VOLUP"),
    VOLUME_DOWN("KEY_VOLDOWN"),
    MUTE("KEY_MUTE"),

    // ── Channel ──────────────────────────────────────────────
    CHANNEL_UP("KEY_CHUP"),
    CHANNEL_DOWN("KEY_CHDOWN"),

    // ── D-Pad ────────────────────────────────────────────────
    UP("KEY_UP"),
    DOWN("KEY_DOWN"),
    LEFT("KEY_LEFT"),
    RIGHT("KEY_RIGHT"),
    ENTER("KEY_ENTER"),
    BACK("KEY_RETURN"),

    // ── Navigation / System ──────────────────────────────────
    HOME("KEY_HOME"),
    MENU("KEY_MENU"),
    SOURCE("KEY_SOURCE"),
    EXIT("KEY_EXIT"),
    GUIDE("KEY_GUIDE"),
    INFO("KEY_INFO"),

    // ── Numeric keypad ───────────────────────────────────────
    KEY_0("KEY_0"),
    KEY_1("KEY_1"),
    KEY_2("KEY_2"),
    KEY_3("KEY_3"),
    KEY_4("KEY_4"),
    KEY_5("KEY_5"),
    KEY_6("KEY_6"),
    KEY_7("KEY_7"),
    KEY_8("KEY_8"),
    KEY_9("KEY_9"),

    // ── Media transport ──────────────────────────────────────
    PLAY("KEY_PLAY"),
    PAUSE("KEY_PAUSE"),
    STOP("KEY_STOP"),
    FAST_FORWARD("KEY_FF"),
    REWIND("KEY_REW"),
    RECORD("KEY_REC"),

    // ── Smart Hub ────────────────────────────────────────────
    SMART_HUB("KEY_CONTENT"),
    NETFLIX("KEY_NETFLIX"),
    PRIME_VIDEO("KEY_PRIME_VIDEO"),

    // ── Trackpad / Pointer ───────────────────────────────────
    MOUSE_LEFT_CLICK("KEY_MOUSE_DOWN"),
    MOUSE_LEFT_RELEASE("KEY_MOUSE_UP"),
    MOUSE_MOVE("KEY_MOUSE_MOVE"),

    // ── Text input ───────────────────────────────────────────
    KEYBOARD("KEY_KEYBOARD"),
    KEYBOARD_CAPS("KEY_CAPS"),
    KEYBOARD_DONE("KEY_DONE"),
}
