package org.project.discipline.domain.user.service

import org.project.discipline.domain.user.dto.OAuthLoginRequest
import org.project.discipline.domain.user.dto.OAuthLoginResponse
import org.project.discipline.domain.user.entity.User
import org.project.discipline.domain.user.entity.UserRole
import org.project.discipline.domain.user.repository.UserRepository
import org.project.discipline.security.service.JwtService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class OAuthLoginService(
    private val userRepository: UserRepository,
    private val jwtService: JwtService
) {
    private val logger = LoggerFactory.getLogger(OAuthLoginService::class.java)

    /**
     * 클라이언트 OAuth 로그인 처리
     * 1. 기존 사용자 조회 (providerId + provider 또는 email로)
     * 2. 사용자가 없으면 새로 생성
     * 3. JWT 토큰 생성 및 반환
     */
    fun processOAuthLogin(request: OAuthLoginRequest): OAuthLoginResponse {
        logger.info("OAuth 로그인 처리 시작: provider=${request.provider}, email=${request.email}")

        val (user, isNewUser) = findOrCreateUser(request)

        // 2. JWT 토큰 생성
        val token = jwtService.generateToken(user)

        // 3. 응답 생성
        val userInfo = OAuthLoginResponse.UserInfo(
            id = user.id,
            email = user.email,
            name = user.name,
            provider = user.provider,
            providerId = user.providerId,
            picture = user.picture,
            roles = listOf(user.role.name),
            isAdmin = user.role == UserRole.ADMIN
        )

        logger.info("OAuth 로그인 성공: userId=${user.id}, isNewUser=$isNewUser")

        return OAuthLoginResponse(
            success = true,
            token = token,
            user = userInfo,
            message = if (isNewUser) "새 계정이 생성되었습니다." else "로그인 성공",
            isNewUser = isNewUser
        )
    }

    /**
     * 사용자 조회 또는 생성
     * @return Pair<User, Boolean> - (사용자, 새로생성여부)
     */
    private fun findOrCreateUser(request: OAuthLoginRequest): Pair<User, Boolean> {
        // 1. provider + providerId로 먼저 조회 (가장 정확한 매칭)
        val userByProviderInfo = userRepository.findByProviderAndProviderId(request.provider, request.providerId)

        if (userByProviderInfo != null) {
            logger.info("기존 사용자 발견 (provider+providerId): id=${userByProviderInfo.id}, email=${userByProviderInfo.email}")

            // OAuth 정보 업데이트 (이름, 이메일, 프로필 사진이 변경될 수 있음)
            updateUserInfo(userByProviderInfo, request)
            val updatedUser = userRepository.save(userByProviderInfo)

            return Pair(updatedUser, false)
        }

        // 2. email + provider로 조회 (같은 이메일, 같은 provider로 이전에 가입한 경우)
        val userByEmailAndProvider = userRepository.findByEmailAndProvider(request.email, request.provider)

        if (userByEmailAndProvider != null) {
            logger.info("기존 사용자 발견 (email+provider): id=${userByEmailAndProvider.id}, email=${userByEmailAndProvider.email}")

            // providerId가 다른 경우 - 같은 provider에서 계정 변경 등의 상황
            logger.warn(
                "동일한 email+provider이지만 다른 providerId: " +
                        "기존=${userByEmailAndProvider.providerId}, 새로운=${request.providerId}"
            )

            // 이 경우에도 이름, 프로필 사진 등은 업데이트 (providerId는 업데이트하지 않음)
            updateUserInfoExceptProviderId(userByEmailAndProvider, request)
            val updatedUser = userRepository.save(userByEmailAndProvider)

            return Pair(updatedUser, false)
        }

        // 3. 새 사용자 생성
        logger.info("새 사용자 생성: email=${request.email}, provider=${request.provider}")

        val newUser = User(
            email = request.email,
            name = request.name,
            picture = request.picture,
            provider = request.provider,
            providerId = request.providerId,
            role = UserRole.USER
        )

        val savedUser = userRepository.save(newUser)
        logger.info("새 사용자 생성 완료: id=${savedUser.id}")

        return Pair(savedUser, true)
    }

    /**
     * 기존 사용자의 OAuth 정보 업데이트 (전체)
     */
    private fun updateUserInfo(user: User, request: OAuthLoginRequest) {
        val hasChanges = user.email != request.email ||
                user.name != request.name ||
                user.picture != request.picture

        if (hasChanges) {
            logger.info("사용자 정보 업데이트: id=${user.id}")
            logger.debug("  - 이메일: ${user.email} → ${request.email}")
            logger.debug("  - 이름: ${user.name} → ${request.name}")
            logger.debug("  - 사진: ${user.picture} → ${request.picture}")

            user.updateOAuthInfo(request.email, request.name, request.picture)
        } else {
            logger.debug("사용자 정보 변경사항 없음: id=${user.id}")
        }
    }

    /**
     * 기존 사용자의 OAuth 정보 업데이트 (providerId 제외)
     * providerId가 다른 경우, providerId는 업데이트하지 않고 다른 정보만 업데이트
     */
    private fun updateUserInfoExceptProviderId(user: User, request: OAuthLoginRequest) {
        val hasChanges = user.name != request.name || user.picture != request.picture

        if (hasChanges) {
            logger.info("사용자 정보 업데이트 (providerId 제외): id=${user.id}")
            logger.debug("  - 이름: ${user.name} → ${request.name}")
            logger.debug("  - 사진: ${user.picture} → ${request.picture}")

            // email과 providerId는 기존 값 유지, name과 picture만 업데이트
            user.updateOAuthInfo(user.email, request.name, request.picture)
        } else {
            logger.debug("사용자 정보 변경사항 없음 (providerId 제외): id=${user.id}")
        }
    }

    /**
     * 지원되는 OAuth 제공자 목록 반환
     */
    fun getSupportedProviders(): List<String> {
        return listOf("google", "apple")
    }
} 