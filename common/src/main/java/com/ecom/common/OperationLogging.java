package com.ecom.common;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.*;
import org.springframework.stereotype.Component;
@Aspect @Component
public class OperationLogging {
    private static final Logger log = LoggerFactory.getLogger(OperationLogging.class);
    @Around("execution(public * com.ecom..*Service.*(..))")
    public Object log(ProceedingJoinPoint call) throws Throwable {
        long start=System.nanoTime();
        try { Object result=call.proceed(); log.info("operation={} outcome=success durationMs={} traceId={}",call.getSignature().toShortString(),(System.nanoTime()-start)/1_000_000,MDC.get("traceId")); return result; }
        catch(Throwable e) { log.warn("operation={} outcome=failure type={} durationMs={} traceId={}",call.getSignature().toShortString(),e.getClass().getSimpleName(),(System.nanoTime()-start)/1_000_000,MDC.get("traceId")); throw e; }
    }
}
