package io.joyrpc.transport.resteasy.mapper;

/*-
 * #%L
 * joyrpc
 * %%
 * Copyright (C) 2019 joyrpc.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.jboss.resteasy.logging.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

/**
 * IllegalArgumentException异常处理Mapper
 */
public class IllegalArgumentExceptionMapper implements ExceptionMapper {

    private final static Logger logger = Logger.getLogger(IllegalArgumentExceptionMapper.class);

    public static IllegalArgumentExceptionMapper mapper = new IllegalArgumentExceptionMapper();

    private IllegalArgumentExceptionMapper() {

    }

    @Override
    public Response toResponse(Throwable throwable) {

        logger.error("Unexpected", throwable);

        String errorMsg = "{\"code\":400, \"message\":\"" + throwable.getMessage() + "\"}";
        Response response = Response.status(BAD_REQUEST).entity(errorMsg).build();
        return response;
    }
}
