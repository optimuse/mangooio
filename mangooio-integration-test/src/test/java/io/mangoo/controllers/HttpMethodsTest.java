package io.mangoo.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import org.junit.Test;

import io.mangoo.test.http.WebRequest;
import io.mangoo.test.http.WebResponse;
import io.undertow.util.StatusCodes;

/**
 * 
 * @author sven.kubiak
 *
 */
public class HttpMethodsTest {
    @Test
    public void testGet() {
        //given
        final WebResponse response = WebRequest.get("/").execute();

        //then
        assertThat(response, not(nullValue()));
        assertThat(response.getStatusCode(), equalTo(StatusCodes.OK));
    }
    
    @Test
    public void testPost() {
        //given
        final WebResponse response = WebRequest.post("/").execute();

        //then
        assertThat(response, not(nullValue()));
        assertThat(response.getStatusCode(), equalTo(StatusCodes.OK));
    }
    
    @Test
    public void testPut() {
        //given
        final WebResponse response = WebRequest.put("/").execute();

        //then
        assertThat(response, not(nullValue()));
        assertThat(response.getStatusCode(), equalTo(StatusCodes.OK));
    }

    @Test
    public void testHead() {
        //given
        final WebResponse response = WebRequest.head("/").execute();

        //then
        assertThat(response, not(nullValue()));
        assertThat(response.getStatusCode(), equalTo(StatusCodes.OK));
    }
    
    @Test
    public void testDelete() {
        //given
        final WebResponse response = WebRequest.delete("/").execute();

        //then
        assertThat(response, not(nullValue()));
        assertThat(response.getStatusCode(), equalTo(StatusCodes.OK));
    }
    
    @Test
    public void testOptions() {
        //given
        final WebResponse response = WebRequest.options("/").execute();

        //then
        assertThat(response, not(nullValue()));
        assertThat(response.getStatusCode(), equalTo(StatusCodes.OK));
    }
    
    @Test
    public void testPatch() {
        //given
        final WebResponse response = WebRequest.patch("/").execute();

        //then
        assertThat(response, not(nullValue()));
        assertThat(response.getStatusCode(), equalTo(StatusCodes.OK));
    }
}