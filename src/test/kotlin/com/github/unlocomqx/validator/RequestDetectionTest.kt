package com.github.unlocomqx.validator

import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.github.unlocomqx.validator.utils.ReqMatcher
import junit.framework.TestCase

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class RequestDetectionTest : BasePlatformTestCase() {

    fun testMatchUploadRequest() {
        TestCase.assertTrue(ReqMatcher.matchValidateReq(LocaleBundle.message("validator_url") + "module/1/validate"))
    }

    override fun getTestDataPath() = "src/test/testData/rename"
}
