package org.amv.access.api;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.UndeclaredThrowableException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@ControllerAdvice(
        annotations = RestController.class
)
public class RestCtrlExceptionHandlerAdvice extends DefaultHandlerExceptionResolver {

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


    /**
     * When using jdk dynamic proxies it is possible that a method throws an
     * exception which is not declared in its method declaration.
     * <p>
     * If such an exception is thrown it will be wrapped in an
     * {@link java.lang.reflect.UndeclaredThrowableException}.
     * <p>
     * This method will unwrap such exceptions and return the original culprit.
     *
     * @param t An exception you want conditionally unwrapped
     * @return The unwrapped exception if necessary otherwise the provided exception
     */
    private Throwable unwrapUndeclaredThrowableExceptionIfNecessary(Throwable t) {
        if (t instanceof UndeclaredThrowableException) {
            return t.getCause();
        }
        return t;
    }
}
