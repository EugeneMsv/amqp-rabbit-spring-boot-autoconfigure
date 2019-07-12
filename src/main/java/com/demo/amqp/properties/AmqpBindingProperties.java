package com.demo.amqp.properties;


import java.util.Map;

public class AmqpBindingProperties {
  private Map<String, String> routingKeys;

  public Map<String, String> getRoutingKeys() {
    return routingKeys;
  }

  public void setRoutingKeys(Map<String, String> routingKeys) {
    this.routingKeys = routingKeys;
  }
}
