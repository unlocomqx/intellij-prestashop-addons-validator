package com.github.unlocomqx.validator

import com.github.unlocomqx.validator.toolWindow.NodesBuilders.getLabelsWithLinks
import com.github.unlocomqx.validator.utils.ReqMatcher
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class LinksReplacerTest : BasePlatformTestCase() {

    fun testGetLabelsWithLinks() {
        val message =
            "The following rule(s) need be applied to match the coding standard: align_multiline_comment, concat_space, no_blank_lines_after_phpdoc"
        val labels = getLabelsWithLinks(message)
        TestCase.assertTrue(ReqMatcher.matchValidateReq(LocaleBundle.message("validator_url") + "module/1/validate"))
    }
}
