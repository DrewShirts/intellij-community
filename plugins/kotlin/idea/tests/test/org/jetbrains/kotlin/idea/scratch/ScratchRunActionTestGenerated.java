/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scratch;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TargetBackend;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.generators.tests.TestsPackage}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("idea/testData/scratch")
@TestDataPath("$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
public class ScratchRunActionTestGenerated extends AbstractScratchRunActionTest {
    public void testAllFilesPresentInScratch() throws Exception {
        KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("idea/testData/scratch"), Pattern.compile("^(.+)\\.kts$"), TargetBackend.ANY, true);
    }

    @TestMetadata("for.kts")
    public void testFor() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/scratch/for.kts");
        doTest(fileName);
    }

    @TestMetadata("simple.kts")
    public void testSimple() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/scratch/simple.kts");
        doTest(fileName);
    }

    @TestMetadata("simpleFun.kts")
    public void testSimpleFun() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/scratch/simpleFun.kts");
        doTest(fileName);
    }

    @TestMetadata("stdlibFun.kts")
    public void testStdlibFun() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/scratch/stdlibFun.kts");
        doTest(fileName);
    }

    @TestMetadata("unresolved.kts")
    public void testUnresolved() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/scratch/unresolved.kts");
        doTest(fileName);
    }

    @TestMetadata("userOutput.kts")
    public void testUserOutput() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/scratch/userOutput.kts");
        doTest(fileName);
    }
}
