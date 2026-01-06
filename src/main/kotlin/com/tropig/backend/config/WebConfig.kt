package com.tropig.backend.config

import com.tropig.backend.common.handler.LoginMemberArgumentResolver
import com.tropig.backend.common.handler.RequireAuthInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val loginMemberArgumentResolver: LoginMemberArgumentResolver,
    private val requireAuthInterceptor: RequireAuthInterceptor,
) : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        println("### register resolver = ${loginMemberArgumentResolver::class.qualifiedName} @${System.identityHashCode(loginMemberArgumentResolver)}")
        resolvers.add(loginMemberArgumentResolver)
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(requireAuthInterceptor)
            .addPathPatterns("/**")
    }
}