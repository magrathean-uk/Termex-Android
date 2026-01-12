package com.termex.app.data.repository;

import com.termex.app.data.local.SnippetDao;
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
public final class SnippetRepositoryImpl_Factory implements Factory<SnippetRepositoryImpl> {
  private final Provider<SnippetDao> snippetDaoProvider;

  public SnippetRepositoryImpl_Factory(Provider<SnippetDao> snippetDaoProvider) {
    this.snippetDaoProvider = snippetDaoProvider;
  }

  @Override
  public SnippetRepositoryImpl get() {
    return newInstance(snippetDaoProvider.get());
  }

  public static SnippetRepositoryImpl_Factory create(Provider<SnippetDao> snippetDaoProvider) {
    return new SnippetRepositoryImpl_Factory(snippetDaoProvider);
  }

  public static SnippetRepositoryImpl newInstance(SnippetDao snippetDao) {
    return new SnippetRepositoryImpl(snippetDao);
  }
}
