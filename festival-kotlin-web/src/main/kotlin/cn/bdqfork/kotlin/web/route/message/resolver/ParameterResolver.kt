package cn.bdqfork.kotlin.web.route.message.resolver

import io.vertx.ext.web.RoutingContext
import java.lang.reflect.Parameter

/**
 * @author bdq
 * @since 2020/2/11
 */
interface ParameterResolver {
    @Throws(Exception::class)
    fun resolve(parameter: Parameter, routingContext: RoutingContext): Any?
}