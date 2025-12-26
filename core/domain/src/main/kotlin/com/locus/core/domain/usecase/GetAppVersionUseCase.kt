package com.locus.core.domain.usecase

import com.locus.core.domain.AppVersion
import com.locus.core.domain.UseCase
import com.locus.core.domain.repository.AppVersionRepository
import com.locus.core.domain.result.LocusResult
import javax.inject.Inject

class GetAppVersionUseCase
    @Inject
    constructor(
        private val repository: AppVersionRepository,
    ) : UseCase {
        suspend operator fun invoke(): LocusResult<AppVersion> {
            return repository.getAppVersion()
        }
    }
