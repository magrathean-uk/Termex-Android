package com.termex.app.di;

import com.termex.app.data.local.ServerDao;
import com.termex.app.data.local.TermexDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideServerDaoFactory implements Factory<ServerDao> {
  private final Provider<TermexDatabase> databaseProvider;

  public AppModule_ProvideServerDaoFactory(Provider<TermexDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public ServerDao get() {
    return provideServerDao(databaseProvider.get());
  }

  public static AppModule_ProvideServerDaoFactory create(
      Provider<TermexDatabase> databaseProvider) {
    return new AppModule_ProvideServerDaoFactory(databaseProvider);
  }

  public static ServerDao provideServerDao(TermexDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideServerDao(database));
  }
}
