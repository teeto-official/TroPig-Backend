package com.tropig.backend.banner.service

import com.tropig.backend.banner.entity.Banner
import com.tropig.backend.banner.enums.BannerDevice
import com.tropig.backend.banner.model.request.BannerImageUploadPresignerRequest
import com.tropig.backend.banner.model.request.CreateBannerRequest
import com.tropig.backend.banner.model.request.UpdateBannerRequest
import com.tropig.backend.banner.model.response.*
import com.tropig.backend.banner.repository.BannerRepository
import com.tropig.backend.common.enums.MessageCode
import com.tropig.backend.common.exception.NotFoundException
import com.tropig.backend.contents.service.S3Service
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID
import com.tropig.backend.common.exception.IllegalArgumentException as TroPigIllegalArgumentException

@Service
class BannerService(private val bannerRepository: BannerRepository, private val s3Service: S3Service) {
    companion object {
        private val logger = LoggerFactory.getLogger(BannerService::class.java)
        private const val ALIAS_PREFIX = "banner-"
        private const val ALIAS_RANDOM_LENGTH = 12
        private const val ALIAS_GENERATE_ATTEMPTS = 5
    }

    @Cacheable(cacheNames = ["bannerDisplay"], key = "#device.name()")
    fun getDisplayBanners(device: BannerDevice): List<BannerResponse> =
        bannerRepository.findDisplayable(LocalDateTime.now())
            .map { it.toDisplayResponse(device, s3Service) }

    @Cacheable(cacheNames = ["banner"], key = "#alias")
    fun getDisplayBanner(alias: String): BannerDetailResponse? {
        val banner = bannerRepository.findDisplayableBannerByAlias(alias, LocalDateTime.now())
        return banner?.toDetailResponse(s3Service)
    }

    fun getAdminBanners(): List<AdminBannerResponse> =
        bannerRepository.findAllByDeletedAtIsNullOrderByOrderNoAscIdDesc()
            .map { it.toAdminResponse(s3Service) }

    fun getAdminBanner(id: Long): AdminBannerResponse = findActiveBanner(id).toAdminResponse(s3Service)

    @Transactional
    @CacheEvict(cacheNames = ["bannerDisplay", "banner", "bannerHtml"], allEntries = true)
    fun createBanner(request: CreateBannerRequest, adminId: Long): AdminBannerResponse {
        validateSchedule(request.startedAt, request.endedAt)
        validateRequired(request.title, "title")
        validateRequired(request.pcImagePath, "pcImagePath")

        val banner = Banner(
            alias = generateAlias(),
            title = request.title,
            subtitle = request.subtitle,
            type = request.type,
            pcImagePath = normalizePath(request.pcImagePath) ?: request.pcImagePath,
            mobileImagePath = normalizePath(request.mobileImagePath),
            htmlPath = normalizePath(request.htmlPath),
            startedAt = request.startedAt,
            endedAt = request.endedAt,
            orderNo = request.orderNo,
            show = request.show,
            lastModifiedAdminId = adminId,
        )

        return bannerRepository.save(banner).toAdminResponse(s3Service)
    }

    @Transactional
    @CacheEvict(cacheNames = ["bannerDisplay", "banner", "bannerHtml"], allEntries = true)
    fun updateBanner(id: Long, request: UpdateBannerRequest, adminId: Long): AdminBannerResponse {
        val banner = findActiveBanner(id)
        val previousPcImagePath = banner.pcImagePath
        val previousMobileImagePath = banner.mobileImagePath
        val previousPcHtmlPath = banner.htmlPath

        request.title?.let {
            validateRequired(it, "title")
            banner.title = it
        }
        request.subtitle?.let { banner.subtitle = it.takeUnless { value -> value.isBlank() } }
        request.type?.let { banner.type = it }
        request.pcImagePath?.let {
            validateRequired(it, "pcImagePath")
            banner.pcImagePath = normalizePath(it) ?: it
        }
        request.mobileImagePath?.let { banner.mobileImagePath = normalizePath(it) }
        request.htmlPath?.let { banner.htmlPath = normalizePath(it) }
        request.startedAt?.let { banner.startedAt = it }
        request.endedAt?.let { banner.endedAt = it }
        request.orderNo?.let { banner.orderNo = it }
        request.show?.let { banner.show = it }

        validateSchedule(banner.startedAt, banner.endedAt)
        banner.lastModifiedAdminId = adminId
        banner.updatedAt = LocalDateTime.now()

        val savedBanner = bannerRepository.save(banner)
        deleteChangedFiles(
            previousPcImagePath to savedBanner.pcImagePath,
            previousMobileImagePath to savedBanner.mobileImagePath,
            previousPcHtmlPath to savedBanner.htmlPath,
        )

        return savedBanner.toAdminResponse(s3Service)
    }

