package com.termex.app.ui.viewmodel;

import com.termex.app.domain.KeyRepository;
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
public final class KeysViewModel_Factory implements Factory<KeysViewModel> {
  private final Provider<KeyRepository> keyRepositoryProvider;

  public KeysViewModel_Factory(Provider<KeyRepository> keyRepositoryProvider) {
    this.keyRepositoryProvider = keyRepositoryProvider;
  }

  @Override
  public KeysViewModel get() {
    return newInstance(keyRepositoryProvider.get());
  }

  public static KeysViewModel_Factory create(Provider<KeyRepository> keyRepositoryProvider) {
    return new KeysViewModel_Factory(keyRepositoryProvider);
  }

  public static KeysViewModel newInstance(KeyRepository keyRepository) {
    return new KeysViewModel(keyRepository);
  }
}
