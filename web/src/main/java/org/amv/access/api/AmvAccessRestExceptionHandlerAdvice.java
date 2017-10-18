package org.amv.access.api;

import lombok.extern.slf4j.Slf4j;
import org.amv.access.api.ErrorResponseDto.ErrorInfoDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.util.NestedServletException;

import javax.servlet.ServletException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Optional;
import java.util.function.Predicate;

@Slf4j
@RestControllerAdvice
public class AmvAccessRestExceptionHandlerAdvice extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
                                                             HttpHeaders headers, HttpStatus status, WebRequest request) {
        log.error("", ex);

        Exception e = unwrapIfNecessary(ex);

        ErrorInfoDto errorInfoDto = ErrorInfoDto.builder()
                .title(e.getClass().getSimpleName())
                .detail(e.getMessage())
                .build();

        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .addError(errorInfoDto)
                .build();

        return new ResponseEntity<>(errorResponseDto, headers, status);
    }


    @ExceptionHandler({
            Exception.class
    })
    public final ResponseEntity<Object> handle(Exception ex, WebRequest request) {
        HttpHeaders headers = new HttpHeaders();

        Exception e = unwrapIfNecessary(ex);

        final HttpStatus status = findHttpStatus(ex)
                .orElseGet(() -> findHttpStatus(e).orElse(HttpStatus.INTERNAL_SERVER_ERROR));

        return handleExceptionInternal(e, null, headers, status, request);
    }

    private Optional<HttpStatus> findHttpStatus(Exception ex) {
        return Optional.ofNullable(ex.getClass().getAnnotation(ResponseStatus.class))
                .map(ResponseStatus::code);
    }

    private Exception unwrapIfNecessary(Exception e) {
        Exception e1 = unwrapUndeclaredThrowableExceptionIfNecessary(e);
        Exception e2 = unwrapIfAssignableFrom(e1, ServletException.class);
        Exception e3 = unwrapIfAssignableFrom(e2, NestedServletException.class);

        return e3;
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
        return unwrapIfAssignableFrom(e, UndeclaredThrowableException.class);
    }

    private Exception unwrapIfAssignableFrom(Exception e, Class<?> clazz) {
        return unwrapIfNecessary(e, t -> clazz.isAssignableFrom(t.getClass()));
    }

    private Exception unwrapIfNecessary(Exception e, Predicate<Exception> predicate) {
        if (predicate.test(e)) {
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
