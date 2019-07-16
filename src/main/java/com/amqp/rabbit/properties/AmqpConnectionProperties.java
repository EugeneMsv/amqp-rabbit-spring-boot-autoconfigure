package com.amqp.rabbit.properties;

public class AmqpConnectionProperties {

    private String connectionKey;

    private String host;

    private int port;

    private String user;

    private String password;

    private String vHost;

    public AmqpConnectionProperties(String host, int port, String user, String password, String vHost) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.vHost = vHost;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConnectionKey() {
        return connectionKey;
    }

    public void setConnectionKey(String connectionKey) {
        this.connectionKey = connectionKey;
    }

    public String getvHost() {
        return vHost;
    }

    public void setvHost(String vHost) {
        this.vHost = vHost;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{host='");
        sb.append(host).append('\'');
        sb.append(", port=").append(port);
        sb.append(", user='").append(user).append('\'');
        sb.append(", password='").append(password).append('\'');
        sb.append(", vHost='").append(vHost).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
