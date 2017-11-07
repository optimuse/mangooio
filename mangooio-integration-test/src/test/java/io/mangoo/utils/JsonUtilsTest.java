package io.mangoo.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.UUID;

import org.junit.Test;

import com.jayway.jsonpath.ReadContext;

import io.mangoo.models.Car;
import io.mangoo.test.ConcurrentTestRunner;

/**
 * 
 * @author svenkubiak
 *
 */
public class JsonUtilsTest {
    private final String expectedJson = "{\"brand\":null,\"doors\":0,\"foo\":\"blablabla\"}";

    @Test
    public void testToJson() {
        //given
        Car car = new Car();
        
        //when
        String json = JsonUtils.toJson(car);
        
        //then
        assertThat(json, not(nullValue()));
        assertThat(json, equalTo(expectedJson));
    }
    
    @Test
    public void testConcurrentToJson() throws InterruptedException {
        Runnable runnable = () -> {
            //given
            String uuid = UUID.randomUUID().toString();
            Car car = new Car(uuid);
            
            //when
            String json = JsonUtils.toJson(car);
            
            //then
            assertThat(json, not(nullValue()));
            assertThat(json, equalTo("{\"brand\":null,\"doors\":0,\"foo\":\"blablabla\",\"id\":\"" + uuid + "\"}"));   
        };
        
        ConcurrentTestRunner.create()
            .withRunnable(runnable)
            .withThreads(50)
            .run();
    }
    
    @Test
    public void testFromJson() {
        //given
        String json = "{\"brand\":null,\"doors\":0,\"foo\":\"blablabla\"}";
        
        //when
        ReadContext readContext = JsonUtils.fromJson(json);
        
        //then
        assertThat(readContext, not(nullValue()));
        assertThat(readContext.read("$.foo"), equalTo("blablabla"));
    }
    
    @Test
    public void testConcurrentFromJson() throws InterruptedException {
        Runnable runnable = () -> {
            //given
            String uuid = UUID.randomUUID().toString(); 
            String json = "{\"brand\":null,\"doors\":0,\"foo\":\"" + uuid + "\"}";
            
            //when
            ReadContext readContext = JsonUtils.fromJson(json);
            
            //then
            assertThat(readContext, not(nullValue()));
            assertThat(readContext.read("$.foo"), equalTo(uuid));
        };
        
        ConcurrentTestRunner.create()
        .withRunnable(runnable)
        .withThreads(50)
        .run();
    }
    
    @Test
    public void testFromJsonToClass() {
        //given
        String json = "{\"brand\":null,\"doors\":0,\"foo\":\"blablabla\"}";
        
        //when
        Car car = JsonUtils.fromJson(json, Car.class);
        
        //then
        assertThat(car, not(nullValue()));
        assertThat(car.brand, equalTo(null));
        assertThat(car.doors, equalTo(0));
        assertThat(car.foo, equalTo("blablabla"));
    }
    
    @Test
    public void testConcurrentFromJsonToClass() throws InterruptedException {
        Runnable runnable = () -> {
            //given
            String uuid = UUID.randomUUID().toString(); 
            String json = "{\"brand\":null,\"doors\":0,\"foo\":\"" + uuid + "\"}";
            
            //when
            Car car = JsonUtils.fromJson(json, Car.class);
            
            //then
            assertThat(car, not(nullValue()));
            assertThat(car.brand, equalTo(null));
            assertThat(car.doors, equalTo(0));
            assertThat(car.foo, equalTo(uuid));
        };
        
        ConcurrentTestRunner.create()
        .withRunnable(runnable)
        .withThreads(50)
        .run();
    }
}