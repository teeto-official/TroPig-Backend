package com.tropig.backend.common.handler

import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.NotFoundException
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.NoHandlerFoundException

class GlobalExceptionHandlerTest {

    private val mockMvc =
        MockMvcBuilders.standaloneSetup(TestController())
            .setControllerAdvice(GlobalExceptionHandler())
            .build()

    @Test
    fun `returns json for missing handler even when accept header excludes json`() {
        mockMvc.perform(get("/no-handler").accept(MediaType.TEXT_HTML))
            .andExpect(status().isNotFound)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("요청한 경로를 찾을 수 없습니다."))
    }

    @Test
    fun `returns json for domain exception even when accept header excludes json`() {
        mockMvc.perform(get("/domain-not-found").accept(MediaType.TEXT_HTML))
            .andExpect(status().isNotFound)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("content not found"))
            .andExpect(jsonPath("$.code").value(MessageCode.NOT_FOUND_CONTENT.name))
    }

    @RestController
    private class TestController {

        @GetMapping("/no-handler")
        fun noHandler(): String = throw NoHandlerFoundException("GET", "/unknown", HttpHeaders())

        @GetMapping("/domain-not-found")
        fun domainNotFound(): String = throw NotFoundException("content not found", MessageCode.NOT_FOUND_CONTENT)
    }
}
