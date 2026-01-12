package com.termex.app.core.billing;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class SubscriptionManager_Factory implements Factory<SubscriptionManager> {
  private final Provider<Context> contextProvider;

  public SubscriptionManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public SubscriptionManager get() {
    return newInstance(contextProvider.get());
  }

  public static SubscriptionManager_Factory create(Provider<Context> contextProvider) {
    return new SubscriptionManager_Factory(contextProvider);
  }

  public static SubscriptionManager newInstance(Context context) {
    return new SubscriptionManager(context);
  }
}
