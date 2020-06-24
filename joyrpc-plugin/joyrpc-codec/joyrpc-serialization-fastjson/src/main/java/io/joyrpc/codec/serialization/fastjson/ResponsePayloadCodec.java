package io.joyrpc.codec.serialization.fastjson;

import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.JSONLexer;
import com.alibaba.fastjson.parser.JSONToken;
import com.alibaba.fastjson.parser.deserializer.AutowiredObjectDeserializer;
import com.alibaba.fastjson.serializer.AutowiredObjectSerializer;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import io.joyrpc.exception.SerializerException;
import io.joyrpc.protocol.message.ResponseMessage;
import io.joyrpc.protocol.message.ResponsePayload;
import io.joyrpc.util.ClassUtils;
import io.joyrpc.util.GenericType;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import static io.joyrpc.protocol.message.ResponsePayload.*;
import static io.joyrpc.util.ClassUtils.getCanonicalName;
import static io.joyrpc.util.GenericMethod.getReturnGenericType;

public class ResponsePayloadCodec extends AbstractSerializer implements AutowiredObjectSerializer, AutowiredObjectDeserializer {

    public static final ResponsePayloadCodec INSTANCE = new ResponsePayloadCodec();

    @Override
    public int getFastMatchToken() {
        return JSONToken.LBRACE;
    }

    @Override
    public Set<Type> getAutowiredFor() {
        Set<Type> result = new HashSet<>(1);
        result.add(ResponsePayload.class);
        return result;
    }

    @Override
    public <T> T deserialze(final DefaultJSONParser parser, final Type type, final Object fieldName) {
        JSONLexer lexer = parser.getLexer();
        switch (lexer.token()) {
            case JSONToken.NULL:
                lexer.nextToken();
                return null;
            case JSONToken.LBRACE:
                return (T) parse(parser, lexer);
            default:
                return null;
        }
    }

    protected ResponsePayload parse(final DefaultJSONParser parser, final JSONLexer lexer) {
        ResponsePayload payload = new ResponsePayload();
        String key;
        int token;
        try {
            String responseClz = null;
            for (; ; ) {
                // lexer.scanSymbol
                key = lexer.scanSymbol(parser.getSymbolTable());
                if (key == null) {
                    token = lexer.token();
                    if (token == JSONToken.RBRACE) {
                        lexer.nextToken(JSONToken.COMMA);
                        break;
                    } else if (token == JSONToken.COMMA) {
                        if (lexer.isEnabled(Feature.AllowArbitraryCommas)) {
                            continue;
                        }
                    }
                }
                lexer.nextTokenWithColon(JSONToken.LITERAL_STRING);
                if (RES_CLASS.equals(key)) {
                    responseClz = parseString(lexer, RES_CLASS, false);
                } else if (RESPONSE.equals(key)) {
                    Type returnType = getReturnGenericType(responseClz);
                    returnType = returnType == null ? ClassUtils.getClass(responseClz) : returnType;
                    payload.setResponse(parseObject(parser, lexer, returnType));
                } else if (EXCEPTION.equals(key)) {
                    payload.setResponse(parseObject(parser, lexer, ClassUtils.getClass(responseClz)));
                }
                if (lexer.token() == JSONToken.RBRACE) {
                    lexer.nextToken(JSONToken.COMMA);
                    break;
                }
            }
            return payload;
        } catch (ClassNotFoundException e) {
            throw new SerializerException(e.getMessage());
        }
    }

    @Override
    public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
        SerializeWriter out = serializer.getWriter();
        if (object == null) {
            out.writeNull();
            return;
        }
        out.append("{");
        ResponsePayload payload = (ResponsePayload) (object instanceof ResponseMessage ? ((ResponseMessage) object).getPayLoad() : object);
        if (payload != null) {
            Throwable exception = payload.getException();
            Object response = payload.getResponse();
            if (response != null) {
                GenericType returnType = payload.getReturnType();
                String responseTypeName = returnType == null ? getCanonicalName(response.getClass())
                        : returnType.getGenericType().getTypeName();
                write(serializer, RES_CLASS, responseTypeName, AFTER);
                write(serializer, RESPONSE, response, NONE);
            } else if (exception != null) {
                write(serializer, RES_CLASS, getCanonicalName(exception.getClass()), AFTER);
                write(serializer, EXCEPTION, exception, NONE);
            }
        }
        out.append("}");
    }
}
