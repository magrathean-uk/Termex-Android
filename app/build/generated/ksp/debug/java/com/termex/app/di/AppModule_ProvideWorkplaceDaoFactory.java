package com.termex.app.di;

import com.termex.app.data.local.TermexDatabase;
import com.termex.app.data.local.WorkplaceDao;
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
public final class AppModule_ProvideWorkplaceDaoFactory implements Factory<WorkplaceDao> {
  private final Provider<TermexDatabase> databaseProvider;

  public AppModule_ProvideWorkplaceDaoFactory(Provider<TermexDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public WorkplaceDao get() {
    return provideWorkplaceDao(databaseProvider.get());
  }

  public static AppModule_ProvideWorkplaceDaoFactory create(
      Provider<TermexDatabase> databaseProvider) {
    return new AppModule_ProvideWorkplaceDaoFactory(databaseProvider);
  }

  public static WorkplaceDao provideWorkplaceDao(TermexDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideWorkplaceDao(database));
  }
}
