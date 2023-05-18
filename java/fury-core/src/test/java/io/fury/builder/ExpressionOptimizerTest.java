/*
 * Copyright 2023 The Fury authors
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fury.builder;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import io.fury.codegen.Code;
import io.fury.codegen.CodeGenerator;
import io.fury.codegen.CodegenContext;
import io.fury.codegen.CompileUnit;
import io.fury.codegen.Expression;
import io.fury.codegen.Expression.Add;
import io.fury.codegen.Expression.Literal;
import io.fury.codegen.Expression.Return;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.testng.annotations.Test;

public class ExpressionOptimizerTest {

  @Test
  public void testInvokeGenerated() throws Exception {
    CodegenContext ctx = new CodegenContext();
    String clsName = "TestInvokeGenerated";
    ctx.setClassName(clsName);
    ctx.setPackage("test");
    Expression expression =
        ExpressionOptimizer.invokeGenerated(
            ctx, () -> new Return(new Add(Literal.ofInt(1), Literal.ofInt(2))), "test");
    Code.ExprCode methodCode = new Return(expression).genCode(ctx);
    ctx.addMethod("add", methodCode.code(), int.class);
    String classCode = ctx.genCode();
    ClassLoader classLoader =
        new CodeGenerator(getClass().getClassLoader())
            .compile(new CompileUnit("test", clsName, classCode));
    Class<?> generatedClass = classLoader.loadClass("test." + clsName);
    {
      Method test = generatedClass.getMethod("add");
      test.setAccessible(true);
      assertEquals(test.invoke(generatedClass.newInstance()), 3);
    }
    {
      // janino generated an static method which take instance of class instead.
      assertTrue(
          Arrays.stream(generatedClass.getDeclaredMethods())
              .anyMatch(m -> m.getName().startsWith("test")));
    }
  }
}