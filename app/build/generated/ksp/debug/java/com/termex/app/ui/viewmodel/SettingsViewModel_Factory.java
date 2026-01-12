package com.termex.app.ui.viewmodel;

import com.termex.app.core.billing.SubscriptionManager;
import com.termex.app.data.prefs.UserPreferencesRepository;
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<UserPreferencesRepository> userPreferencesRepositoryProvider;

  private final Provider<SubscriptionManager> subscriptionManagerProvider;

  public SettingsViewModel_Factory(
      Provider<UserPreferencesRepository> userPreferencesRepositoryProvider,
      Provider<SubscriptionManager> subscriptionManagerProvider) {
    this.userPreferencesRepositoryProvider = userPreferencesRepositoryProvider;
    this.subscriptionManagerProvider = subscriptionManagerProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(userPreferencesRepositoryProvider.get(), subscriptionManagerProvider.get());
  }

  public static SettingsViewModel_Factory create(
      Provider<UserPreferencesRepository> userPreferencesRepositoryProvider,
      Provider<SubscriptionManager> subscriptionManagerProvider) {
    return new SettingsViewModel_Factory(userPreferencesRepositoryProvider, subscriptionManagerProvider);
  }

  public static SettingsViewModel newInstance(UserPreferencesRepository userPreferencesRepository,
      SubscriptionManager subscriptionManager) {
    return new SettingsViewModel(userPreferencesRepository, subscriptionManager);
  }
}
