package com.termex.app.data.repository;

import com.termex.app.data.local.ServerDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class ServerRepositoryImpl_Factory implements Factory<ServerRepositoryImpl> {
  private final Provider<ServerDao> serverDaoProvider;

  public ServerRepositoryImpl_Factory(Provider<ServerDao> serverDaoProvider) {
    this.serverDaoProvider = serverDaoProvider;
  }

  @Override
  public ServerRepositoryImpl get() {
    return newInstance(serverDaoProvider.get());
  }

  public static ServerRepositoryImpl_Factory create(Provider<ServerDao> serverDaoProvider) {
    return new ServerRepositoryImpl_Factory(serverDaoProvider);
  }

  public static ServerRepositoryImpl newInstance(ServerDao serverDao) {
    return new ServerRepositoryImpl(serverDao);
  }
}
