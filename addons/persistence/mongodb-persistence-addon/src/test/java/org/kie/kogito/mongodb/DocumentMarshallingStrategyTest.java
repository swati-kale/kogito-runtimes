/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.kogito.mongodb;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.kie.kogito.mongodb.marshalling.DocumentMarshallingStrategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DocumentMarshallingStrategyTest {

    private DocumentMarshallingStrategy documentMarshallingStrategy = new DocumentMarshallingStrategy();

    @Test
    public void testStringMarshalling() throws Exception {

        String value = "here is simple string value";

        boolean accepted = documentMarshallingStrategy.accept(value);
        assertTrue(accepted, "String type should be accepted");

        byte[] data = documentMarshallingStrategy.marshal(null, null, value);
        assertNotNull(data, "Marshalled content should not be null");

        Object returned = documentMarshallingStrategy.unmarshal("java.lang.String", null, null, data, null);
        assertNotNull(returned, "Unmarshalled value should not be null");
        assertEquals(value, returned, "Values should be the same");

    }

    @Test
    public void testIntegerMarshalling() throws Exception {

        Integer value = 25;

        boolean accepted = documentMarshallingStrategy.accept(value);
        assertTrue(accepted, "Integer type should be accepted");

        byte[] data = documentMarshallingStrategy.marshal(null, null, value);
        assertNotNull(data, "Marshalled content should not be null");

        Object returned = documentMarshallingStrategy.unmarshal("java.lang.Integer", null, null, data, null);
        assertNotNull(returned, "Unmarshalled value should not be null");
        assertEquals(value, returned, "Values should be the same");

    }

    @Test
    public void testLongMarshalling() throws Exception {

        Long value = 555L;

        boolean accepted = documentMarshallingStrategy.accept(value);
        assertTrue(accepted, "Long type should be accepted");

        byte[] data = documentMarshallingStrategy.marshal(null, null, value);
        assertNotNull(data, "Marshalled content should not be null");

        Object returned = documentMarshallingStrategy.unmarshal("java.lang.Long", null, null, data, null);
        assertNotNull(returned, "Unmarshalled value should not be null");
        assertEquals(value, returned, "Values should be the same");

    }

    @Test
    public void testDoubleMarshalling() throws Exception {

        Double value = 55.5;

        boolean accepted = documentMarshallingStrategy.accept(value);
        assertTrue(accepted, "Double type should be accepted");

        byte[] data = documentMarshallingStrategy.marshal(null, null, value);
        assertNotNull(data, "Marshalled content should not be null");

        Object returned = documentMarshallingStrategy.unmarshal("java.lang.Double", null, null, data, null);
        assertNotNull(returned, "Unmarshalled value should not be null");
        assertEquals(value, returned, "Values should be the same");

    }

    @Test
    public void testFloatMarshalling() throws Exception {

        Float value = 55.5f;

        boolean accepted = documentMarshallingStrategy.accept(value);
        assertTrue(accepted, "Float type should be accepted");

        byte[] data = documentMarshallingStrategy.marshal(null, null, value);
        assertNotNull(data, "Marshalled content should not be null");

        Object returned = documentMarshallingStrategy.unmarshal("java.lang.Float", null, null, data, null);
        assertNotNull(returned, "Unmarshalled value should not be null");
        assertEquals(value, returned, "Values should be the same");

    }

    @Test
    public void testBooleanMarshalling() throws Exception {

        Boolean value = true;

        boolean accepted = documentMarshallingStrategy.accept(value);
        assertTrue(accepted, "Boolean type should be accepted");

        byte[] data = documentMarshallingStrategy.marshal(null, null, value);
        assertNotNull(data, "Marshalled content should not be null");

        Object returned = documentMarshallingStrategy.unmarshal("java.lang.Boolean", null, null, data, null);
        assertNotNull(returned, "Unmarshalled value should not be null");
        assertEquals(value, returned, "Values should be the same");

    }

    @Test
    public void testDateMarshalling() throws Exception {

        Date value = new Date();

        boolean accepted = documentMarshallingStrategy.accept(value);
        assertTrue(accepted, "Date type should be accepted");

        byte[] data = documentMarshallingStrategy.marshal(null, null, value);
        assertNotNull(data, "Marshalled content should not be null");
        Object returned = documentMarshallingStrategy.unmarshal("java.util.Date", null, null, data, null);
        assertNotNull(returned, "Unmarshalled value should not be null");
        assertEquals(value, returned, "Values should be the same");

    }

    @Test
    public void testObjectMarshalling() throws Exception {

        Address value = new Address("main street", "Boston", "10005", "US");

        boolean accepted = documentMarshallingStrategy.accept(value);
        assertTrue(accepted, "String type should be accepted");

        byte[] data = documentMarshallingStrategy.marshal(null, null, value);
        assertNotNull(data, "Marshalled content should not be null");
        Object returned = documentMarshallingStrategy.unmarshal("org.kie.kogito.mongodb.Address", null, null, data, null);
        assertNotNull(returned, "Unmarshalled value should not be null");
        assertEquals(value.getClass().getCanonicalName(), returned.getClass().getCanonicalName(), "Values should be the same");

        Address a = (Address) returned;
        assertEquals(value.getCity(), a.getCity(), "City should be the same");
        assertEquals(value.getCountry(), a.getCountry(), "Country should be the same");
        assertEquals(value.getZipCode(), a.getZipCode(), "ZipCode should be the same");
    }
}
