package com.github.eugenemsv.amqp.rabbit.properties;

public class AmqpRetryProperties {

    /**
     * Whether or not publishing retries are enabled.
     */
    private boolean enabled;

    /**
     * Maximum number of attempts to publish or deliver a message.
     */
    private int maxAttempts = 3;

    /**
     * Interval between the first and second attempt to publish or deliver a message.
     */
    private long initialInterval = 1000L;

    /**
     * A multiplier to apply to the previous retry interval.
     */
    private double multiplier = 1.0;

    /**
     * Maximum interval between attempts.
     */
    private long maxInterval = 10000L;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public long getInitialInterval() {
        return initialInterval;
    }

    public void setInitialInterval(long initialInterval) {
        this.initialInterval = initialInterval;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    public long getMaxInterval() {
        return maxInterval;
    }

    public void setMaxInterval(long maxInterval) {
        this.maxInterval = maxInterval;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{enabled=");
        sb.append(enabled);
        sb.append(", maxAttempts=").append(maxAttempts);
        sb.append(", initialInterval=").append(initialInterval);
        sb.append(", multiplier=").append(multiplier);
        sb.append(", maxInterval=").append(maxInterval);
        sb.append('}');
        return sb.toString();
    }
}
