package com.gov.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bootstrap initialization behavior for first startup.
 */
@ConfigurationProperties(prefix = "gov.bootstrap")
public class BootstrapProperties {

    private boolean enabled = true;

    private boolean forceResetAdminOnFirstStart = true;

    private Admin admin = new Admin();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isForceResetAdminOnFirstStart() {
        return forceResetAdminOnFirstStart;
    }

    public void setForceResetAdminOnFirstStart(boolean forceResetAdminOnFirstStart) {
        this.forceResetAdminOnFirstStart = forceResetAdminOnFirstStart;
    }

    public Admin getAdmin() {
        return admin;
    }

    public void setAdmin(Admin admin) {
        this.admin = admin;
    }

    public static class Admin {

        private String username = "admin";

        private String initialPassword = "admin123";

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getInitialPassword() {
            return initialPassword;
        }

        public void setInitialPassword(String initialPassword) {
            this.initialPassword = initialPassword;
        }
    }
}
