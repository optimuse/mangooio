package io.mangoo.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import org.junit.Test;

import com.google.common.net.MediaType;

import io.mangoo.test.http.WebRequest;
import io.mangoo.test.http.WebResponse;
import io.undertow.util.StatusCodes;

/**
 *
 * @author svenkubiak
 *
 */
public class JsonControllerTest {
    private static final String json = "{\"firstname\":\"Peter\",\"lastname\":\"Parker\",\"age\":24}";

    @Test
    public void testJsonSerialization() {
        //given
        WebResponse response = WebRequest.get("/render").execute();

        //then
        assertThat(response, not(nullValue()));
        assertThat(response.getStatusCode(), equalTo(StatusCodes.OK));
        assertThat(response.getContent(), equalTo(json));
    }

    @Test
    public void testJsonParsingPost() {
        //given
        WebResponse response = WebRequest.post("/parse")
                .withContentType(MediaType.JSON_UTF_8.withoutParameters().toString())
                .withRequestBody(json)
                .execute();

        //then
        assertThat(response, not(nullValue()));
        assertThat(response.getStatusCode(), equalTo(StatusCodes.OK));
        assertThat(response.getContent(), equalTo("Peter;Parker;24"));
    }
    
    @Test
    public void testJsonParsingPut() {
        //given
        WebResponse response = WebRequest.put("/parse")
                .withContentType(MediaType.JSON_UTF_8.withoutParameters().toString())
                .withRequestBody(json)
                .execute();

        //then
        assertThat(response, not(nullValue()));
        assertThat(response.getStatusCode(), equalTo(StatusCodes.OK));
        assertThat(response.getContent(), equalTo("Peter;Parker;24"));
    }
    
    @Test
    public void testJsonParsingPatch() {
        //given
        WebResponse response = WebRequest.patch("/parse")
                .withContentType(MediaType.JSON_UTF_8.withoutParameters().toString())
                .withRequestBody(json)
                .execute();

        //then
        assertThat(response, not(nullValue()));
        assertThat(response.getStatusCode(), equalTo(StatusCodes.OK));
        assertThat(response.getContent(), equalTo("Peter;Parker;24"));
    }

    @Test
    public void testJsonEmptyResponseBody() {
        //given
        WebResponse response = WebRequest.post("/body")
                .withContentType(MediaType.JSON_UTF_8.withoutParameters().toString())
                .withRequestBody("")
                .execute();

        //then
        assertThat(response, not(nullValue()));
        assertThat(response.getStatusCode(), equalTo(StatusCodes.OK));
        assertThat(response.getContent(), not(nullValue()));
    }
    
    @Test
    public void testJsonNullResponseBody() {
        //given
        WebResponse response = WebRequest.post("/body")
                .withContentType(MediaType.JSON_UTF_8.withoutParameters().toString())
                .withRequestBody(null)
                .execute();

        //then
        assertThat(response, not(nullValue()));
        assertThat(response.getStatusCode(), equalTo(StatusCodes.OK));
        assertThat(response.getContent(), not(nullValue()));
    }
    
    @Test
    public void testJsonResponseBody() {
        //given
        WebResponse response = WebRequest.post("/body")
                .withContentType(MediaType.JSON_UTF_8.withoutParameters().toString())
                .withRequestBody(json)
                .execute();

        //then
        assertThat(response, not(nullValue()));
        assertThat(response.getStatusCode(), equalTo(StatusCodes.OK));
        assertThat(response.getContent(), equalTo("/body"));
    }

    @Test
    public void testJsonRequestBodyPost() {
        //given
        WebResponse response = WebRequest.post("/requestAndJson")
                .withContentType(MediaType.JSON_UTF_8.withoutParameters().toString())
                .withRequestBody(json)
                .execute();

        //then
        assertThat(response, not(nullValue()));
        assertThat(response.getStatusCode(), equalTo(StatusCodes.OK));
        assertThat(response.getContent(), equalTo("/requestAndJsonPeter"));
    }
}