    @Transactional
    @CacheEvict(cacheNames = ["bannerDisplay", "banner", "bannerHtml"], allEntries = true)
    fun deleteBanner(id: Long, adminId: Long) {
        val banner = findActiveBanner(id)
        val now = LocalDateTime.now()

        banner.deletedAt = now
        banner.updatedAt = now
        banner.lastModifiedAdminId = adminId
        bannerRepository.save(banner)

        deleteFiles(
            banner.pcImagePath,
            banner.mobileImagePath,
            banner.htmlPath,
        )
    }

    fun createImageUploadPresigner(request: BannerImageUploadPresignerRequest): BannerImageUploadPresignerResponse {
        validateImage(request.contentType)
        validateRequired(request.fileName, "fileName")

        val extension = s3Service.extractExtension(request.fileName)
        val s3Key = "banner/${UUID.randomUUID()}.$extension"
        val presignedUrl = s3Service.generateUploadPresignerUrl(s3Key, request.contentType)

        return BannerImageUploadPresignerResponse(
            presignedUrl = presignedUrl,
            s3Key = s3Key,
            publicUrl = s3Service.toUrl(s3Key) ?: s3Key,
            expiresInSeconds = 300,
            uuid = request.uuid,
        )
    }

    fun createHtmlUploadPresigner(request: BannerImageUploadPresignerRequest): BannerImageUploadPresignerResponse {
        validateHtml(request.contentType)
        validateRequired(request.fileName, "fileName")

        val s3Key = "banner/html/${UUID.randomUUID()}.html"
        val presignedUrl = s3Service.generateUploadPresignerUrl(s3Key, request.contentType)

        return BannerImageUploadPresignerResponse(
            presignedUrl = presignedUrl,
            s3Key = s3Key,
            publicUrl = s3Service.toUrl(s3Key) ?: s3Key,
            expiresInSeconds = 300,
            uuid = request.uuid,
        )
    }

    @CacheEvict(cacheNames = ["bannerDisplay", "bannerHtml"], allEntries = true)
    fun deleteImage(path: String) {
        val normalizedPath = normalizePath(path) ?: throwInvalid("path가 비어 있습니다.")
        s3Service.deleteFile(s3Service.extractS3Key(normalizedPath))
    }

    private fun findActiveBanner(id: Long): Banner = bannerRepository.findByIdAndDeletedAtIsNull(id)
        ?: throw NotFoundException("배너를 찾을 수 없습니다. id: $id", MessageCode.NOT_FOUND_BANNER)

    private fun generateAlias(): String {
        repeat(ALIAS_GENERATE_ATTEMPTS) {
            val alias = ALIAS_PREFIX + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .take(ALIAS_RANDOM_LENGTH)

            if (!bannerRepository.existsByAliasAndDeletedAtIsNull(alias)) {
                return alias
            }
        }

        throwInvalid("배너 alias 생성에 실패했습니다.")
    }

    private fun validateSchedule(startedAt: LocalDateTime, endedAt: LocalDateTime) {
        if (!startedAt.isBefore(endedAt)) {
            throwInvalid("startedAt은 endedAt보다 이전이어야 합니다.")
        }
    }

    private fun validateImage(contentType: String) {
        if (!contentType.lowercase().startsWith("image/")) {
            throwInvalid("이미지 파일만 업로드할 수 있습니다.")
        }
    }

    private fun validateHtml(contentType: String) {
        if (!contentType.lowercase().startsWith("text/html")) {
            throwInvalid("HTML 파일만 업로드할 수 있습니다.")
        }
    }

    private fun validateRequired(value: String, fieldName: String) {
        if (value.isBlank()) {
            throwInvalid("$fieldName 값이 비어 있습니다.")
        }
    }

    private fun normalizePath(path: String?): String? = path
        ?.trim()
        ?.takeUnless { it.isBlank() }
        ?.let {
            when {
                it.contains("amazonaws.com/") -> s3Service.extractS3Key(it)
                it.startsWith("http://") || it.startsWith("https://") -> {
                    throwInvalid("S3 URL 또는 S3 key만 사용할 수 있습니다.")
                }

                else -> it
            }
        }

    private fun deleteFiles(vararg paths: String?) {
        paths.mapNotNull { normalizePath(it) }
            .distinct()
            .forEach { s3Service.deleteFile(it) }
    }

    private fun deleteChangedFiles(vararg pathPairs: Pair<String?, String?>) {
        pathPairs
            .mapNotNull { (previousPath, currentPath) ->
                val previous = normalizePath(previousPath)
                val current = normalizePath(currentPath)

                previous?.takeIf { it != current }
            }
            .distinct()
            .forEach { path ->
                runCatching {
                    s3Service.deleteFile(path)
                }.onFailure {
                    logger.warn("배너 기존 이미지 삭제 실패: path=$path", it)
                }
            }
    }

    private fun throwInvalid(message: String): Nothing = throw TroPigIllegalArgumentException(
        message,
        MessageCode.INVALID_PARAMS,
    )
}
