package com.bds.chat.infrastructure.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.broker.BrokerAvailabilityEvent;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
public class BrokerAvailabilityListener implements ApplicationListener<BrokerAvailabilityEvent> {
    private final AtomicBoolean available = new AtomicBoolean(false);

    @Override
    public void onApplicationEvent(BrokerAvailabilityEvent event) {
        boolean now = event.isBrokerAvailable();
        boolean before = available.getAndSet(now);
        if(now==before){
            return;
        }
        if(now){
            log.info("[BROKER-UP] STOMP relay 연결 복구 — 브로드캐스트 정상화");
        }else{
            log.error("[BROKER-DOWN] STOMP relay 연결 끊김 — echo 불가, 클라이언트 재전송 증가 예상");
        }
    }

    //health check
    public boolean isAvailable() {
        return available.get();
    }
}
