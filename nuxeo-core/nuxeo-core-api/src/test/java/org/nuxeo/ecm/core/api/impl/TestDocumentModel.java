/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *  
 * Contributors:
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.api.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.log4j.spi.LoggingEvent;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;

/**
 * @since 9.1
 */
@Features({ LogCaptureFeature.class })
public class TestDocumentModel extends NXRuntimeTestCase {

    private static final String DEPRECATED_PROPERTY_LOG_NAME = "org.nuxeo.ecm.core.api.model.impl.DeprecatedProperty";

    private static final String GET_VALUE_DEPRECATED_LOG = "Field '%s' is marked as deprecated from '%s' schema, don't use it anymore. Return value from deprecated property";

    private static final String SET_VALUE_DEPRECATED_LOG = "Field '%s' is marked as deprecated from '%s' schema, don't use it anymore. Set value to deprecated property";

    private static final String GET_VALUE_FALLBACK_DEPRECATED_LOG = "Field '%s' is marked as deprecated from '%s' schema, don't use it anymore. Return value from %s if not null, from deprecated property otherwise";

    private static final String SET_VALUE_FALLBACK_DEPRECATED_LOG = "Field '%s' is marked as deprecated from '%s' schema, don't use it anymore. Set value to deprecated property";

    @Inject
    private LogCaptureFeature.Result logCaptureResult;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        deployContrib("org.nuxeo.ecm.core.api.tests", "OSGI-INF/test-documentmodel-types-contrib.xml");
    }

    // --------------------------------
    // Tests with deprecated properties
    // --------------------------------

    @Test
    @LogCaptureFeature.FilterOn(logLevel = "WARN", loggerName = DEPRECATED_PROPERTY_LOG_NAME)
    public void testSetDeprecatedScalarProperty() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty("deprecated", "scalar", "test scalar");
        // As property is just deprecated we still store the value
        assertEquals("test scalar", doc.getProperty("deprecated", "scalar"));

        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        // 3 logs:
        // - a set is basically one get then on set if value are different
        // - a get to assert property
        assertEquals(3, events.size());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(2).getRenderedMessage());
    }

    @Test
    @LogCaptureFeature.FilterOn(logLevel = "WARN", loggerName = DEPRECATED_PROPERTY_LOG_NAME)
    public void testSetDeprecatedScalarPropertyValue() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue("deprecated:scalar", "test scalar");
        assertEquals("test scalar", doc.getPropertyValue("deprecated:scalar"));

        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        // 3 logs:
        // - a set is basically one get then on set if value are different
        // - a get to assert property
        assertEquals(3, events.size());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(2).getRenderedMessage());
    }

    @Test
    @LogCaptureFeature.FilterOn(logLevel = "WARN", loggerName = DEPRECATED_PROPERTY_LOG_NAME)
    public void testSetDeprecatedScalarProperties() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties("deprecated", Collections.singletonMap("scalar", "test scalar"));
        assertEquals("test scalar", doc.getProperties("deprecated").get("scalar"));

        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        // 8 logs:
        // - a set is basically one get then on set if value are different
        // - a get to assert property
        // - one log for each of deprecated properties as we get a map of whole properties: here 5 deprecated properties
        assertEquals(8, events.size());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(2).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "complex", "deprecated"),
                events.get(3).getRenderedMessage());
        assertEquals(
                String.format(GET_VALUE_FALLBACK_DEPRECATED_LOG, "scalar2complex", "deprecated",
                        "DeprecatedProperty(complexfallback/scalar=StringProperty(complexfallback/scalar=null))"),
                events.get(4).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(5).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_FALLBACK_DEPRECATED_LOG, "scalar2scalar", "deprecated",
                "StringProperty(scalarfallback=null)"), events.get(6).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_FALLBACK_DEPRECATED_LOG, "complex2complex", "deprecated",
                "MapProperty(/complexfallback)"), events.get(7).getRenderedMessage());
    }

    @Test
    @LogCaptureFeature.FilterOn(logLevel = "WARN", loggerName = DEPRECATED_PROPERTY_LOG_NAME)
    public void testSetDeprecatedComplexProperty() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty("deprecated", "complex", Collections.singletonMap("scalar", "test scalar"));
        assertEquals("test scalar", doc.getProperty("deprecated", "complex/scalar"));

        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        // 3 logs:
        // - a set is basically one get then on set if value are different
        // - a get to assert property
        assertEquals(3, events.size());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "complex", "deprecated"),
                events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_DEPRECATED_LOG, "complex", "deprecated"),
                events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "complex", "deprecated"),
                events.get(2).getRenderedMessage());
    }

    @Test
    @LogCaptureFeature.FilterOn(logLevel = "WARN", loggerName = DEPRECATED_PROPERTY_LOG_NAME)
    public void testSetDeprecatedComplexPropertyValue() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue("deprecated:complex", (Serializable) Collections.singletonMap("scalar", "test scalar"));
        assertEquals("test scalar", doc.getPropertyValue("deprecated:complex/scalar"));

        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        assertEquals(3, events.size());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(2).getRenderedMessage());
    }

    @Test
    @LogCaptureFeature.FilterOn(logLevel = "WARN", loggerName = DEPRECATED_PROPERTY_LOG_NAME)
    public void testSetDeprecatedComplexProperties() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties("deprecated",
                Collections.singletonMap("complex", Collections.singletonMap("scalar", "test scalar")));
        assertNull(doc.getProperties("deprecated").get("complex"));

        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        assertEquals(3, events.size());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(2).getRenderedMessage());
    }

    @Test
    @LogCaptureFeature.FilterOn(logLevel = "WARN", loggerName = DEPRECATED_PROPERTY_LOG_NAME)
    public void testSetScalarOnDeprecatedComplexProperty() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty("deprecated", "complex/scalar", "test scalar");
        assertNull(doc.getProperty("deprecated", "complex/scalar"));

        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        assertEquals(3, events.size());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(2).getRenderedMessage());
    }

    @Test
    @LogCaptureFeature.FilterOn(logLevel = "WARN", loggerName = DEPRECATED_PROPERTY_LOG_NAME)
    public void testSetScalarOnDeprecatedComplexPropertyValue() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue("deprecated:complex/scalar", "test scalar");
        assertNull(doc.getPropertyValue("deprecated:complex/scalar"));

        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        assertEquals(3, events.size());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(2).getRenderedMessage());
    }

    @Test
    @LogCaptureFeature.FilterOn(logLevel = "WARN", loggerName = DEPRECATED_PROPERTY_LOG_NAME)
    public void testSetDeprecatedScalarPropertyWithFallbackOnScalar() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty("deprecated", "scalar2scalar", "test scalar");
        assertEquals("test scalar", doc.getProperty("deprecated", "scalarfallback"));

        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        assertEquals(3, events.size());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(2).getRenderedMessage());
    }

    @Test
    @LogCaptureFeature.FilterOn(logLevel = "WARN", loggerName = DEPRECATED_PROPERTY_LOG_NAME)
    public void testSetDeprecatedScalarPropertyValueWithFallbackOnScalar() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue("deprecated:scalar2scalar", "test scalar");
        assertEquals("test scalar", doc.getPropertyValue("deprecated:scalarfallback"));

        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        assertEquals(3, events.size());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(2).getRenderedMessage());
    }

    @Test
    @LogCaptureFeature.FilterOn(logLevel = "WARN", loggerName = DEPRECATED_PROPERTY_LOG_NAME)
    public void testSetDeprecatedScalarPropertiesWithFallbackOnScalar() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties("deprecated", Collections.singletonMap("scalar2scalar", "test scalar"));
        assertEquals("test scalar", doc.getProperties("deprecated").get("scalarfallback").toString());

        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        assertEquals(3, events.size());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(2).getRenderedMessage());
    }

    @Test
    @LogCaptureFeature.FilterOn(logLevel = "WARN", loggerName = DEPRECATED_PROPERTY_LOG_NAME)
    public void testSetDeprecatedScalarPropertyWithFallbackOnComplex() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty("deprecated", "scalar2complex", "test scalar");
        assertEquals("test scalar", doc.getProperty("deprecated", "complexfallback/scalar"));

        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        assertEquals(3, events.size());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(2).getRenderedMessage());
    }

    @Test
    @LogCaptureFeature.FilterOn(logLevel = "WARN", loggerName = DEPRECATED_PROPERTY_LOG_NAME)
    public void testSetDeprecatedScalarPropertyValueWithFallbackOnComplex() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue("deprecated:scalar2complex", "test scalar");
        assertEquals("test scalar", doc.getPropertyValue("deprecated:complexfallback/scalar"));

        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        assertEquals(3, events.size());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(2).getRenderedMessage());
    }

    @Test
    @SuppressWarnings("unchecked")
    @LogCaptureFeature.FilterOn(logLevel = "WARN", loggerName = DEPRECATED_PROPERTY_LOG_NAME)
    public void testSetDeprecatedScalarPropertiesWithFallbackOnComplex() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties("deprecated", Collections.singletonMap("scalar2complex", "test scalar"));
        assertEquals("test scalar",
                ((Map<String, Serializable>) doc.getProperties("deprecated").get("complexfallback")).get("scalar")
                                                                                                    .toString());

        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        assertEquals(3, events.size());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(2).getRenderedMessage());
    }

    @Test
    @LogCaptureFeature.FilterOn(logLevel = "WARN", loggerName = DEPRECATED_PROPERTY_LOG_NAME)
    public void testSetDeprecatedComplexPropertyWithFallbackOnComplex() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty("deprecated", "complex2complex", Collections.singletonMap("scalar", "test scalar"));
        assertEquals("test scalar", doc.getProperty("deprecated", "complexfallback/scalar"));

        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        assertEquals(3, events.size());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(2).getRenderedMessage());
    }

    @Test
    @LogCaptureFeature.FilterOn(logLevel = "WARN", loggerName = DEPRECATED_PROPERTY_LOG_NAME)
    public void testSetDeprecatedComplexPropertyValueWithFallbackOnComplex() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue("deprecated:complex2complex",
                (Serializable) Collections.singletonMap("scalar", "test scalar"));
        assertEquals("test scalar", doc.getPropertyValue("deprecated:complexfallback/scalar"));

        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        assertEquals(3, events.size());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(2).getRenderedMessage());
    }

    @Test
    @SuppressWarnings("unchecked")
    @LogCaptureFeature.FilterOn(logLevel = "WARN", loggerName = DEPRECATED_PROPERTY_LOG_NAME)
    public void testSetDeprecatedComplexPropertiesWithFallbackOnComplex() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties("deprecated",
                Collections.singletonMap("complex2complex", Collections.singletonMap("scalar", "test scalar")));
        assertEquals("test scalar",
                ((Map<String, Serializable>) doc.getProperties("deprecated").get("complexfallback")).get("scalar")
                                                                                                    .toString());

        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        assertEquals(3, events.size());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(2).getRenderedMessage());
    }

    @Test
    @LogCaptureFeature.FilterOn(logLevel = "WARN", loggerName = DEPRECATED_PROPERTY_LOG_NAME)
    public void testSetScalarOnDeprecatedComplexPropertyWithFallbackOnComplex() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty("deprecated", "complex2complex/scalar", "test scalar");
        assertEquals("test scalar", doc.getProperty("deprecated", "complexfallback/scalar"));

        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        assertEquals(3, events.size());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(2).getRenderedMessage());
    }

    @Test
    @LogCaptureFeature.FilterOn(logLevel = "WARN", loggerName = DEPRECATED_PROPERTY_LOG_NAME)
    public void testSetScalarOnDeprecatedComplexPropertyValueWithFallbackOnComplex() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue("deprecated:complex2complex/scalar", "test scalar");
        assertEquals("test scalar", doc.getPropertyValue("deprecated:complexfallback/scalar"));

        logCaptureResult.assertHasEvent();
        List<LoggingEvent> events = logCaptureResult.getCaughtEvents();
        assertEquals(3, events.size());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(0).getRenderedMessage());
        assertEquals(String.format(SET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(1).getRenderedMessage());
        assertEquals(String.format(GET_VALUE_DEPRECATED_LOG, "scalar", "deprecated"),
                events.get(2).getRenderedMessage());
    }

    // -----------------------------
    // Tests with removed properties
    // -----------------------------

    @Test
    public void testSetRemovedScalarProperty() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty("removed", "scalar", "test scalar");
        assertNull(doc.getProperty("removed", "scalar"));
    }

    @Test
    public void testSetRemovedScalarPropertyValue() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue("removed:scalar", "test scalar");
        assertNull(doc.getPropertyValue("removed:scalar"));
    }

    @Test
    public void testSetRemovedScalarProperties() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties("removed", Collections.singletonMap("scalar", "test scalar"));
        assertNull(doc.getProperties("removed").get("scalar"));
    }

    @Test
    public void testSetRemovedComplexProperty() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty("removed", "complex", Collections.singletonMap("scalar", "test scalar"));
        assertNull(doc.getProperty("removed", "complex/scalar"));
    }

    @Test
    public void testSetRemovedComplexPropertyValue() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue("removed:complex", (Serializable) Collections.singletonMap("scalar", "test scalar"));
        assertNull(doc.getPropertyValue("removed:complex/scalar"));
    }

    @Test
    public void testSetRemovedComplexProperties() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties("removed",
                Collections.singletonMap("complex", Collections.singletonMap("scalar", "test scalar")));
        assertNull(doc.getProperties("removed").get("complex"));
    }

    @Test
    public void testSetScalarOnRemovedComplexProperty() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty("removed", "complex/scalar", "test scalar");
        assertNull(doc.getProperty("removed", "complex/scalar"));
    }

    @Test
    public void testSetScalarOnRemovedComplexPropertyValue() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue("removed:complex/scalar", "test scalar");
        assertNull(doc.getPropertyValue("removed:complex/scalar"));
    }

    @Test
    public void testSetRemovedScalarPropertyWithFallbackOnScalar() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty("removed", "scalar2scalar", "test scalar");
        assertEquals("test scalar", doc.getProperty("removed", "scalarfallback"));
    }

    @Test
    public void testSetRemovedScalarPropertyValueWithFallbackOnScalar() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue("removed:scalar2scalar", "test scalar");
        assertEquals("test scalar", doc.getPropertyValue("removed:scalarfallback"));
    }

    @Test
    public void testSetRemovedScalarPropertiesWithFallbackOnScalar() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties("removed", Collections.singletonMap("scalar2scalar", "test scalar"));
        assertEquals("test scalar", doc.getProperties("removed").get("scalarfallback").toString());
    }

    @Test
    public void testSetRemovedScalarPropertyWithFallbackOnComplex() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty("removed", "scalar2complex", "test scalar");
        assertEquals("test scalar", doc.getProperty("removed", "complexfallback/scalar"));
    }

    @Test
    public void testSetRemovedScalarPropertyValueWithFallbackOnComplex() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue("removed:scalar2complex", "test scalar");
        assertEquals("test scalar", doc.getPropertyValue("removed:complexfallback/scalar"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSetRemovedScalarPropertiesWithFallbackOnComplex() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties("removed", Collections.singletonMap("scalar2complex", "test scalar"));
        assertEquals("test scalar",
                ((Map<String, Serializable>) doc.getProperties("removed").get("complexfallback")).get("scalar")
                                                                                                 .toString());
    }

    @Test
    public void testSetRemovedComplexPropertyWithFallbackOnComplex() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty("removed", "complex2complex", Collections.singletonMap("scalar", "test scalar"));
        assertEquals("test scalar", doc.getProperty("removed", "complexfallback/scalar"));
    }

    @Test
    public void testSetRemovedComplexPropertyValueWithFallbackOnComplex() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue("removed:complex2complex",
                (Serializable) Collections.singletonMap("scalar", "test scalar"));
        assertEquals("test scalar", doc.getPropertyValue("removed:complexfallback/scalar"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSetRemovedComplexPropertiesWithFallbackOnComplex() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties("removed",
                Collections.singletonMap("complex2complex", Collections.singletonMap("scalar", "test scalar")));
        assertEquals("test scalar",
                ((Map<String, Serializable>) doc.getProperties("removed").get("complexfallback")).get("scalar")
                                                                                                 .toString());
    }

    @Test
    public void testSetScalarOnRemovedComplexPropertyWithFallbackOnComplex() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty("removed", "complex2complex/scalar", "test scalar");
        assertEquals("test scalar", doc.getProperty("removed", "complexfallback/scalar"));
    }

    @Test
    public void testSetScalarOnRemovedComplexPropertyValueWithFallbackOnComplex() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue("removed:complex2complex/scalar", "test scalar");
        assertEquals("test scalar", doc.getPropertyValue("removed:complexfallback/scalar"));
    }

    // -------------------------------------
    // Tests with removed properties on blob
    // -------------------------------------

    @Test
    public void testSetRemovedScalarPropertyWithFallbackOnBlob() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty("file", "filename", "test filename");
        assertEquals("test filename", doc.getProperty("file", "content/name"));
    }

    @Test
    public void testSetRemovedScalarPropertyValueWithFallbackOnBlob() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue("file:filename", "test filename");
        assertEquals("test filename", doc.getPropertyValue("file:content/name"));
    }

    @Test
    public void testSetRemovedScalarPropertiesWithFallbackOnBlob() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties("file", Collections.singletonMap("filename", "test filename"));
        assertEquals("test filename", ((Blob) doc.getProperties("file").get("content")).getFilename());
    }

}
