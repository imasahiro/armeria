package com.linecorp.armeria.example.http.thrift;

import static com.linecorp.armeria.common.MediaType.JSON_UTF_8;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.linecorp.armeria.common.http.DefaultHttpResponse;
import com.linecorp.armeria.common.http.HttpData;
import com.linecorp.armeria.common.http.HttpHeaderNames;
import com.linecorp.armeria.common.http.HttpHeaders;
import com.linecorp.armeria.common.http.HttpResponse;
import com.linecorp.armeria.common.http.HttpStatus;
import com.linecorp.armeria.server.http.dynamic.ResponseConverter;

public class JacksonResponseConverter implements ResponseConverter {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public HttpResponse convert(Object resObj) throws Exception {
        final DefaultHttpResponse res = new DefaultHttpResponse();
        final String json = OBJECT_MAPPER.writeValueAsString(resObj);
        res.write(HttpHeaders.of(HttpStatus.OK)
                             .set(HttpHeaderNames.CONTENT_TYPE, JSON_UTF_8.toString())
                             .setInt(HttpHeaderNames.CONTENT_LENGTH, json.length()));
        res.write(HttpData.ofUtf8(json));
        return res;
    }
}
