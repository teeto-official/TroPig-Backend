package com.tropig.backend.recruitment.repository

import com.tropig.backend.recruitment.entity.Recruitment
import com.tropig.backend.recruitment.model.dto.SearchRecruitmentDto
import org.springframework.data.domain.Page

interface RecruitmentCustomRepository {
    fun searchRecruitments(request: SearchRecruitmentDto): Page<Recruitment>
}
