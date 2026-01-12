package com.termex.app.ui.viewmodel;

import com.termex.app.core.ssh.SSHClient;
import com.termex.app.domain.ServerRepository;
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
public final class TerminalViewModel_Factory implements Factory<TerminalViewModel> {
  private final Provider<SSHClient> sshClientProvider;

  private final Provider<ServerRepository> serverRepositoryProvider;

  public TerminalViewModel_Factory(Provider<SSHClient> sshClientProvider,
      Provider<ServerRepository> serverRepositoryProvider) {
    this.sshClientProvider = sshClientProvider;
    this.serverRepositoryProvider = serverRepositoryProvider;
  }

  @Override
  public TerminalViewModel get() {
    return newInstance(sshClientProvider.get(), serverRepositoryProvider.get());
  }

  public static TerminalViewModel_Factory create(Provider<SSHClient> sshClientProvider,
      Provider<ServerRepository> serverRepositoryProvider) {
    return new TerminalViewModel_Factory(sshClientProvider, serverRepositoryProvider);
  }

  public static TerminalViewModel newInstance(SSHClient sshClient,
      ServerRepository serverRepository) {
    return new TerminalViewModel(sshClient, serverRepository);
  }
}
