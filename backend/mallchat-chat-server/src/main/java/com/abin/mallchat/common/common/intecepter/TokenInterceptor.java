package com.abin.mallchat.common.common.intecepter;

import com.abin.mallchat.common.common.constant.MDCKey;
import com.abin.mallchat.common.common.exception.HttpErrorEnum;
import com.abin.mallchat.common.user.service.LoginService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Objects;
import java.util.Optional;

@Order(-2)
@Slf4j
@Component
public class TokenInterceptor implements HandlerInterceptor {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String AUTHORIZATION_SCHEMA = "Bearer ";
    public static final String ATTRIBUTE_UID = "uid";

    @Autowired
    private LoginService loginService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 添加详细调试信息
        System.out.println("TokenInterceptor被调用: " + request.getRequestURI());
        log.error("TokenInterceptor处理请求: " + request.getRequestURI()); // 使用ERROR级别确保一定会显示


        //获取用户登录token
        String token = getToken(request);
        Long validUid = loginService.getValidUid(token);

        if (Objects.nonNull(validUid)) {//有登录态
            request.setAttribute(ATTRIBUTE_UID, validUid);
            MDC.put(MDCKey.UID, String.valueOf(validUid));
        } else {
            boolean isPublicURI = isPublicURI(request.getRequestURI());
            if (!isPublicURI) {//又没有登录态，又不是公开路径，直接401
                HttpErrorEnum.ACCESS_DENIED.sendHttpError(response);
                return false;
            }
            // 对于公开路径，使用默认值
            MDC.put(MDCKey.UID, "0");
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        MDC.remove(MDCKey.UID);
    }

    /**
     * 判断是不是公共方法，可以未登录访问的
     *
     * @param requestURI
     */
    private boolean isPublicURI(String requestURI) {
    // 更精确的匹配方式，使用startsWith或正则表达式
    if (requestURI.startsWith("/capi/user/login/")) {
        return true;
    }

    // 输出日志，便于调试
    log.debug("检查公开路径: {}", requestURI);

    String[] split = requestURI.split("/");
    return split.length > 3 && "public".equals(split[3]);
    }

    private String getToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        return Optional.ofNullable(header)
                .filter(h -> h.startsWith(AUTHORIZATION_SCHEMA))
                .map(h -> h.substring(AUTHORIZATION_SCHEMA.length()))
                .orElse(null);
    }
}