package com.termex.app;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.termex.app.core.billing.SubscriptionManager;
import com.termex.app.core.ssh.SSHClient;
import com.termex.app.data.local.ServerDao;
import com.termex.app.data.local.SnippetDao;
import com.termex.app.data.local.TermexDatabase;
import com.termex.app.data.prefs.UserPreferencesRepository;
import com.termex.app.data.repository.KeyRepositoryImpl;
import com.termex.app.data.repository.ServerRepositoryImpl;
import com.termex.app.data.repository.SnippetRepositoryImpl;
import com.termex.app.di.AppModule_ProvideDataStoreFactory;
import com.termex.app.di.AppModule_ProvideDatabaseFactory;
import com.termex.app.di.AppModule_ProvideServerDaoFactory;
import com.termex.app.di.AppModule_ProvideSnippetDaoFactory;
import com.termex.app.ui.viewmodel.AppViewModel;
import com.termex.app.ui.viewmodel.AppViewModel_HiltModules;
import com.termex.app.ui.viewmodel.KeysViewModel;
import com.termex.app.ui.viewmodel.KeysViewModel_HiltModules;
import com.termex.app.ui.viewmodel.ServersViewModel;
import com.termex.app.ui.viewmodel.ServersViewModel_HiltModules;
import com.termex.app.ui.viewmodel.SettingsViewModel;
import com.termex.app.ui.viewmodel.SettingsViewModel_HiltModules;
import com.termex.app.ui.viewmodel.SnippetsViewModel;
import com.termex.app.ui.viewmodel.SnippetsViewModel_HiltModules;
import com.termex.app.ui.viewmodel.TerminalViewModel;
import com.termex.app.ui.viewmodel.TerminalViewModel_HiltModules;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.IdentifierNameString;
import dagger.internal.KeepFieldType;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.MapBuilder;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

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
public final class DaggerTermexApplication_HiltComponents_SingletonC {
  private DaggerTermexApplication_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public TermexApplication_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements TermexApplication_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public TermexApplication_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements TermexApplication_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public TermexApplication_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements TermexApplication_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public TermexApplication_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements TermexApplication_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public TermexApplication_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements TermexApplication_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public TermexApplication_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements TermexApplication_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public TermexApplication_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements TermexApplication_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public TermexApplication_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends TermexApplication_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends TermexApplication_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends TermexApplication_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends TermexApplication_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
      injectMainActivity2(mainActivity);
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(MapBuilder.<String, Boolean>newMapBuilder(6).put(LazyClassKeyProvider.com_termex_app_ui_viewmodel_AppViewModel, AppViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_termex_app_ui_viewmodel_KeysViewModel, KeysViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_termex_app_ui_viewmodel_ServersViewModel, ServersViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_termex_app_ui_viewmodel_SettingsViewModel, SettingsViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_termex_app_ui_viewmodel_SnippetsViewModel, SnippetsViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_termex_app_ui_viewmodel_TerminalViewModel, TerminalViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @CanIgnoreReturnValue
    private MainActivity injectMainActivity2(MainActivity instance) {
      MainActivity_MembersInjector.injectUserPreferencesRepository(instance, singletonCImpl.userPreferencesRepositoryProvider.get());
      return instance;
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_termex_app_ui_viewmodel_KeysViewModel = "com.termex.app.ui.viewmodel.KeysViewModel";

      static String com_termex_app_ui_viewmodel_ServersViewModel = "com.termex.app.ui.viewmodel.ServersViewModel";

      static String com_termex_app_ui_viewmodel_SettingsViewModel = "com.termex.app.ui.viewmodel.SettingsViewModel";

      static String com_termex_app_ui_viewmodel_AppViewModel = "com.termex.app.ui.viewmodel.AppViewModel";

      static String com_termex_app_ui_viewmodel_SnippetsViewModel = "com.termex.app.ui.viewmodel.SnippetsViewModel";

      static String com_termex_app_ui_viewmodel_TerminalViewModel = "com.termex.app.ui.viewmodel.TerminalViewModel";

      @KeepFieldType
      KeysViewModel com_termex_app_ui_viewmodel_KeysViewModel2;

      @KeepFieldType
      ServersViewModel com_termex_app_ui_viewmodel_ServersViewModel2;

      @KeepFieldType
      SettingsViewModel com_termex_app_ui_viewmodel_SettingsViewModel2;

      @KeepFieldType
      AppViewModel com_termex_app_ui_viewmodel_AppViewModel2;

      @KeepFieldType
      SnippetsViewModel com_termex_app_ui_viewmodel_SnippetsViewModel2;

      @KeepFieldType
      TerminalViewModel com_termex_app_ui_viewmodel_TerminalViewModel2;
    }
  }

  private static final class ViewModelCImpl extends TermexApplication_HiltComponents.ViewModelC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<AppViewModel> appViewModelProvider;

    private Provider<KeysViewModel> keysViewModelProvider;

    private Provider<ServersViewModel> serversViewModelProvider;

    private Provider<SettingsViewModel> settingsViewModelProvider;

    private Provider<SnippetsViewModel> snippetsViewModelProvider;

    private Provider<TerminalViewModel> terminalViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;

      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.appViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.keysViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.serversViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.settingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.snippetsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.terminalViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(MapBuilder.<String, javax.inject.Provider<ViewModel>>newMapBuilder(6).put(LazyClassKeyProvider.com_termex_app_ui_viewmodel_AppViewModel, ((Provider) appViewModelProvider)).put(LazyClassKeyProvider.com_termex_app_ui_viewmodel_KeysViewModel, ((Provider) keysViewModelProvider)).put(LazyClassKeyProvider.com_termex_app_ui_viewmodel_ServersViewModel, ((Provider) serversViewModelProvider)).put(LazyClassKeyProvider.com_termex_app_ui_viewmodel_SettingsViewModel, ((Provider) settingsViewModelProvider)).put(LazyClassKeyProvider.com_termex_app_ui_viewmodel_SnippetsViewModel, ((Provider) snippetsViewModelProvider)).put(LazyClassKeyProvider.com_termex_app_ui_viewmodel_TerminalViewModel, ((Provider) terminalViewModelProvider)).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return Collections.<Class<?>, Object>emptyMap();
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_termex_app_ui_viewmodel_TerminalViewModel = "com.termex.app.ui.viewmodel.TerminalViewModel";

      static String com_termex_app_ui_viewmodel_SettingsViewModel = "com.termex.app.ui.viewmodel.SettingsViewModel";

      static String com_termex_app_ui_viewmodel_AppViewModel = "com.termex.app.ui.viewmodel.AppViewModel";

      static String com_termex_app_ui_viewmodel_KeysViewModel = "com.termex.app.ui.viewmodel.KeysViewModel";

      static String com_termex_app_ui_viewmodel_SnippetsViewModel = "com.termex.app.ui.viewmodel.SnippetsViewModel";

      static String com_termex_app_ui_viewmodel_ServersViewModel = "com.termex.app.ui.viewmodel.ServersViewModel";

      @KeepFieldType
      TerminalViewModel com_termex_app_ui_viewmodel_TerminalViewModel2;

      @KeepFieldType
      SettingsViewModel com_termex_app_ui_viewmodel_SettingsViewModel2;

      @KeepFieldType
      AppViewModel com_termex_app_ui_viewmodel_AppViewModel2;

      @KeepFieldType
      KeysViewModel com_termex_app_ui_viewmodel_KeysViewModel2;

      @KeepFieldType
      SnippetsViewModel com_termex_app_ui_viewmodel_SnippetsViewModel2;

      @KeepFieldType
      ServersViewModel com_termex_app_ui_viewmodel_ServersViewModel2;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.termex.app.ui.viewmodel.AppViewModel 
          return (T) new AppViewModel(singletonCImpl.userPreferencesRepositoryProvider.get(), singletonCImpl.subscriptionManagerProvider.get());

          case 1: // com.termex.app.ui.viewmodel.KeysViewModel 
          return (T) new KeysViewModel(singletonCImpl.keyRepositoryImplProvider.get());

          case 2: // com.termex.app.ui.viewmodel.ServersViewModel 
          return (T) new ServersViewModel(singletonCImpl.serverRepositoryImplProvider.get());

          case 3: // com.termex.app.ui.viewmodel.SettingsViewModel 
          return (T) new SettingsViewModel(singletonCImpl.userPreferencesRepositoryProvider.get(), singletonCImpl.subscriptionManagerProvider.get());

          case 4: // com.termex.app.ui.viewmodel.SnippetsViewModel 
          return (T) new SnippetsViewModel(singletonCImpl.snippetRepositoryImplProvider.get());

          case 5: // com.termex.app.ui.viewmodel.TerminalViewModel 
          return (T) new TerminalViewModel(singletonCImpl.sSHClientProvider.get(), singletonCImpl.serverRepositoryImplProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends TermexApplication_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends TermexApplication_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }
  }

  private static final class SingletonCImpl extends TermexApplication_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<DataStore<Preferences>> provideDataStoreProvider;

    private Provider<UserPreferencesRepository> userPreferencesRepositoryProvider;

    private Provider<SubscriptionManager> subscriptionManagerProvider;

    private Provider<KeyRepositoryImpl> keyRepositoryImplProvider;

    private Provider<TermexDatabase> provideDatabaseProvider;

    private Provider<ServerRepositoryImpl> serverRepositoryImplProvider;

    private Provider<SnippetRepositoryImpl> snippetRepositoryImplProvider;

    private Provider<SSHClient> sSHClientProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    private ServerDao serverDao() {
      return AppModule_ProvideServerDaoFactory.provideServerDao(provideDatabaseProvider.get());
    }

    private SnippetDao snippetDao() {
      return AppModule_ProvideSnippetDaoFactory.provideSnippetDao(provideDatabaseProvider.get());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.provideDataStoreProvider = DoubleCheck.provider(new SwitchingProvider<DataStore<Preferences>>(singletonCImpl, 1));
      this.userPreferencesRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<UserPreferencesRepository>(singletonCImpl, 0));
      this.subscriptionManagerProvider = DoubleCheck.provider(new SwitchingProvider<SubscriptionManager>(singletonCImpl, 2));
      this.keyRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<KeyRepositoryImpl>(singletonCImpl, 3));
      this.provideDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<TermexDatabase>(singletonCImpl, 5));
      this.serverRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<ServerRepositoryImpl>(singletonCImpl, 4));
      this.snippetRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<SnippetRepositoryImpl>(singletonCImpl, 6));
      this.sSHClientProvider = DoubleCheck.provider(new SwitchingProvider<SSHClient>(singletonCImpl, 7));
    }

    @Override
    public void injectTermexApplication(TermexApplication termexApplication) {
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return Collections.<Boolean>emptySet();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.termex.app.data.prefs.UserPreferencesRepository 
          return (T) new UserPreferencesRepository(singletonCImpl.provideDataStoreProvider.get());

          case 1: // androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> 
          return (T) AppModule_ProvideDataStoreFactory.provideDataStore(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 2: // com.termex.app.core.billing.SubscriptionManager 
          return (T) new SubscriptionManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 3: // com.termex.app.data.repository.KeyRepositoryImpl 
          return (T) new KeyRepositoryImpl(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 4: // com.termex.app.data.repository.ServerRepositoryImpl 
          return (T) new ServerRepositoryImpl(singletonCImpl.serverDao());

          case 5: // com.termex.app.data.local.TermexDatabase 
          return (T) AppModule_ProvideDatabaseFactory.provideDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 6: // com.termex.app.data.repository.SnippetRepositoryImpl 
          return (T) new SnippetRepositoryImpl(singletonCImpl.snippetDao());

          case 7: // com.termex.app.core.ssh.SSHClient 
          return (T) new SSHClient();

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
