package org.amv.access.api;

import lombok.extern.slf4j.Slf4j;
import org.amv.access.api.ErrorResponseDto.ErrorInfoDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.lang.reflect.UndeclaredThrowableException;

@Slf4j
@RestControllerAdvice
public class AmvAccessRestExceptionHandlerAdvice extends ResponseEntityExceptionHandler {


    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
                                                             HttpHeaders headers, HttpStatus status, WebRequest request) {
        log.error("", ex);

        Exception e = unwrapUndeclaredThrowableExceptionIfNecessary(ex);

        ErrorInfoDto errorInfoDto = ErrorInfoDto.builder()
                .title(e.getClass().getSimpleName())
                .detail(e.getMessage())
                .build();

        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .addError(errorInfoDto)
                .build();

        return new ResponseEntity<>(errorResponseDto, headers, status);
    }

    /**
     * When using jdk dynamic proxies it is possible that a method throws an
     * exception which is not declared in its method declaration.
     * <p>
     * If such an exception is thrown it will be wrapped in an
     * {@link java.lang.reflect.UndeclaredThrowableException}.
     * <p>
     * This method will unwrap such exceptions and return the original culprit.
     *
     * @param e An exception you want conditionally unwrapped
     * @return The unwrapped exception if necessary otherwise the provided exception
     */
    private Exception unwrapUndeclaredThrowableExceptionIfNecessary(Exception e) {
        if (UndeclaredThrowableException.class.isAssignableFrom(e.getClass())) {
            return (Exception) e.getCause();
        }
        return e;
    }
}

        /*extends DefaultHandlerExceptionResolver {

    public static class AmvException extends Exception {

    }

    @Override
    protected ModelAndView doResolveException(HttpServletRequest request, HttpServletResponse response,
                                              Object handler, Exception ex) {
        try {
            Throwable t = unwrapUndeclaredThrowableExceptionIfNecessary(ex);
            boolean isAmvException = t.getClass().isAssignableFrom(AmvException.class);

            if (isAmvException) {
                ModelAndView mv = new ModelAndView();
                mv.setView(new MappingJackson2JsonView());

                mv.addObject("timestamp", System.currentTimeMillis());
                mv.addObject("dateTime", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
                mv.addObject("message", StringEscapeUtils.escapeEcmaScript(t.getLocalizedMessage()));
                mv.addObject("url", request.getRequestURL().toString());
                mv.addObject("exception", t.getClass());

                return mv;
            }
        } catch (Exception e) {
            log.error("", e);
        }

        return super.doResolveException(request, response, handler, ex);
    }

}*/
