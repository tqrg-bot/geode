/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gemstone.gemfire.test.dunit.tests;

import static com.gemstone.gemfire.test.dunit.Assert.*;
import static com.gemstone.gemfire.test.dunit.Invoke.*;
import static com.googlecode.catchexception.CatchException.*;
import static com.googlecode.catchexception.throwable.CatchThrowable.*;
import static org.hamcrest.Matchers.*;

import java.util.Properties;

import com.gemstone.gemfire.test.dunit.AsyncInvocation;
import com.gemstone.gemfire.test.dunit.DUnitEnv;
import com.gemstone.gemfire.test.dunit.DistributedTestCase;
import com.gemstone.gemfire.test.dunit.Host;
import com.gemstone.gemfire.test.dunit.RMIException;
import com.gemstone.gemfire.test.dunit.VM;

/**
 * This class tests the basic functionality of the distributed unit
 * test framework.
 */
@SuppressWarnings("unused")
public class BasicDUnitTest extends DistributedTestCase {

  private static final String MESSAGE_FOR_remoteThrowException = "Test exception.  Please ignore.";

  private static Properties bindings;

  private VM vm0;
  private VM vm1;

  public BasicDUnitTest(String name) {
    super(name);
  }

  @Override
  public final void postSetUp() throws Exception {
    bindings = new Properties();
    invokeInEveryVM(() -> bindings = new Properties());
    this.vm0 = Host.getHost(0).getVM(0);
    this.vm1 = Host.getHost(0).getVM(1);
  }

  @Override
  public void postTearDown() throws Exception {
    bindings = null;
    invokeInEveryVM(() -> bindings = null);
  }

  public void testPreconditions() throws Exception {
    invokeInEveryVM(() -> assertThat("getUniqueName() must not return null", getUniqueName(), notNullValue()));
    invokeInEveryVM(() -> assertThat("bindings must not be null", bindings, notNullValue()));
  }

  public void testInvokeOnClassTargetWithEmptyArgs() throws Exception {
    assertThat(this.vm0.invoke(BasicDUnitTest.class, "booleanValue", new Object[] {}), is(true));
  }
  public void testInvokeOnObjectTargetWithEmptyArgs() throws Exception {
    assertThat(this.vm0.invoke(new BasicDUnitTest(""), "booleanValue", new Object[] {}), is(true));
  }
  public void testInvokeAsyncOnClassTargetWithEmptyArgs() throws Exception {
    AsyncInvocation<?> async = this.vm0.invokeAsync(BasicDUnitTest.class, "booleanValue", new Object[] {}).join();
    assertThat(async.getResult(), is(true));
  }
  public void testInvokeAsyncOnObjectTargetWithEmptyArgs() throws Exception {
    AsyncInvocation<?> async = this.vm0.invokeAsync(new BasicDUnitTest(""), "booleanValue", new Object[] {}).join();
    assertThat(async.getResult(), is(true));
  }

  public void testInvokeOnClassTargetWithNullArgs() throws Exception {
    assertThat(this.vm0.invoke(BasicDUnitTest.class, "booleanValue", null), is(true));
  }
  public void testInvokeOnObjectTargetWithNullArgs() throws Exception {
    assertThat(this.vm0.invoke(new BasicDUnitTest(""), "booleanValue", null), is(true));
  }
  public void testInvokeAsyncOnClassTargetWithNullArgs() throws Exception {
    AsyncInvocation<?> async = this.vm0.invokeAsync(BasicDUnitTest.class, "booleanValue", null).join();
    assertThat(async.getResult(), is(true));
  }
  public void testInvokeAsyncOnObjectTargetWithNullArgs() throws Exception {
    AsyncInvocation<?> async = this.vm0.invokeAsync(new BasicDUnitTest(""), "booleanValue", null).join();
    assertThat(async.getResult(), is(true));
  }

  public void testRemoteInvocationWithException() throws Exception {
    catchException(this.vm0).invoke(() -> remoteThrowException());

    assertThat(caughtException(), instanceOf(RMIException.class));
    assertThat(caughtException().getCause(), notNullValue());
    assertThat(caughtException().getCause(), instanceOf(BasicTestException.class));
    assertThat(caughtException().getCause().getMessage(), is(MESSAGE_FOR_remoteThrowException));
  }

