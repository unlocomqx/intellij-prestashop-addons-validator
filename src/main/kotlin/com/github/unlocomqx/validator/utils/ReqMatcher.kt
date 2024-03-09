package com.github.unlocomqx.validator.utils

import com.github.unlocomqx.validator.LocaleBundle

class ReqMatcher {
    companion object {
        fun matchValidateReq(url: String?): Boolean {
            if (url.isNullOrBlank()) {
                return false
            }
            if (!url.contains(LocaleBundle.message("validator_url"))) {
                return false
            }
            // remove LocaleBundle.message("validator_url")
            val reqUrl = url.replace(LocaleBundle.message("validator_url"), "")
            val regex = """module/\d+/validate$""".toRegex()
            return regex.matches(reqUrl)
        }
    }

}
