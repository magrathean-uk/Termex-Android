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
public final class AppViewModel_Factory implements Factory<AppViewModel> {
  private final Provider<UserPreferencesRepository> userPreferencesRepositoryProvider;

  private final Provider<SubscriptionManager> subscriptionManagerProvider;

  public AppViewModel_Factory(Provider<UserPreferencesRepository> userPreferencesRepositoryProvider,
      Provider<SubscriptionManager> subscriptionManagerProvider) {
    this.userPreferencesRepositoryProvider = userPreferencesRepositoryProvider;
    this.subscriptionManagerProvider = subscriptionManagerProvider;
  }

  @Override
  public AppViewModel get() {
    return newInstance(userPreferencesRepositoryProvider.get(), subscriptionManagerProvider.get());
  }

  public static AppViewModel_Factory create(
      Provider<UserPreferencesRepository> userPreferencesRepositoryProvider,
      Provider<SubscriptionManager> subscriptionManagerProvider) {
    return new AppViewModel_Factory(userPreferencesRepositoryProvider, subscriptionManagerProvider);
  }

  public static AppViewModel newInstance(UserPreferencesRepository userPreferencesRepository,
      SubscriptionManager subscriptionManager) {
    return new AppViewModel(userPreferencesRepository, subscriptionManager);
  }
}
