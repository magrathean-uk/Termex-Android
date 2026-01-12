package com.termex.app.ui.viewmodel;

import com.termex.app.domain.SnippetRepository;
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
public final class SnippetsViewModel_Factory implements Factory<SnippetsViewModel> {
  private final Provider<SnippetRepository> snippetRepositoryProvider;

  public SnippetsViewModel_Factory(Provider<SnippetRepository> snippetRepositoryProvider) {
    this.snippetRepositoryProvider = snippetRepositoryProvider;
  }

  @Override
  public SnippetsViewModel get() {
    return newInstance(snippetRepositoryProvider.get());
  }

  public static SnippetsViewModel_Factory create(
      Provider<SnippetRepository> snippetRepositoryProvider) {
    return new SnippetsViewModel_Factory(snippetRepositoryProvider);
  }

  public static SnippetsViewModel newInstance(SnippetRepository snippetRepository) {
    return new SnippetsViewModel(snippetRepository);
  }
}
