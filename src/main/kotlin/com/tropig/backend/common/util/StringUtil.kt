package com.tropig.backend.common.util

import org.springframework.cache.annotation.Cacheable
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.InputStreamReader

@Component
class StringUtil {

    @Cacheable(cacheNames = ["nicknameComponent"], key = "#key")
    fun getWordLists(): Pair<List<String>, List<String>> {
        val adjust = ClassPathResource("nickname/adjustList.txt")
        val noun = ClassPathResource("nickname/nounList.txt")

        // 2️⃣ InputStream으로 읽기
        val adjustList = adjust.inputStream.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                // 3️⃣ 파일 전체 내용을 읽고 ',' 로 split
                reader.readText()
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            }
        }

        val nounList = noun.inputStream.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                // 3️⃣ 파일 전체 내용을 읽고 ',' 로 split
                reader.readText()
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            }
        }

        return Pair(adjustList, nounList)
    }
}