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

            val reqUrl = url.replace(LocaleBundle.message("validator_url"), "")
            val regex = """module/\d+/validate$""".toRegex()
            return regex.matches(reqUrl)
        }

        fun matchResultReq(url: String?): String? {
            if (url.isNullOrBlank()) {
                return null
            }
            if (!url.contains(LocaleBundle.message("validator_url"))) {
                return null
            }

            val reqUrl = url.replace(LocaleBundle.message("validator_url"), "")
            val regex = """module/\d+/validate/(\w+)\?""".toRegex()
            val matches = regex.find(reqUrl)
            if (matches != null) {
                val (result) = matches.destructured
                return result
            }
            return null
        }
    }

}