  public void testInvokeWithLambda() throws Exception {
    assertThat(this.vm0.invoke(() -> DUnitEnv.get().getVMID()), is(0));
    assertThat(this.vm1.invoke(() -> DUnitEnv.get().getVMID()), is(1));
  }

  public void testInvokeLambdaAsync() throws Throwable {
    assertThat(this.vm0.invokeAsync(() -> DUnitEnv.get().getVMID()).getResult(), is(0));
  }

  public void testInvokeWithNamedLambda() {
    assertThat(this.vm0.invoke("getVMID", () -> DUnitEnv.get().getVMID()), is(0));
    assertThat(this.vm1.invoke("getVMID", () -> DUnitEnv.get().getVMID()), is(1));
  }

  public void testInvokeNamedLambdaAsync() throws Throwable {
    assertThat(this.vm0.invokeAsync("getVMID", () -> DUnitEnv.get().getVMID()).getResult(), is(0));
  }

  public void testRemoteInvokeAsync() throws Exception {
    String name = getUniqueName();
    String value = "Hello";

    this.vm0.invokeAsync(() -> remoteBind(name, value)).join().checkException();
    this.vm0.invokeAsync(() -> remoteValidateBind(name, value )).join().checkException();
  }

  public void testRemoteInvokeAsyncWithException() throws Exception {
    AsyncInvocation<?> async = this.vm0.invokeAsync(() -> remoteThrowException()).join();

    assertThat(async.exceptionOccurred(), is(true));
    assertThat(async.getException(), instanceOf(BasicTestException.class));

    catchThrowable(async).checkException();

    assertThat(caughtThrowable(), instanceOf(AssertionError.class));
    assertThat(caughtThrowable().getCause(), notNullValue());
    assertThat(caughtThrowable().getCause(), instanceOf(BasicTestException.class));
    assertThat(caughtThrowable().getCause().getMessage(), is(MESSAGE_FOR_remoteThrowException));
  }

  public void testInvokeNamedRunnableLambdaAsync() throws Exception {
    catchThrowable(this.vm0.invokeAsync("throwSomething", () -> throwException()).join()).checkException();

    assertThat(caughtThrowable(), notNullValue());
    assertThat(caughtThrowable().getCause(), notNullValue());
    assertThat(caughtThrowable().getCause(), instanceOf(BasicDUnitException.class));
  }

  public void testInvokeNamedRunnableLambda() throws Exception {
    catchException(this.vm0).invoke("throwSomething", () -> throwException());

    assertThat(caughtException(), notNullValue());
    assertThat(caughtException().getCause(), notNullValue());
    assertThat(caughtException().getCause(), instanceOf(BasicDUnitException.class));
    assertThat(caughtException().getCause().getMessage(), nullValue());
  }

  private static boolean booleanValue() { // invoked by reflection
    return true;
  }

  private static boolean booleanValue(final boolean value) { // invoked by reflection
    return value;
  }

  private static void remoteThrowException() {
    throw new BasicTestException(MESSAGE_FOR_remoteThrowException);
  }

  private static void throwException() throws BasicDUnitException {
    throw new BasicDUnitException();
  }

  private static void remoteBind(String name, String value) {
    assertNotNull("name must not be null", name);
    assertNotNull("value must not be null", value);
    assertNotNull("bindings must not be null", bindings);

    new BasicDUnitTest("").getSystem(); // forces connection
    bindings.setProperty(name, value);
  }

  private static void remoteValidateBind(String name, String expected) {
    assertEquals(expected, bindings.getProperty(name));
  }

  private static class BasicTestException extends RuntimeException {
    BasicTestException() {
      this("Test exception.  Please ignore.");
    }
    BasicTestException(String s) {
      super(s);
    }
  }

  private static class BasicDUnitException extends RuntimeException {
    public BasicDUnitException() {
    }
  }
}
