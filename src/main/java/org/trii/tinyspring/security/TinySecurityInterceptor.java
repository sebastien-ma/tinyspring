package org.trii.tinyspring.security;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created with IntelliJ IDEA.
 * User: Sebastian MA
 * Date: August 10, 2014
 * Time: 15:02
 */
public class TinySecurityInterceptor extends HandlerInterceptorAdapter {

	private static final Logger log = LoggerFactory.getLogger(TinySecurityInterceptor.class);

	private String accessDeniedUrl;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object
			handler) throws Exception {

		if(handler instanceof ResourceHttpRequestHandler) {
			return true;
		}

		if(handler instanceof HandlerMethod) {
			HandlerMethod handlerMethod = (HandlerMethod) handler;

			SecurityCheck classAnnotation
					= handlerMethod.getMethod().getDeclaringClass().getAnnotation(SecurityCheck
					.class);
			SecurityCheck methodAnnotation = handlerMethod.getMethodAnnotation(SecurityCheck
					.class);
			String entry = request.getRequestURI().replace(request.getContextPath(), "");

			if(methodAnnotation != null) {

				if(!methodAnnotation.value()) {
					log.debug("Security Check for [{}] disabled", entry);
				} else {
					log.debug("Security Check for [{}]", entry);
					log.debug("require role [{}]", StringUtils.join(methodAnnotation.requireRole(),
							"," +
									""));
					log.debug("require privilege [{}]", methodAnnotation.requirePrivilege());
					TinyUser user
							= (TinyUser) request.getSession().getAttribute(TinyAuthenticator
							.SESSION_NAME_USER);
					if(user == null) {
						request.setAttribute("roleDenied", true);
						request.setAttribute("privilegeDenied", true);
						RequestDispatcher rd = request.getRequestDispatcher(accessDeniedUrl);
						rd.forward(request, response);
					}
				}
			} else {
				if(classAnnotation == null) {
					log.debug("No @SecurityCheck found for [{}]", entry);
				} else {
					String[] matches = classAnnotation.matches();
					String[] excludes = classAnnotation.excludes();

					boolean requireCheck = false;

					for(String match : matches) {
						if(match.contains("**")) {
							match = match.replace("**", ".*");
						} else {
							match = match.replace("*", "[^/]*");
						}
						if(entry.matches(match)) {
							requireCheck = true;
						}

						if(requireCheck) {
							for(String exclude : excludes) {
								if(exclude.contains("**")) {
									exclude = exclude.replace("**", ".*");
								} else {
									exclude = exclude.replace("*", "[^/]*");
								}
								if(request.getRequestURI().matches(exclude)) {
									requireCheck = false;
									break;
								}
							}
						}
					}
					if(requireCheck) {
						log.debug("Security Check for [{}]", request.getRequestURI());
						log.debug("require role [{}]", StringUtils.join(classAnnotation
								.requireRole(), ","));
						log.debug("require privilege [{}]", classAnnotation.requirePrivilege());
						TinyUser user
								= (TinyUser) request.getSession().getAttribute(TinyAuthenticator
								.SESSION_NAME_USER);
						if(user == null) {
							request.setAttribute("roleDenied", true);
							request.setAttribute("privilegeDenied", true);
							RequestDispatcher rd = request.getRequestDispatcher(accessDeniedUrl);
							rd.forward(request, response);
						}
					}


				}
			}
		}
		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response,
	                       Object handler, ModelAndView modelAndView) throws Exception {

	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
	                            Object handler, Exception ex) throws Exception {

	}

	public String getAccessDeniedUrl() {

		return accessDeniedUrl;
	}

	public void setAccessDeniedUrl(String accessDeniedUrl) {

		this.accessDeniedUrl = accessDeniedUrl;
	}
}