package com.gov.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gov.security.sm4")
public class SmCryptoProperties {

    private String key = "";

    private boolean enabled = true;

    private boolean allowPlaintextFallback = true;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAllowPlaintextFallback() {
        return allowPlaintextFallback;
    }

    public void setAllowPlaintextFallback(boolean allowPlaintextFallback) {
        this.allowPlaintextFallback = allowPlaintextFallback;
    }
}
