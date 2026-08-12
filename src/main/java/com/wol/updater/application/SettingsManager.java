package com.wol.updater.application;

import com.wol.updater.domain.UserSettings;

public interface SettingsManager {
    UserSettings loadSettings();
    void saveSettings(UserSettings settings);
}
