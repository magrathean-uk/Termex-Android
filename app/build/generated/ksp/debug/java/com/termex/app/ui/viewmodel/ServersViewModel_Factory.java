package com.termex.app.ui.viewmodel;

import com.termex.app.domain.ServerRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class ServersViewModel_Factory implements Factory<ServersViewModel> {
  private final Provider<ServerRepository> serverRepositoryProvider;

  public ServersViewModel_Factory(Provider<ServerRepository> serverRepositoryProvider) {
    this.serverRepositoryProvider = serverRepositoryProvider;
  }

  @Override
  public ServersViewModel get() {
    return newInstance(serverRepositoryProvider.get());
  }

  public static ServersViewModel_Factory create(
      Provider<ServerRepository> serverRepositoryProvider) {
    return new ServersViewModel_Factory(serverRepositoryProvider);
  }

  public static ServersViewModel newInstance(ServerRepository serverRepository) {
    return new ServersViewModel(serverRepository);
  }
}
