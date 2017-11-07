package io.mangoo.utils.cookie;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

import org.junit.Test;

import io.mangoo.helpers.cookie.CookieBuilder;
import io.mangoo.test.ConcurrentTestRunner;
import io.undertow.server.handlers.Cookie;

/**
 * 
 * @author svenkubiak
 *
 */
public class CookieBuilderTest {

    @Test
    public void testCookieBuilder() {
        //given
        LocalDateTime now = LocalDateTime.now();
        
        //when
        Cookie cookie = CookieBuilder.create()
                .name("foo")
                .value("bar")
                .path("/foobar")
                .domain("www.foo.com")
                .maxAge(1223)
                .expires(now)
                .discard(true)
                .secure(true)
                .httpOnly(true)
                .build();
        
        //then
        assertThat(cookie, not(nullValue()));
        assertThat("foo", equalTo(cookie.getName()));
        assertThat("bar", equalTo(cookie.getValue()));
        assertThat("/foobar", equalTo(cookie.getPath()));
        assertThat("www.foo.com", equalTo(cookie.getDomain()));
        assertThat(Date.from(now.atZone(ZoneId.systemDefault()).toInstant()), equalTo(cookie.getExpires()));
        assertThat(cookie.getMaxAge(), equalTo(1223));
        assertThat(cookie.isDiscard(), equalTo(true));
        assertThat(cookie.isSecure(), equalTo(true));
        assertThat(cookie.isHttpOnly(), equalTo(true));
    }
    
    @Test
    public void testCookieBuilderConcurrent() throws InterruptedException {
        Runnable runnable = () -> {
            //given
            LocalDateTime now = LocalDateTime.now();
            String uuid = UUID.randomUUID().toString();
            
            //when
            Cookie cookie = CookieBuilder.create()
                    .name(uuid)
                    .value("bar")
                    .path("/foobar")
                    .domain("www.foo.com")
                    .maxAge(1223)
                    .expires(now)
                    .discard(true)
                    .secure(true)
                    .httpOnly(true)
                    .build();
            
            //then
            assertThat(cookie, not(nullValue()));
            assertThat(uuid, equalTo(cookie.getName()));
            assertThat("bar", equalTo(cookie.getValue()));
            assertThat("/foobar", equalTo(cookie.getPath()));
            assertThat("www.foo.com", equalTo(cookie.getDomain()));
            assertThat(Date.from(now.atZone(ZoneId.systemDefault()).toInstant()), equalTo(cookie.getExpires()));
            assertThat(cookie.getMaxAge(), equalTo(1223));
            assertThat(cookie.isDiscard(), equalTo(true));
            assertThat(cookie.isSecure(), equalTo(true));
            assertThat(cookie.isHttpOnly(), equalTo(true));
        };
        
        ConcurrentTestRunner.create()
        .withRunnable(runnable)
        .withThreads(50)
        .run();
    }
}