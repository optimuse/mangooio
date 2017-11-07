package io.mangoo.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import org.junit.Test;

import io.mangoo.enums.Default;
import io.mangoo.test.http.WebBrowser;
import io.mangoo.test.http.WebRequest;
import io.mangoo.test.http.WebResponse;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;

/**
 * 
 * @author svenkubiak
 *
 */
public class I18nControllerTest {
    
    @Test
    public void testWithOutAdditionalHeader() {
        //given
        WebResponse response = WebRequest.get("/translation").execute();
        
        //then
        assertThat(response, not(nullValue()));
        assertThat(response.getStatusCode(), equalTo(StatusCodes.OK));
        assertThat(response.getContent(), equalTo("welcome"));
    }
    
    @Test
    public void testSpecialCharacters() {
        //given
        WebResponse response = WebRequest.get("/special")
                .withHeader("Accept-Language", "fr-FR")
                .execute();
        
        //then
        assertThat(response, not(nullValue()));
        assertThat(response.getStatusCode(), equalTo(StatusCodes.OK));
        assertThat(response.getContent(), equalTo("Côte d&#39;Ivoire"));
    }
    
    @Test
    public void testUmlaute() {
        //given
        WebResponse response = WebRequest.get("/umlaute")
                .withHeader("Accept-Language", "de-DE")
                .execute();
        
        //then
        assertThat(response, not(nullValue()));
        assertThat(response.getStatusCode(), equalTo(StatusCodes.OK));
        assertThat(response.getContent(), equalTo("Umlaute test : äöü und special (§&amp;! charcter- . te;;st"));
    }
    
    @Test
    public void testWithAdditionalHeaderDe() {
        //given
        WebResponse response = WebRequest.get("/translation")
                .withHeader("Accept-Language", "de-DE")
                .execute();
        
        //then
        assertThat(response, not(nullValue()));
        assertThat(response.getStatusCode(), equalTo(StatusCodes.OK));
        assertThat(response.getContent(), equalTo("willkommen"));
    }
    
    @Test
    public void testWithAdditionalHeaderEn() {
        //given
        WebResponse response = WebRequest.get("/translation")
                .withHeader("Accept-Language", "en-US")
                .execute();
        
        //then
        assertThat(response, not(nullValue()));
        assertThat(response.getStatusCode(), equalTo(StatusCodes.OK));
        assertThat(response.getContent(), equalTo("welcome"));
    }
    
    @Test
    public void testWithI18nCookie() {
        //given
        WebBrowser browser = WebBrowser.open();
        WebResponse response = browser.withMethod(Methods.GET).withUri("/localize").execute();
        
        //then
        assertThat(response, not(nullValue()));
        assertThat(response.getStatusCode(), equalTo(StatusCodes.OK));
        assertThat(response.getCookie(Default.I18N_COOKIE_NAME.toString()), not(nullValue()));
        
        //given
        response = browser.withUri("/translation").execute();
        
        //then
        assertThat(response, not(nullValue()));
        assertThat(response.getStatusCode(), equalTo(StatusCodes.OK));
        assertThat(response.getContent(), equalTo("welcome"));
    }
}