package com.github.eugenemsv.amqp.rabbit.properties;

public class AmqpDeadLetterProperties {

    private boolean bidirectional;

    private Integer timeToLive = 3_600_000;

    public boolean isBidirectional() {
        return bidirectional;
    }

    public void setBidirectional(boolean bidirectional) {
        this.bidirectional = bidirectional;
    }

    public Integer getTimeToLive() {
        return timeToLive;
    }

    public void setTimeToLive(Integer timeToLive) {
        this.timeToLive = timeToLive;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{bidirectional=");
        sb.append(bidirectional);
        sb.append(", timeToLive=").append(timeToLive);
        sb.append('}');
        return sb.toString();
    }
}
