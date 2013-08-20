package com.google.gson.stream

/**
 * Created with IntelliJ IDEA.
 * User: leiko
 * Date: 5/23/13
 * Time: 10:43 AM
 * To change this template use File | Settings | File Templates.
 */
public object JsonToken {
    /**
     * The opening of a JSON array. Written using {@link JsonWriter#beginArray}
     * and read using {@link JsonReader#beginArray}.
     */
    public val BEGIN_ARRAY : Int = 0

    /**
     * The closing of a JSON array. Written using {@link JsonWriter#endArray}
     * and read using {@link JsonReader#endArray}.
     */
    public val END_ARRAY : Int = 1

    /**
     * The opening of a JSON object. Written using {@link JsonWriter#beginObject}
     * and read using {@link JsonReader#beginObject}.
     */
    public val BEGIN_OBJECT : Int = 2

    /**
     * The closing of a JSON object. Written using {@link JsonWriter#endObject}
     * and read using {@link JsonReader#endObject}.
     */
    public val END_OBJECT : Int = 3

    /**
     * A JSON property name. Within objects, tokens alternate between names and
     * their values. Written using {@link JsonWriter#name} and read using {@link
     * JsonReader#nextName}
     */
    public val NAME : Int = 4

    /**
     * A JSON string.
     */
    public val STRING : Int = 5

    /**
     * A JSON number represented in this API by a Java {@code double}, {@code
     * long}, or {@code int}.
     */
    public val NUMBER : Int = 6

    /**
     * A JSON {@code true} or {@code false}.
     */
    public val BOOLEAN : Int = 7

    /**
     * A JSON {@code null}.
     */
    public val NULL : Int = 8

    /**
     * The end of the JSON stream. This sentinel value is returned by {@link
     * JsonReader#peek()} to signal that the JSON-encoded value has no more
     * tokens.
     */
    public val END_DOCUMENT : Int = 9
}