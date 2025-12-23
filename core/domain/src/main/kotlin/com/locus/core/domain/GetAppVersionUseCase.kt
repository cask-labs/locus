package com.locus.core.domain

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
