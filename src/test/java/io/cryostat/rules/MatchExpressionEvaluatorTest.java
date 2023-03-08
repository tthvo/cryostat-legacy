/*
 * Copyright The Cryostat Authors
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or data
 * (collectively the "Software"), free of charge and under any and all copyright
 * rights in the Software, and any and all patent rights owned or freely
 * licensable by each licensor hereunder covering either (i) the unmodified
 * Software as contributed to or provided by such licensor, or (ii) the Larger
 * Works (as defined below), to deal in both
 *
 * (a) the Software, and
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software (each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 * The above copyright notice and either this complete permission notice or at
 * a minimum a reference to the UPL must be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.cryostat.rules;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptException;

import io.cryostat.MainModule;
import io.cryostat.configuration.CredentialsManager;
import io.cryostat.core.log.Logger;
import io.cryostat.platform.ServiceRef;
import io.cryostat.platform.ServiceRef.AnnotationKey;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchExpressionEvaluatorTest {

    MatchExpressionEvaluator ruleMatcher;
    @Mock ServiceRef serviceRef;
    @Mock Logger logger;
    @Mock CredentialsManager credentials;
    @Mock RuleRegistry rules;

    URI serviceUri;
    String jvmId;
    String alias;
    Map<String, String> labels;
    Map<String, String> platformAnnotations;
    Map<AnnotationKey, String> cryostatAnnotations;

    @BeforeEach
    void setup() throws Exception {
        this.ruleMatcher =
                new MatchExpressionEvaluator(
                        MainModule.provideScriptEngine(), credentials, rules, logger);

        this.serviceUri = new URI("service:jmx:rmi:///jndi/rmi://cryostat:9091/jmxrmi");
        this.jvmId = "-some1234HashId=";
        this.alias = "someAlias";
        this.labels = Map.of("label1", "someLabel");
        this.platformAnnotations = Map.of("annotation1", "someAnnotation");
        this.cryostatAnnotations = Map.of(AnnotationKey.JAVA_MAIN, "io.cryostat.Cryostat");

        Mockito.when(serviceRef.getServiceUri()).thenReturn(this.serviceUri);
        Mockito.when(serviceRef.getJvmId()).thenReturn(this.jvmId);
        Mockito.when(serviceRef.getAlias()).thenReturn(Optional.of(this.alias));
        Mockito.when(serviceRef.getLabels()).thenReturn(this.labels);
        Mockito.when(serviceRef.getPlatformAnnotations()).thenReturn(this.platformAnnotations);
        Mockito.when(serviceRef.getCryostatAnnotations()).thenReturn(this.cryostatAnnotations);
    }

    @Nested
    class BindingsContents {
        Bindings bindings;

        @BeforeEach
        void setup() {
            this.bindings = ruleMatcher.createBindings(serviceRef);
        }

        @Test
        void bindingsShouldContainTargetReference() {
            MatcherAssert.assertThat(bindings, Matchers.hasKey("target"));
            MatcherAssert.assertThat(bindings.size(), Matchers.equalTo(1));
        }

        @Test
        void targetShouldHaveExpectedKeys() {
            Set<String> keys = ((Map<String, Object>) bindings.get("target")).keySet();
            MatcherAssert.assertThat(
                    keys,
                    Matchers.equalTo(
                            Set.of("connectUrl", "jvmId", "alias", "labels", "annotations")));
        }

        @Test
        void targetShouldHaveServiceUriAsUri() {
            String uri = (String) ((Map<String, Object>) bindings.get("target")).get("connectUrl");
            MatcherAssert.assertThat(
                    uri, Matchers.equalTo(MatchExpressionEvaluatorTest.this.serviceUri.toString()));
        }

        @Test
        void targetShouldHaveAliasAsString() {
            String alias = (String) (((Map<String, Object>) bindings.get("target")).get("alias"));
            MatcherAssert.assertThat(
                    alias, Matchers.equalTo(MatchExpressionEvaluatorTest.this.alias));
        }

        @Test
        void targetShouldHaveJvmIdAsString() {
            String jvmId = (String) (((Map<String, Object>) bindings.get("target")).get("jvmId"));
            MatcherAssert.assertThat(
                    jvmId, Matchers.equalTo(MatchExpressionEvaluatorTest.this.jvmId));
        }

        @Test
        void targetShouldHaveLabels() {
            Map<String, String> labels =
                    (Map<String, String>)
                            (((Map<String, Object>) bindings.get("target")).get("labels"));
            MatcherAssert.assertThat(
                    labels, Matchers.equalTo(MatchExpressionEvaluatorTest.this.labels));
        }

        @Test
        void annotationsShouldHaveExpectedFields() {
            Set<String> keys =
                    ((Map<String, Object>)
                                    ((Map<String, Object>) bindings.get("target"))
                                            .get("annotations"))
                            .keySet();
            MatcherAssert.assertThat(keys, Matchers.equalTo(Set.of("platform", "cryostat")));
        }

        @Test
        void targetShouldHavePlatformAnnotations() {
            Map<String, String> annotations =
                    (Map<String, String>)
                            ((Map<String, Object>)
                                            ((Map<String, Object>) bindings.get("target"))
                                                    .get("annotations"))
                                    .get("platform");
            MatcherAssert.assertThat(
                    annotations,
                    Matchers.equalTo(MatchExpressionEvaluatorTest.this.platformAnnotations));
        }

        @Test
        void targetShouldHaveCryostatAnnotations() {
            Map<String, String> annotations =
                    (Map<String, String>)
                            ((Map<String, Object>)
                                            ((Map<String, Object>) bindings.get("target"))
                                                    .get("annotations"))
                                    .get("cryostat");
            Map<String, String> expected = new HashMap<>();
            for (Map.Entry<ServiceRef.AnnotationKey, String> entry :
                    MatchExpressionEvaluatorTest.this.cryostatAnnotations.entrySet()) {
                expected.put(entry.getKey().name(), entry.getValue());
            }
            MatcherAssert.assertThat(annotations, Matchers.equalTo(expected));
        }
    }

    @Nested
    class ExpressionEvaluation {

        @Test
        void shouldMatchOnTrue() throws Exception {
            Assertions.assertTrue(ruleMatcher.applies("true", serviceRef));
        }

        @Test
        void shouldNotMatchOnFalse() throws Exception {
            Assertions.assertFalse(ruleMatcher.applies("false", serviceRef));
        }

        @Test
        void shouldMatchOnAlias() throws Exception {
            String expr =
                    String.format("target.alias == '%s'", MatchExpressionEvaluatorTest.this.alias);
            Assertions.assertTrue(ruleMatcher.applies(expr, serviceRef));
        }

        @Test
        void shouldMatchOnJvmId() throws Exception {
            String expr =
                    String.format("target.jvmId == '%s'", MatchExpressionEvaluatorTest.this.jvmId);
            Assertions.assertTrue(ruleMatcher.applies(expr, serviceRef));
        }

        @Test
        void shouldNotMatchOnWrongJvmId() throws Exception {
            String expr = "target.jvmId == \"hello-world\"";
            Assertions.assertFalse(ruleMatcher.applies(expr, serviceRef));
        }

        @ParameterizedTest
        @ValueSource(strings = {"foo", "somethingelse", "true"})
        @NullAndEmptySource
        void shouldNotMatchOnWrongAlias(String s) throws Exception {
            String expr = String.format("target.alias == '%s'", s);
            Assertions.assertFalse(ruleMatcher.applies(expr, serviceRef));
        }

        @Test
        void shouldMatchOnConnectUrl() throws Exception {
            String expr =
                    String.format(
                            "target.connectUrl == '%s'",
                            MatchExpressionEvaluatorTest.this.serviceUri.toString());
            Assertions.assertTrue(ruleMatcher.applies(expr, serviceRef));
        }

        @Test
        void shouldMatchOnLabels() throws Exception {
            String expr = "target.labels.label1 == 'someLabel'";
            Assertions.assertTrue(ruleMatcher.applies(expr, serviceRef));
        }

        @Test
        void shouldNotMatchOnMissingLabels() throws Exception {
            String expr = "target.labels.label2 == 'someLabel'";
            Assertions.assertFalse(ruleMatcher.applies(expr, serviceRef));
        }

        @Test
        void shouldMatchOnPlatformAnnotations() throws Exception {
            String expr = "target.annotations.platform.annotation1 == 'someAnnotation'";
            Assertions.assertTrue(ruleMatcher.applies(expr, serviceRef));
        }

        @Test
        void shouldMatchOnCryostatAnnotations() throws Exception {
            String expr = "target.annotations.cryostat.JAVA_MAIN == 'io.cryostat.Cryostat'";
            Assertions.assertTrue(ruleMatcher.applies(expr, serviceRef));
        }

        @ParameterizedTest
        @ValueSource(strings = {"1", "null", "target.alias", "\"a string\""})
        void shouldThrowExceptionOnNonBooleanExpressionEval(String expr) throws Exception {
            Assertions.assertThrows(
                    ScriptException.class, () -> ruleMatcher.applies(expr, serviceRef));
        }
    }
}
