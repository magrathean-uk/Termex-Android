package com.termex.app.di;

import com.termex.app.data.local.SnippetDao;
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
public final class AppModule_ProvideSnippetDaoFactory implements Factory<SnippetDao> {
  private final Provider<TermexDatabase> databaseProvider;

  public AppModule_ProvideSnippetDaoFactory(Provider<TermexDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public SnippetDao get() {
    return provideSnippetDao(databaseProvider.get());
  }

  public static AppModule_ProvideSnippetDaoFactory create(
      Provider<TermexDatabase> databaseProvider) {
    return new AppModule_ProvideSnippetDaoFactory(databaseProvider);
  }

  public static SnippetDao provideSnippetDao(TermexDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideSnippetDao(database));
  }
}
