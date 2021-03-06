package cn.bdqfork.kotlin.web.route.response

import cn.bdqfork.core.util.StringUtils
import cn.bdqfork.kotlin.web.constant.ContentType
import java.util.concurrent.ConcurrentHashMap

/**
 * @author bdq
 * @since 2020/2/15
 */
open class ResponseHandlerFactory {
    open val responseHandlerMap: MutableMap<String, ResponseHandleStrategy?> = ConcurrentHashMap(16)

    fun registerResponseHandler(contentType: String, responseHandleStrategy: ResponseHandleStrategy) {
        responseHandlerMap[contentType] = responseHandleStrategy
    }

    fun getResponseHandler(contentType: String?): ResponseHandleStrategy {
        if (StringUtils.isEmpty(contentType) || !responseHandlerMap.containsKey(contentType)) {
            return responseHandlerMap[ContentType.JSON]!!
        }
        return responseHandlerMap[contentType]!!
    }

    init {
        registerResponseHandler(ContentType.PLAIN, TextPlainResponseHandler())
        registerResponseHandler(ContentType.JSON, JsonResponseHandler())
        registerResponseHandler(ContentType.XML, XmlResponseHandler())
    }
}