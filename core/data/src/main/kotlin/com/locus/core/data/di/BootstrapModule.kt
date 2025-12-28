package com.locus.core.data.di

import com.locus.core.data.source.remote.aws.AssetTemplateResourceProvider
import com.locus.core.data.source.remote.aws.CloudFormationClient
import com.locus.core.data.source.remote.aws.InfrastructureProvisioner
import com.locus.core.data.source.remote.aws.TemplateResourceProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class BootstrapModule {
    @Binds
    abstract fun bindInfrastructureProvisioner(client: CloudFormationClient): InfrastructureProvisioner

    @Binds
    abstract fun bindTemplateResourceProvider(provider: AssetTemplateResourceProvider): TemplateResourceProvider
}
