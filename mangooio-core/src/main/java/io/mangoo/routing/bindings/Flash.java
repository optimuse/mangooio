package io.mangoo.routing.bindings;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Sets;

/**
 *
 * @author svenkubiak
 *
 */
public class Flash {
    private static final Logger LOG = LogManager.getLogger(Flash.class);
    private static final Set<String> INVALID_CHARACTERS = Sets.newHashSet("|", ":", "&", " ");
    private static final String ERROR = "error";
    private static final String WARNING = "warning";
    private static final String SUCCESS = "success";
    private Map<String, String> values = new HashMap<>();
    private boolean discard;

    public Flash() {
      //Empty constructor required for Google Guice
    }

    public Flash(Map<String, String> values) {
        this.values = values;
    }

    /**
     * Sets a specific error message available with
     * the key 'error'
     *
     * @param value The message
     */
    public void setError(String value) {
        if (validCharacters(value)) {
            this.values.put(ERROR, value);
        }
    }

    /**
     * Sets a specific warning message available with
     * the key 'warning'
     *
     * @param value The message
     */
    public void setWarning(String value) {
        if (validCharacters(value)) {
            this.values.put(WARNING, value);
        }
    }

    /**
     * Sets a specific success message available with
     * the key 'success'
     *
     * @param value The message
     */
    public void setSuccess(String value) {
        if (validCharacters(value)) {
            this.values.put(SUCCESS, value);
        }
    }

    /**
     * Adds a value with a specific key to the flash overwriting an
     * existing value
     *
     * @param key The key
     * @param value The value
     */
    public void put(String key, String value) {
        if (validCharacters(key) && validCharacters(value)) {
            this.values.put(key, value);
        }
    }

    /**
     * Retrieves a specific value from the flash
     *
     * @param key The key
     * @return The value or null if not found
     */
    public String get(String key) {
        return this.values.get(key);
    }

    public Map<String, String> getValues() {
        return this.values;
    }

    public boolean isDiscard() {
        return discard;
    }

    public void setDiscard(boolean discard) {
        this.discard = discard;
    }

    public boolean hasContent() {
        return !this.values.isEmpty();
    }

    /**
     * Checks if the given value contains characters that are not allowed
     * in the key or value of a flash cookie
     *
     * @param value The value to check
     * @return True if the given string is valid, false otherwise
     */
    private boolean validCharacters(String value) {
        if (INVALID_CHARACTERS.contains(value)) {
            LOG.error("Flash key or value can not contain the following characters: spaces, |, & or :");
            return false;
        }

        return true;
    }
}