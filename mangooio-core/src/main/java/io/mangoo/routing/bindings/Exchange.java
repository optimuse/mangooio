package io.mangoo.routing.bindings;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author svenkubiak
 *
 * @deprecated As of release 1.1.0, replaced by {@link #Payload}
 *
 */
@Deprecated
public class Exchange {
    private Map<String, Object> content = new HashMap<String, Object>();

    public void addContent(String key, Object value) {
        this.content.put(key, value);
    }

    public Map<String, Object> getContent() {
        return this.content;
    }
}