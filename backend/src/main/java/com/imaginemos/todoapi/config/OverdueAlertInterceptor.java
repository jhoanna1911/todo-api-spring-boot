package com.imaginemos.todoapi.config;

import com.imaginemos.todoapi.service.TaskService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class OverdueAlertInterceptor implements HandlerInterceptor {

    private final TaskService taskService;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        long count = taskService.countOverdue();
        response.setHeader("X-Overdue-Count", String.valueOf(count));
        if (count > 0) {
            response.setHeader("X-Overdue-Alert",
                    "Tienes " + count + " tarea(s) programada(s) cuya fecha ya vencio y siguen sin iniciar");
        }
        return true;
    }
}
