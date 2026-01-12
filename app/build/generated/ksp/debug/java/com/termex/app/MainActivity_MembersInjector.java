package com.termex.app;

import com.termex.app.data.prefs.UserPreferencesRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<UserPreferencesRepository> userPreferencesRepositoryProvider;

  public MainActivity_MembersInjector(
      Provider<UserPreferencesRepository> userPreferencesRepositoryProvider) {
    this.userPreferencesRepositoryProvider = userPreferencesRepositoryProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<UserPreferencesRepository> userPreferencesRepositoryProvider) {
    return new MainActivity_MembersInjector(userPreferencesRepositoryProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectUserPreferencesRepository(instance, userPreferencesRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.termex.app.MainActivity.userPreferencesRepository")
  public static void injectUserPreferencesRepository(MainActivity instance,
      UserPreferencesRepository userPreferencesRepository) {
    instance.userPreferencesRepository = userPreferencesRepository;
  }
}
