package com.pot.zing.framework.common.handler;

import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Default global exception advice used by modules that do not provide their own
 * extension. Registered by {@code CommonAutoConfiguration} when no other
 * {@code BaseGlobalExceptionHandler} bean is present.
 */
@RestControllerAdvice
public class DefaultGlobalExceptionHandler extends BaseGlobalExceptionHandler {
}