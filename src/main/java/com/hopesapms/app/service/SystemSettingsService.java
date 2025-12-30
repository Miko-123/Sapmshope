package com.hopesapms.app.service;

import org.springframework.stereotype.Service;

@Service
public class SystemSettingsService {
    private boolean maintenanceMode = false;

    public boolean isMaintenanceMode() {
        return maintenanceMode;
    }

    public void setMaintenanceMode(boolean maintenanceMode) {
        this.maintenanceMode = maintenanceMode;
    }
}
