package com.tropig.backend.common.annotation

import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RestController

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@RestController
@CrossOrigin(origins = ["*"])
@SecurityRequirement(name = "bearerAuth")
annotation class ApiController()