package com.tropig.backend.contents.service

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tropig.backend.common.enums.Genre
import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.enums.Rule
import com.tropig.backend.common.exception.ContentException
import com.tropig.backend.common.exception.NotFoundException
import com.tropig.backend.common.model.CursorSlice
import com.tropig.backend.contents.entity.Content
import com.tropig.backend.contents.entity.ContentTag
import com.tropig.backend.contents.entity.ContentThumbnail
import com.tropig.backend.contents.entity.RelatedContent
import com.tropig.backend.contents.enums.ContentType
import com.tropig.backend.contents.enums.ContentsStatus
import com.tropig.backend.contents.enums.PlayerCountType
import com.tropig.backend.contents.enums.TermType
import com.tropig.backend.contents.model.dto.SearchContentRequestDto
import com.tropig.backend.contents.model.request.CreateContentRequest
import com.tropig.backend.contents.model.request.UpdateContentRequest
import com.tropig.backend.contents.model.result.CountSearchContentsResult
import com.tropig.backend.contents.model.result.PickContentResult
import com.tropig.backend.contents.model.result.TagResult
import com.tropig.backend.contents.model.serialize.PublishingInfo
import com.tropig.backend.contents.model.serialize.toJson
import com.tropig.backend.contents.repository.ContentRepository
import com.tropig.backend.contents.repository.ContentTagRepository
import com.tropig.backend.contents.repository.ContentThumbnailRepository
import com.tropig.backend.contents.repository.RelatedContentRepository
import com.tropig.backend.contents.repository.TagRepository
import jakarta.transaction.Transactional
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrNull

