package com.bds.chat.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("bds.msa-broker")
public class MsaBrokerProperties {

    private String host = "localhost";
    private int port = 5672;
    private String virtualHost = "msa";
    private String username;
    private String password;

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getVirtualHost() { return virtualHost; }
    public void setVirtualHost(String virtualHost) { this.virtualHost = virtualHost; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}