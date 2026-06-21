package com.seckill.api;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;



@org.springframework.stereotype.Component
public class Component {
    @Autowired
    private static final Logger log = LoggerFactory.getLogger(Component.class);
    @EventListener
    public void aaa(UserRegisteredEvent event){
        log.debug("Component");
    }
}
