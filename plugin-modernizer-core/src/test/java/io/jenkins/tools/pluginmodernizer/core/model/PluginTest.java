package io.jenkins.tools.pluginmodernizer.core.model;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.github.GHService;
import io.jenkins.tools.pluginmodernizer.core.impl.MavenInvoker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PluginTest {

    @Mock
    private MavenInvoker mavenInvoker;

    @Mock
    private GHService ghService;

    @Mock
    private Config config;

    @Test
    public void testPluginName() {
        Plugin plugin = Plugin.build("example");
        assertEquals("example", plugin.getName());
        plugin.withName("new-name");
        assertEquals("new-name", plugin.getName());
    }

    @Test
    public void testRepositoryName() {
        Plugin plugin = Plugin.build("example");
        assertNull(plugin.getRepositoryName());
        plugin.withRepositoryName("new-repo");
        assertEquals("new-repo", plugin.getRepositoryName());
    }

    @Test
    public void testLocalRepository() {
        Plugin plugin = Plugin.build("example");
        assertEquals(
                Settings.getPluginsDirectory(plugin).resolve("sources").toString(),
                plugin.getLocalRepository().toString());
    }

    @Test
    public void testGetGitHubRepository() {
        Plugin plugin = Plugin.build("example");
        plugin.withRepositoryName("repo-name");
        assertEquals(
                "https://github.com/foobar/repo-name.git",
                plugin.getGitRepositoryURI("foobar").toString());
    }

    @Test
    public void testHasCommits() {
        Plugin plugin = Plugin.build("example");
        assertFalse(plugin.hasCommits());
        plugin.withCommits();
        assertTrue(plugin.hasCommits());
        plugin.withoutCommits();
        assertFalse(plugin.hasCommits());
    }

    @Test
    public void testClean() {
        Plugin plugin = Plugin.build("example");
        plugin.clean(mavenInvoker);
        verify(mavenInvoker).invokeGoal(plugin, "clean");
        verifyNoMoreInteractions(mavenInvoker);
    }

    @Test
    public void testCompile() {
        Plugin plugin = Plugin.build("example");
        plugin.withConfig(config);
        doReturn(false).when(config).isFetchMetadataOnly();
        plugin.withJDK(JDK.JAVA_21);
        plugin.compile(mavenInvoker);
        verify(mavenInvoker).invokeGoal(plugin, "compile");
        verifyNoMoreInteractions(mavenInvoker);
    }

    @Test
    public void shouldSkipCompileInFetchMetadata() {
        Plugin plugin = Plugin.build("example");
        plugin.withConfig(config);
        doReturn(true).when(config).isFetchMetadataOnly();
        plugin.withJDK(JDK.JAVA_21);
        plugin.compile(mavenInvoker);
        verify(mavenInvoker, times(0)).invokeGoal(plugin, "compile");
        verifyNoMoreInteractions(mavenInvoker);
    }

    @Test
    public void testVerify() {
        Plugin plugin = Plugin.build("example");
        plugin.withConfig(config);
        doReturn(false).when(config).isFetchMetadataOnly();
        plugin.withJDK(JDK.JAVA_21);
        plugin.verify(mavenInvoker);
        verify(mavenInvoker).invokeGoal(plugin, "verify");
        verifyNoMoreInteractions(mavenInvoker);
    }

    @Test
    public void shouldNotVerifyInFetchMetadataMode() {
        Plugin plugin = Plugin.build("example");
        plugin.withConfig(config);
        doReturn(true).when(config).isFetchMetadataOnly();
        plugin.withJDK(JDK.JAVA_21);
        plugin.verify(mavenInvoker);
        verify(mavenInvoker, times(0)).invokeGoal(plugin, "verify");
        verifyNoMoreInteractions(mavenInvoker);
    }

    @Test
    public void testRewrite() {
        Plugin plugin = Plugin.build("example");
        plugin.withConfig(config);
        doReturn(false).when(config).isFetchMetadataOnly();
        plugin.runOpenRewrite(mavenInvoker);
        verify(mavenInvoker).invokeRewrite(plugin);
        verifyNoMoreInteractions(mavenInvoker);
    }

    @Test
    public void shouldSkipRewriteInFetchMetadataMode() {
        Plugin plugin = Plugin.build("example");
        plugin.withConfig(config);
        doReturn(true).when(config).isFetchMetadataOnly();
        plugin.runOpenRewrite(mavenInvoker);
        verify(mavenInvoker, times(0)).invokeRewrite(plugin);
        verifyNoMoreInteractions(mavenInvoker);
    }

    @Test
    public void testFork() {
        Plugin plugin = Plugin.build("example");
        plugin.withConfig(config);
        doReturn(false).when(config).isFetchMetadataOnly();
        plugin.fork(ghService);
        verify(ghService).fork(plugin);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void shouldNotForkInFetchMetadataMode() {
        Plugin plugin = Plugin.build("example");
        plugin.withConfig(config);
        doReturn(true).when(config).isFetchMetadataOnly();
        plugin.fork(ghService);
        verify(ghService, times(0)).fork(plugin);
    }

    @Test
    public void testSync() {
        Plugin plugin = Plugin.build("example");
        plugin.withConfig(config);
        doReturn(false).when(config).isFetchMetadataOnly();
        plugin.sync(ghService);
        verify(ghService).sync(plugin);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void shouldNotSyncInFetchMetadataMode() {
        Plugin plugin = Plugin.build("example");
        plugin.withConfig(config);
        doReturn(true).when(config).isFetchMetadataOnly();
        plugin.sync(ghService);
        verify(ghService, times(0)).sync(plugin);
    }

    @Test
    public void testIsFork() {
        Plugin plugin = Plugin.build("example");
        plugin.isForked(ghService);
        verify(ghService).isForked(plugin);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void testDeleteFork() {
        Plugin plugin = Plugin.build("example");
        plugin.deleteFork(ghService);
        verify(ghService).deleteFork(plugin);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void testIsArchived() {
        Plugin plugin = Plugin.build("example");
        plugin.isArchived(ghService);
        verify(ghService).isArchived(plugin);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void testCheckoutBranch() {
        Plugin plugin = Plugin.build("example");
        plugin.checkoutBranch(ghService);
        verify(ghService).checkoutBranch(plugin);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void testCommit() {
        Plugin plugin = Plugin.build("example");
        plugin.commit(ghService);
        verify(ghService).commitChanges(plugin);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void testPush() {
        Plugin plugin = Plugin.build("example");
        plugin.push(ghService);
        verify(ghService).pushChanges(plugin);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void testOpenPullRequest() {
        Plugin plugin = Plugin.build("example");
        plugin.openPullRequest(ghService);
        verify(ghService).openPullRequest(plugin);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void testFetch() {
        Plugin plugin = Plugin.build("example");
        plugin.fetch(ghService);
        verify(ghService).fetch(plugin);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void testGetRemoteRepository() {
        Plugin plugin = Plugin.build("example");
        plugin.withRepositoryName("repo-name");
        plugin.getRemoteRepository(ghService);
        verify(ghService).getRepository(plugin);
    }

    @Test
    public void testGetRemoteForkRepository() {
        Plugin plugin = Plugin.build("example");
        plugin.withRepositoryName("repo-name");
        plugin.getRemoteForkRepository(ghService);
        verify(ghService).getRepositoryFork(plugin);
    }

    @Test
    public void testHasErrors() {
        Plugin plugin = Plugin.build("example").withConfig(mock(Config.class));
        assertFalse(plugin.hasErrors());
        plugin.addError("error", new Exception("error"));
        assertTrue(plugin.hasErrors());
    }

    @Test
    public void testToString() {
        Plugin plugin = Plugin.build("example");
        assertEquals("example", plugin.toString());
    }

    @Test
    public void testGetMarker() {
        Plugin plugin = Plugin.build("example");
        Marker expectedMarker = MarkerFactory.getMarker("example");
        Marker actualMarker = plugin.getMarker();
        assertEquals(expectedMarker, actualMarker);
    }
    @Captor
    private ArgumentCaptor<Plugin> pluginCaptor;

    @Test
    public void testAddRelativePathIfMissing() {
        Plugin plugin = Plugin.build("example");
        plugin.withConfig(config);
        doReturn(false).when(config).isFetchMetadataOnly();
        plugin.withJDK(JDK.JAVA_21);
        plugin.compile(mavenInvoker);

        verify(mavenInvoker).invokeGoal(pluginCaptor.capture(), eq("compile"));
        verify(mavenInvoker).addRelativePathIfMissing(pluginCaptor.getValue());
        verifyNoMoreInteractions(mavenInvoker);
    }
}
