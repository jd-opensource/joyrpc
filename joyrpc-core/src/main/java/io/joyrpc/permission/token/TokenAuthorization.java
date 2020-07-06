package io.joyrpc.permission.token;

import io.joyrpc.InvokerAware;
import io.joyrpc.config.InterfaceOption.MethodOption;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.URL;
import io.joyrpc.permission.Authorization;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;

import static io.joyrpc.constants.Constants.HIDDEN_KEY_TOKEN;

/**
 * 基于令牌的方法权限认证
 */
@Extension(value = "token")
public class TokenAuthorization implements Authorization, InvokerAware {

    /**
     * URL
     */
    protected URL url;
    /**
     * 接口类，在泛型调用情况下，clazz和clazzName可能不相同
     */
    protected Class clazz;
    /**
     * 接口类名
     */
    protected String className;

    @Override
    public boolean authenticate(final RequestMessage<Invocation> request) {
        //方法鉴权
        Invocation invocation = request.getPayLoad();
        MethodOption option = request.getOption();
        String token = option.getToken();
        return token == null || token.isEmpty() || token.equals(invocation.getAttachment(HIDDEN_KEY_TOKEN));
    }

    @Override
    public void setUrl(final URL url) {
        this.url = url;
    }

    @Override
    public void setClass(final Class clazz) {
        this.clazz = clazz;
    }

    @Override
    public void setClassName(final String className) {
        this.className = className;
    }

}