@Service
class ContentService(
    private val contentRepository: ContentRepository,
    private val contentTagRepository: ContentTagRepository,
    private val contentThumbnailRepository: ContentThumbnailRepository,
    private val tagRepository: TagRepository,
    private val relatedContentRepository: RelatedContentRepository,
    private val s3Service: S3Service,
) {
    private val typeToken = object : TypeToken<List<String>>() {}.type

    fun findById(id: Long): Content? = contentRepository.findById(id).getOrNull()

    fun findByIdInAndType(ids: List<Long>, type: ContentType): List<Content> =
        contentRepository.findByIdInAndType(ids, type)

    @Cacheable(value = ["pickContentByType"], key = "#type.name() + '_' + #isAdult")
    fun getPickContentsByType(type: ContentType, isAdult: Boolean, contentIds: List<Long>): List<PickContentResult> {
        val contents = if (isAdult) {
            contentRepository.findByIdInAndType(contentIds, type)
        } else {
            contentRepository.findContentsByIdInAndTypeAndAdult(contentIds, type, false)
        }
        val thumbnails = getThumbnailPath(contentIds).associateBy { it.contentId }
        val tags = contentTagRepository.findByContentIdIn(contentIds)
            .map { TagResult(it.tagId, it.contentId, it.type) }
            .groupBy { it.contentId }

        return contents.map {
            PickContentResult(
                id = it.id,
                title = it.title,
                alias = it.alias,
                thumbnailPath = s3Service.toUrl(thumbnails[it.id]?.path),
                writerId = it.memberId,
                tags = tags[it.id] ?: emptyList(),
            )
        }
    }

    @Cacheable(value = ["getNewestContents"], key = "#type.name() + '_' + #isAdult")
    fun getNewestContents(type: ContentType, isAdult: Boolean): List<Content> = if (isAdult) {
        contentRepository.findTop20ByTypeAndStatusOrderByPublishedAtDesc(type, ContentsStatus.PUBLISHED)
    } else {
        contentRepository.findTop20ByTypeAndStatusAndAdultOrderByPublishedAtDesc(
            type,
            ContentsStatus.PUBLISHED,
            false,
        )
    }

    fun getThumbnailPath(contentIds: List<Long>): List<ContentThumbnail> =
        contentThumbnailRepository.findByContentIdIn(contentIds)

    @Cacheable(value = ["getDraftContent"], key = "#memberId")
    fun getDraftContent(memberId: Long): List<Content> =
        contentRepository.findByMemberIdAndStatus(memberId, ContentsStatus.DRAFT)

    fun searchContents(request: SearchContentRequestDto): CursorSlice<Content> =
        contentRepository.searchContents(request)

    fun countSearchContents(request: SearchContentRequestDto): CountSearchContentsResult =
        contentRepository.countSearchContents(request)

    fun saveAllContentThumbnail(contentThumbnails: List<ContentThumbnail>): List<ContentThumbnail> =
        contentThumbnailRepository.saveAll(contentThumbnails)

    fun findThumbnailByContentId(contentId: Long): List<ContentThumbnail> =
        contentThumbnailRepository.findByContentId(contentId)

    fun deleteThumbnails(ids: List<Long>) = contentThumbnailRepository.deleteByIdIn(ids)

    fun save(content: Content): Content = contentRepository.save(content)

    @Transactional
    fun updateContentToDelete(memberId: Long) {
        contentRepository.findByMemberId(memberId).forEach {
            it.status = ContentsStatus.DELETED
        }
    }

    fun findByAlias(alias: String): Content? = contentRepository.findByAliasAndStatusNot(alias, ContentsStatus.DELETED)

    /**
     * Content를 생성합니다.
     * @param request 생성 요청
     * @param memberId 작성자 ID
     * @param writerNickname 작성자 닉네임
     * @return 생성된 Content
     */
    @Transactional
    @CacheEvict(
        cacheNames = ["creatorContentsByMember"],
        key = "#memberId + '-' + #request.type.name()",
    )
    fun createContent(request: CreateContentRequest, memberId: Long, writerNickname: String): Content {
        // 1. Content 엔티티 생성 (임시 ID로 alias 생성)
        val tempContent = Content(
            alias = "", // 임시값, 저장 후 업데이트
            title = request.title ?: "",
            type = request.type,
            memberId = memberId,
            rule =
            if (request.type == ContentType.SCENARIO) {
                request.rule!!
            } else {
                Rule.RESOURCE
            },
            genre = request.genre ?: Genre.NONE,
            playerCountType = request.playerCountType ?: PlayerCountType.NONE,
            termType = request.termType ?: TermType.NONE,
            publishingInfo =
            if (request.type == ContentType.SCENARIO) {
                request.publishingInfo?.toJson()
            } else {
                request.publishingType?.let { listOf(PublishingInfo(type = it, path = null)).toJson() }
            },
            status = request.status,
            adult = request.adult,
            publishedAt = if (request.status == ContentsStatus.PUBLISHED) LocalDateTime.now() else request.publishedAt,
            freeContent = request.freeContent,
            nonFreeContent = null, // S3 업로드 후 업데이트
            price = request.price,
            level = request.level ?: 0,
            searchText = "", // 임시값, 나중에 업데이트
        )
        validatePublishing(tempContent)

        // 2. Content 저장하여 ID 획득
        val savedContent = contentRepository.save(tempContent)

        // 3. alias 생성 (memberId + contentId를 사용하여 5~10자 문자열)
        val alias = generateAlias(memberId, savedContent.id)
        savedContent.alias = alias
        // alias를 먼저 저장하여 연관 작품 저장 시 사용 가능하도록 함
        contentRepository.save(savedContent)

        // 4. non_free_content가 있으면 S3에 업로드
        val nonFreeContentS3Path = request.nonFreeContent?.let { content ->
            val txtBytes = content.toByteArray(Charsets.UTF_8)
            val fileName = "content_${savedContent.id}_${System.currentTimeMillis()}.txt"
            val inputStream = txtBytes.inputStream()
            s3Service.uploadFile(
                inputStream = inputStream,
                contentType = "text/plain",
                originalFileName = fileName,
                id = savedContent.id,
            )
        }

        // 5. tag 정보 저장
        request.tagIds?.let { tagIds ->
            if (tagIds.isNotEmpty()) {
                // 존재하는 tag만 필터링
                val existingTagIds = tagRepository.findAllById(tagIds)
                    .map { it.id }
                    .toSet()

                // ContentTag 저장
                val contentTags = tagIds
                    .filter { it in existingTagIds }
                    .map { tagId ->
                        ContentTag(
                            contentId = savedContent.id,
                            tagId = tagId,
                        )
                    }

                if (contentTags.isNotEmpty()) {
                    contentTagRepository.saveAll(contentTags)
                }
            }
        }

        // 6. 연관 작품 저장
        request.relatedContentIds?.let { relatedIds ->
            if (relatedIds.isNotEmpty()) {
                // 연관 작품 조회: relatedIds 기준, status가 PUBLISHED이고, type은 SCENARIO인 것만
                val existingContents = contentRepository.findAllById(relatedIds)
                    .filter {
                        it.status == ContentsStatus.PUBLISHED &&
                            it.type == ContentType.SCENARIO
                    }
                val existingContentIds = existingContents.map { it.id }.toSet()

                val relatedContents = relatedIds
                    .filter { it in existingContentIds }
                    .mapIndexed { index, relatedContentId ->
                        RelatedContent(
                            parentContentId = savedContent.id,
                            contentId = relatedContentId,
                            orderNo = index + 1,
                            path = "/content/${savedContent.alias}", // 기본 경로
                        )
                    }

                if (relatedContents.isNotEmpty()) {
                    relatedContentRepository.saveAll(relatedContents)
                }
            }
        }

        // 7. search_text 생성 (writer.nickname, content.title, tag정보, 장르, rule을 띄워쓰기로 이어붙임)
        val tagNames = request.tagIds?.let { tagIds ->
            tagRepository.findAllById(tagIds)
                .map { it.name }
        } ?: emptyList()

        val searchTextParts = mutableListOf<String>()
        searchTextParts.add(writerNickname)
        searchTextParts.add(savedContent.title)
        searchTextParts.addAll(tagNames)
        searchTextParts.add(request.genre?.displayName ?: "")
        searchTextParts.add(request.rule?.displayName ?: "")

        val searchText = searchTextParts.joinToString(" ")

        // 8. Content 업데이트
        savedContent.nonFreeContent = nonFreeContentS3Path
        savedContent.searchText = searchText

        return contentRepository.save(savedContent)
    }

    /**
     * Content를 수정합니다.
     * @param contentId 수정할 Content ID
     * @param request 수정 요청
     * @param memberId 작성자 ID
     * @param writerNickname 작성자 닉네임
     * @return 수정된 Content
     */
    @Transactional
    @CacheEvict(
        cacheNames = ["creatorContentsByMember"],
        key = "#memberId + '-' + #request.type.name()",
    )
    fun updateContent(
        contentId: Long,
        request: UpdateContentRequest,
        memberId: Long,
        writerNickname: String,
    ): Content {
        // 1. Content 조회
        val content = contentRepository.findById(contentId)
            .orElseThrow { IllegalArgumentException("Content를 찾을 수 없습니다: $contentId") }

        // 2. memberId 검증 (Controller에서도 체크하지만, Service에서도 한번 더 체크)
        if (content.memberId != memberId) {
            throw IllegalArgumentException("본인이 작성한 작품만 수정할 수 있습니다.")
        }

        // 3. Content 필드 업데이트 (alias는 수정하지 않음)
        request.title?.let { content.title = it }

        content.rule = if (request.type == ContentType.SCENARIO) request.rule ?: Rule.NONE else Rule.RESOURCE
        request.genre?.let { content.genre = it }
        request.playerCountType?.let { content.playerCountType = it }
        request.termType?.let { content.termType = it }
        content.publishingInfo =
            if (request.type == ContentType.SCENARIO) {
                request.publishingInfo?.toJson()
            } else {
                request.publishingType?.let { listOf(PublishingInfo(type = it, path = null)).toJson() }
            }
        content.status = request.status
        content.adult = request.adult
        content.publishedAt = request.publishedAt
        content.freeContent = request.freeContent
        content.price = request.price
        content.level = request.level ?: 0

        validatePublishing(content)

        // 4. non_free_content 처리
        // 기존 path가 있고, 새로운 nonFreeContent가 제공되면 S3에 업데이트
        val nonFreeContentS3Path = if (request.nonFreeContent != null) {
            content.nonFreeContent?.let {
                // 기존 path가 있으면 해당 path를 사용하여 업데이트
                val existingS3Key = s3Service.extractS3Key(it)
                val txtBytes = request.nonFreeContent.toByteArray(Charsets.UTF_8)
                val inputStream = txtBytes.inputStream()

                // 기존 key를 사용하여 파일 업데이트
                s3Service.updateFile(
                    inputStream = inputStream,
                    contentType = "text/plain",
                    s3Key = existingS3Key,
                )
                // 기존 path 반환 (동일한 path 사용)
                it
            } ?: run {
                // 기존 path가 없으면 새로 업로드
                val txtBytes = request.nonFreeContent.toByteArray(Charsets.UTF_8)
                val fileName = "content_${contentId}_${System.currentTimeMillis()}.txt"
                val inputStream = txtBytes.inputStream()
                s3Service.uploadFile(
                    inputStream = inputStream,
                    contentType = "text/plain",
                    originalFileName = fileName,
                    id = contentId,
                )
            }
        } else {
            // nonFreeContent가 null이면 기존 값 유지
            content.nonFreeContent
        }
        content.nonFreeContent = nonFreeContentS3Path

        // 6. 연관 작품 저장
        relatedContentRepository.deleteByContentId(contentId)
        request.relatedContentIds?.let { relatedIds ->
            if (relatedIds.isNotEmpty()) {
                // 연관 작품 조회: relatedIds 기준, status가 PUBLISHED이고, type은 SCENARIO인 것만
                val existingContents = contentRepository.findAllById(relatedIds)
                    .filter {
                        it.status == ContentsStatus.PUBLISHED &&
                            it.type == ContentType.SCENARIO
                    }
                val existingContentIds = existingContents.map { it.id }.toSet()

                val relatedContents = relatedIds
                    .filter { it in existingContentIds }
                    .mapIndexed { index, relatedContentId ->
                        RelatedContent(
                            parentContentId = content.id,
                            contentId = relatedContentId,
                            orderNo = index + 1,
                            path = "/content/${content.alias}", // 기본 경로
                        )
                    }

                if (relatedContents.isNotEmpty()) {
                    relatedContentRepository.saveAll(relatedContents)
                }
            }
        }

        // 5. tag 정보 삭제 후 신규 저장
        contentTagRepository.deleteByContentId(contentId)

        request.tagIds?.let { tagIds ->
            if (tagIds.isNotEmpty()) {
                // 존재하는 tag만 필터링
                val existingTagIds = tagRepository.findAllById(tagIds)
                    .map { it.id }
                    .toSet()

                // ContentTag 저장
                val contentTags = tagIds
                    .filter { it in existingTagIds }
                    .map { tagId ->
                        ContentTag(
                            contentId = contentId,
                            tagId = tagId,
                        )
                    }

                if (contentTags.isNotEmpty()) {
                    contentTagRepository.saveAll(contentTags)
                }
            }
        }

        // 6. search_text 생성 (writer.nickname, content.title, tag정보, 장르, rule을 띄워쓰기로 이어붙임)
        val tagNames = request.tagIds?.let { tagIds ->
            tagRepository.findAllById(tagIds)
                .map { it.name }
        } ?: emptyList()

        val searchTextParts = mutableListOf<String>()
        searchTextParts.add(writerNickname)
        searchTextParts.add(content.title)
        searchTextParts.addAll(tagNames)
        searchTextParts.add(request.genre?.displayName ?: "")
        searchTextParts.add(request.rule?.displayName ?: "")

        content.searchText = searchTextParts.joinToString(" ")

        // 7. Content 저장
        return contentRepository.save(content)
    }

    /**
     * memberId와 contentId를 사용하여 5~10자의 alias를 생성합니다.
     * 대소문자와 숫자만 사용합니다.
     */
    private fun generateAlias(memberId: Long, contentId: Long): String {
        val input = "${memberId}_${contentId}_${System.currentTimeMillis()}"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray())

        // Base62 인코딩 (대소문자, 숫자만 사용)
        val base62Chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        val sb = StringBuilder()

        var value = hashBytes.fold(0L) { acc, byte -> (acc shl 8) or (byte.toLong() and 0xFF) }
        if (value < 0) value = -value

        while (value > 0) {
            sb.append(base62Chars[(value % 62).toInt()])
            value /= 62
        }

        // 5~10자로 조정
        val alias = sb.toString().take(10)
        return if (alias.length < 5) {
            // 5자 미만이면 반복하여 5자 이상으로 만들기
            (alias + alias + alias).take(5)
        } else {
            alias
        }
    }

    @Transactional
    @CacheEvict(
        cacheNames = ["creatorContentsByMember"],
        key = "#memberId + '-' + #content.type.name()",
    )
    fun updateContentStatus(content: Content, memberId: Long, status: ContentsStatus): Content {
        if (content.memberId != memberId) {
            throw ContentException(
                "본인의 콘텐츠만 상태를 변경할 수 있습니다.",
                MessageCode.NOT_OWN_CONTENT,
            )
        }

        content.status = status
        return save(content)
    }

    @CacheEvict(
        cacheNames = ["creatorContentsByMember"],
        key = "#memberId + '-' + #content.type.name()",
    )
    fun deleteContent(content: Content, memberId: Long): Content {
        if (content.memberId != memberId) {
            throw ContentException(
                "본인의 콘텐츠만 삭제할 수 있습니다.",
                MessageCode.NOT_OWN_CONTENT,
            )
        }

        content.status = ContentsStatus.DELETED
        return save(content)
    }

    fun getRandomGenreContent(genres: String, isAdult: Boolean): Content {
        val genreNames: List<String> = Gson().fromJson(genres, typeToken)
        val genreList = genreNames.map { Genre.valueOf(it) }

        return contentRepository.findRandomGenreContent(genreList, isAdult)
    }

    fun getRandomRuleContent(rules: String, isAdult: Boolean): Content {
        val ruleNames: List<String> = Gson().fromJson(rules, typeToken)
        val ruleList = ruleNames.map { Rule.valueOf(it) }

        return contentRepository.findRandomRuleContent(ruleList, isAdult)
    }

    fun getRandomContent(isAdult: Boolean): Content = contentRepository.findRandomContent(isAdult)

    /**
     * non_free_content를 조회합니다.
     * @param contentId 콘텐츠 ID
     * @param memberId 회원 ID (null 가능)
     * @param isContentPurchased 구매 이력 확인 함수
     * @return S3에서 가져온 non_free_content 내용
     * @throws NotFoundException 콘텐츠를 찾을 수 없는 경우
     * @throws ContentException 구매가 필요한 경우
     */
    fun getNonFreeContent(contentId: Long, memberId: Long?, isContentPurchased: (Long, Long) -> Boolean): String {
        // 1. Content 조회
        val content = findById(contentId)
            ?: throw NotFoundException(
                "콘텐츠를 찾을 수 없습니다.",
                MessageCode.NOT_FOUND_CONTENT,
            )

        // 2. nonFreeContent가 null인 경우
        if (content.nonFreeContent == null) {
            return ""
        }

        // 3. 구매 이력 확인
        val hasPurchaseHistory = memberId?.let { isContentPurchased(it, contentId) } ?: false

        // 4. 구매 이력이 있으면 조회 가능
        // 5. 구매 이력이 없고 price가 0이면 조회 가능
        if (hasPurchaseHistory || content.price == 0.0) {
            return s3Service.getFileAsString(content.nonFreeContent!!)
        }

        // 6. 구매 이력이 없고 price가 0 초과면 예외 발생
        throw ContentException(
            "구매가 필요한 콘텐츠입니다.",
            MessageCode.PURCHASE_REQUIRED,
        )
    }

    fun validatePublishing(content: Content) {
        if (content.status == ContentsStatus.PUBLISHED) {
            check(content.title.isNotEmpty())
            check(content.rule != Rule.NONE)
            check(content.genre != Genre.NONE)
            check(content.playerCountType != PlayerCountType.NONE)
            check(content.termType != TermType.NONE)
        }
    }
}
