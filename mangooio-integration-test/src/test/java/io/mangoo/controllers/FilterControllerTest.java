package io.mangoo.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import org.junit.Test;

import io.mangoo.test.http.WebRequest;
import io.mangoo.test.http.WebResponse;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

/**
 * 
 * @author svenkubiak
 *
 */
public class FilterControllerTest {

    @Test
    public void testFilterWithAdditionalContent() {
        //given
        WebResponse response = WebRequest.get("/filter").execute();

        //then
        assertThat(response, not(nullValue()));
        assertThat(response.getStatusCode(), equalTo(StatusCodes.OK));
        assertThat(response.getContent(), equalTo("bar"));
    }

    @Test
    public void testFilterWithAdditionalHeader() {
        //given
        WebResponse response = WebRequest.get("/headerfilter").execute();

        //then
        assertThat(response, not(nullValue()));
        assertThat(response.getStatusCode(), equalTo(StatusCodes.OK));
        assertThat(response.getHeader(Headers.CONTENT_MD5_STRING), equalTo("12"));
    }
    
    @Test
    public void testMultipleFilters() {
        //given
        WebResponse response = WebRequest.get("/filters").execute();

        //then
        assertThat(response, not(nullValue()));
        assertThat(response.getStatusCode(), equalTo(StatusCodes.OK));
        assertThat(response.getContent(), equalTo("filteronefiltertwofilterthree"));
    }
}