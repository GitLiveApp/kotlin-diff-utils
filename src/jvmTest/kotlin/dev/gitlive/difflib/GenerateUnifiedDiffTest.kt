package dev.gitlive.difflib

import dev.gitlive.difflib.algorithm.DiffException
import dev.gitlive.difflib.patch.Patch
import dev.gitlive.difflib.patch.PatchFailedException
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException
import java.util.ArrayList
import java.util.Arrays
import java.util.stream.Collectors.joining
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class GenerateUnifiedDiffTest {

    @Test
    @Throws(DiffException::class, IOException::class)
    fun testGenerateUnified() {
        val origLines = fileToLines(TestConstants.MOCK_FOLDER + "original.txt")
        val revLines = fileToLines(TestConstants.MOCK_FOLDER + "revised.txt")

        verify(origLines, revLines, "original.txt", "revised.txt")
    }

    @Test
    @Throws(DiffException::class, IOException::class)
    fun testGenerateUnifiedWithOneDelta() {
        val origLines = fileToLines(TestConstants.MOCK_FOLDER + "one_delta_test_original.txt")
        val revLines = fileToLines(TestConstants.MOCK_FOLDER + "one_delta_test_revised.txt")

        verify(origLines, revLines, "one_delta_test_original.txt", "one_delta_test_revised.txt")
    }

    @Test
    @Throws(DiffException::class)
    fun testGenerateUnifiedDiffWithoutAnyDeltas() {
        val test = Arrays.asList("abc")
        val patch = DiffUtils.diff(test, test)
        UnifiedDiffUtils.generateUnifiedDiff("abc", "abc", test, patch, 0)
    }

    @Test
    @Throws(IOException::class)
    fun testDiff_Issue10() {
        val baseLines = fileToLines(TestConstants.MOCK_FOLDER + "issue10_base.txt")
        val patchLines = fileToLines(TestConstants.MOCK_FOLDER + "issue10_patch.txt")
        val p = UnifiedDiffUtils.parseUnifiedDiff(patchLines)
        try {
            DiffUtils.patch(baseLines, p)
        } catch (e: PatchFailedException) {
            fail(e.message)
        }

    }

    /**
     * Issue 12
     */
    @Test
    @Throws(DiffException::class, IOException::class)
    fun testPatchWithNoDeltas() {
        val lines1 = fileToLines(TestConstants.MOCK_FOLDER + "issue11_1.txt")
        val lines2 = fileToLines(TestConstants.MOCK_FOLDER + "issue11_2.txt")
        verify(lines1, lines2, "issue11_1.txt", "issue11_2.txt")
    }

    @Test
    @Throws(DiffException::class, IOException::class)
    fun testDiff5() {
        val lines1 = fileToLines(TestConstants.MOCK_FOLDER + "5A.txt")
        val lines2 = fileToLines(TestConstants.MOCK_FOLDER + "5B.txt")
        verify(lines1, lines2, "5A.txt", "5B.txt")
    }

    /**
     * Issue 19
     */
    @Test
    @Throws(DiffException::class)
    fun testDiffWithHeaderLineInText() {
        val original = ArrayList<String>()
        val revised = ArrayList<String>()

        original.add("test line1")
        original.add("test line2")
        original.add("test line 4")
        original.add("test line 5")

        revised.add("test line1")
        revised.add("test line2")
        revised.add("@@ -2,6 +2,7 @@")
        revised.add("test line 4")
        revised.add("test line 5")

        val patch = DiffUtils.diff(original, revised)
        val udiff = UnifiedDiffUtils.generateUnifiedDiff("original", "revised",
                original, patch, 10)
        UnifiedDiffUtils.parseUnifiedDiff(udiff)
    }

    @Throws(DiffException::class)
    private fun verify(origLines: List<String>, revLines: List<String>,
                       originalFile: String, revisedFile: String) {
        val patch = DiffUtils.diff(origLines, revLines)
        val unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(originalFile, revisedFile,
                origLines, patch, 10)

        println(unifiedDiff.stream().collect(joining("\n")))

        val fromUnifiedPatch = UnifiedDiffUtils.parseUnifiedDiff(unifiedDiff)
        val patchedLines: List<String>
        try {
            patchedLines = fromUnifiedPatch.applyTo(origLines)
            assertEquals(revLines.size.toLong(), patchedLines.size.toLong())
            for (i in revLines.indices) {
                val l1 = revLines[i]
                val l2 = patchedLines[i]
                if (l1 != l2) {
                    fail("Line " + (i + 1) + " of the patched file did not match the revised original")
                }
            }
        } catch (e: PatchFailedException) {
            fail(e.message)
        }

    }

    /**
     * https://github.com/GitLiveApp/kotlin-diff-utils/issues/7
     */
    @Test
    @Throws(DiffException::class, IOException::class)
    fun testIssue7() {
        val patch = """--- /monitor/src/main/java/fairx/backup/ArchiveReplicator.java
+++ /monitor/src/main/java/fairx/backup/ArchiveReplicator.java
@@ -14,0 +14,1 @@
+import java.util.concurrent.TimeUnit;
@@ -33,1 +34,0 @@
-import static fairx.cluster.ServiceConfigUtil.clusterBackupContext;
@@ -56,1 +56,1 @@
-    @Inject ArchiveReplicator(ServiceRegistry serviceRegistry, ApplicationDriver appDriver, Aeron aeron,
+    @Inject ArchiveReplicator(ServiceRegistry serviceRegistry, ApplicationDriver appDriver,
@@ -60,0 +60,1 @@
+<<<<<<< Updated upstream
@@ -61,0 +62,4 @@
+=======
+            @Named(CLUSTER_BACKUP) AeronArchive clusterBackupArchive, ReplicatorArchivePruner replicatorPruner,
+            ClusterBackup.Context clusterBackupCtx) {
+>>>>>>> Stashed changes
@@ -67,0 +72,1 @@
+<<<<<<< Updated upstream
@@ -72,0 +78,2 @@
+=======
+>>>>>>> Stashed changes
@@ -73,0 +81,1 @@
+        this.clusterBackupCtx = clusterBackupCtx;
@@ -86,0 +95,2 @@
+        log.info("backupIntervalMs: {}", TimeUnit.NANOSECONDS.toMillis(clusterBackupCtx.clusterBackupIntervalNs()));
+        log.info("deleteClusterDirOnStart: {}", clusterBackupCtx.deleteDirOnStart());"""


        val content = """package fairx.backup;

import io.aeron.Aeron;
import io.aeron.Subscription;
import io.aeron.archive.Archive;
import io.aeron.archive.client.AeronArchive;
import io.aeron.cluster.ClusterBackup;
import lombok.CustomLog;
import org.agrona.CloseHelper;
import org.agrona.concurrent.ShutdownSignalBarrier;

import javax.inject.Inject;
import javax.inject.Named;

import fairx.appdriver.ApplicationDriver;
import fairx.appdriver.ThrottledTask;
import fairx.archive.ArchivePruner;
import fairx.cluster.ClusterSnapshotPruner;
import fairx.service.ErrorLogger;
import fairx.service.ServiceConfig;
import fairx.service.ServiceRegistry;
import fairx.service.ServiceStatusListener;
import fairx.version.VersionLogger;

import static io.aeron.archive.Archive.Configuration.ARCHIVE_CONTROL_SESSIONS_TYPE_ID;
import static io.aeron.archive.Archive.Configuration.ARCHIVE_ERROR_COUNT_TYPE_ID;
import static java.util.concurrent.TimeUnit.MINUTES;

import static fairx.cluster.ServiceConfigUtil.aeronCtx;
import static fairx.cluster.ServiceConfigUtil.aeronDirectoryName;
import static fairx.cluster.ServiceConfigUtil.archiveCtx;
import static fairx.cluster.ServiceConfigUtil.archiveDirectoryName;
import static fairx.cluster.ServiceConfigUtil.clusterBackupContext;
import static fairx.cluster.ServiceConfigUtil.remoteArchiveClientCtx;
import static fairx.service.RecordedChannelId.ACCOUNT_AUX_EVENTS;
import static fairx.service.ServiceRegistry.CLUSTER_BACKUP;
import static fairx.service.ServiceRegistry.REPLICATOR;
import static fairx.service.ServiceRegistry.SERVICE_STATUS_REQUEST;
import static fairx.service.ServiceRegistry.TRADING_SYSTEM;
import static fairx.service.StreamId.CLUSTER_BACKUP_ARCHIVE_LOCAL_CONTROL_REQUEST;
import static fairx.service.StreamId.CLUSTER_REPLICATION_ARCHIVE_RESPONSE;
import static fairx.service.StreamId.OTHER_REPLICATION_ARCHIVE_RESPONSE;
import static fairx.service.StreamId.REPLICATOR_ARCHIVE_LOCAL_CONTROL_REQUEST;

@CustomLog
public class ArchiveReplicator implements ApplicationDriver.Application {
    private final ApplicationDriver appDriver;
    private final ServiceStatusListener serviceStatusListener;
    private final GatewayArchiveReplicator gatewayReplicator;
    private final OneToOneArchiveReplicator.Factory replicatorFactory;
    private final ServiceConfig clusterBackupConfig, replicatorConfig;
    private final ArchivePruner replicatorPruner, clusterBackupPruner;
    private final ClusterBackup.Context clusterBackupCtx;
    private final ClusterSnapshotPruner clusterSnapshotPruner;

    @Inject ArchiveReplicator(ServiceRegistry serviceRegistry, ApplicationDriver appDriver, Aeron aeron,
            @Named(SERVICE_STATUS_REQUEST) Subscription serviceStatusSub, ServiceStatusListener serviceStatusListener,
            BackupEventsListener eventsListener, GatewayArchiveReplicator gatewayReplicator,
            OneToOneArchiveReplicator.Factory replicatorFactory, @Named(REPLICATOR) AeronArchive replicatorArchive,
            @Named(CLUSTER_BACKUP) AeronArchive clusterBackupArchive) {
        this.appDriver = appDriver;
        this.serviceStatusListener = serviceStatusListener;
        this.gatewayReplicator = gatewayReplicator;
        this.replicatorFactory = replicatorFactory;
        clusterBackupConfig = serviceRegistry.service(CLUSTER_BACKUP);
        replicatorConfig = serviceRegistry.service(REPLICATOR);
        replicatorPruner = new ArchivePruner(replicatorArchive, replicatorConfig.getInt("max-segments-per-recording", 100));
        clusterBackupPruner = new ArchivePruner(clusterBackupArchive, clusterBackupConfig.getInt("max-segments-per-recording", 10));
        clusterBackupCtx = clusterBackupContext(clusterBackupConfig, serviceRegistry.service(TRADING_SYSTEM))
                .aeronDirectoryName(aeron.context().aeronDirectoryName()).errorHandler(ErrorLogger::log)
                .useAgentInvoker(true).eventsListener(eventsListener);
        clusterSnapshotPruner = ClusterSnapshotPruner.create(clusterBackupArchive, clusterBackupConfig);
        appDriver.addCloseable(clusterBackupArchive);
        appDriver.addCloseable(replicatorArchive);
        appDriver.addCloseable(eventsListener);
        appDriver.register(serviceStatusSub, serviceStatusListener);
        appDriver.register(gatewayReplicator);
    }

    @Override public String name() { return REPLICATOR; }

    @Override public void start() {
        log.info("consensusEndpoints: {}", clusterBackupCtx.clusterConsensusEndpoints());
        log.info("consensusChannel: {}", clusterBackupCtx.consensusChannel());
        log.info("catchupEndpoint: {}", clusterBackupCtx.catchupEndpoint());
        appDriver.register(ClusterBackup.launch(clusterBackupCtx).conductorAgentInvoker());
        appDriver.register(replicatorFactory.create(ACCOUNT_AUX_EVENTS));
        appDriver.register(new ThrottledTask(1, replicatorConfig.getInt("prune-interval-minutes", 60), MINUTES, this::pruneArchives));
        appDriver.register(new ThrottledTask(2, clusterBackupConfig.getInt("prune-interval-minutes", 60), MINUTES,
                nanoTime -> { clusterSnapshotPruner.prune(); return 0; }));
        gatewayReplicator.start();
        serviceStatusListener.startupComplete();
    }

    private int pruneArchives(long nanoTime) {
        replicatorPruner.prune();
        clusterBackupPruner.prune();
        return 0;
    }

    @Override public void close() { CloseHelper.close(serviceStatusListener); }

    public static void main(String[] args) {
        VersionLogger.logVersionInfo();
        ApplicationDriver.Context appCtx = new ApplicationDriver.Context(REPLICATOR);
        ServiceConfig replicatorConfig = appCtx.serviceRegistry().service(REPLICATOR);
        ServiceConfig clusterBackupConfig = appCtx.serviceRegistry().service(CLUSTER_BACKUP);
        ServiceConfig clusterConfig = appCtx.serviceRegistry().service(TRADING_SYSTEM);
        appCtx.aeronDirectoryName(aeronDirectoryName(replicatorConfig));
        Archive.Context clusterBackupArchiveCtx = archiveCtx(clusterBackupConfig).errorHandler(t -> ErrorLogger.log(t, "ClusterBackupArchive"))
                .archiveDirectoryName(archiveDirectoryName("cluster-backup")).localControlStreamId(CLUSTER_BACKUP_ARCHIVE_LOCAL_CONTROL_REQUEST.id)
                .archiveClientContext(remoteArchiveClientCtx(clusterConfig, "cluster-backup", CLUSTER_REPLICATION_ARCHIVE_RESPONSE));
        Archive.Context replicatorArchiveCtx = archiveCtx(replicatorConfig).errorHandler(t -> ErrorLogger.log(t, "ClusterBackupArchive"))
                .archiveDirectoryName(archiveDirectoryName("replicator")).localControlStreamId(REPLICATOR_ARCHIVE_LOCAL_CONTROL_REQUEST.id)
                .archiveClientContext(remoteArchiveClientCtx(clusterConfig, "replicate", OTHER_REPLICATION_ARCHIVE_RESPONSE));
        try (Aeron aeron = Aeron.connect(aeronCtx(replicatorConfig).errorHandler(t -> ErrorLogger.log(t, "ArchiveAeronClient")));
             Archive clusterBackupArchive = Archive.launch(clusterBackupArchiveCtx.aeron(aeron)
                     .errorCounter(aeron.addCounter(ARCHIVE_ERROR_COUNT_TYPE_ID, "ClusterBackup Archive Errors"))
                     .controlSessionsCounter(aeron.addCounter(ARCHIVE_CONTROL_SESSIONS_TYPE_ID, "ClusterBackup Archive Control Sessions")));
             Archive replicatorArchive = Archive.launch(replicatorArchiveCtx.aeron(aeron)
                     .errorCounter(aeron.addCounter(ARCHIVE_ERROR_COUNT_TYPE_ID, "Replicator Archive Errors"))
                     .controlSessionsCounter(aeron.addCounter(ARCHIVE_CONTROL_SESSIONS_TYPE_ID, "Replicator Archive Control Sessions")))) {
            log.info("clusterBackupArchiveDir={}", clusterBackupArchive.context().archiveDirectoryName());
            log.info("replicatorArchiveDir={}", replicatorArchive.context().archiveDirectoryName());
            try (ApplicationDriver ignored = ApplicationDriver.launch(appCtx, new ArchiveReplicatorModule())) {
                new ShutdownSignalBarrier().await();
                log.info("shutting down replicator");
            }
        }
    }
}

"""
        println(UnifiedDiffUtils.parseUnifiedDiff(patch.lines())
                .applyTo(content.lines())
                .joinToString("\n"))
    }

    companion object {

        @Throws(FileNotFoundException::class, IOException::class)
        fun fileToLines(filename: String): List<String> {
            val lines = ArrayList<String>()
            var line = ""
            BufferedReader(FileReader(filename)).use { `in` ->
                while (`in`.readLine().also { line = it } != null) {
                    lines.add(line)
                }
            }
            return lines
        }
    }
}
