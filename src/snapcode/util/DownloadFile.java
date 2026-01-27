package snapcode.util;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe "download on demand" holder for a remote URL and a local file path.
 *
 * Guarantees:
 * - At most one download attempt runs at a time for a given instance.
 * - Concurrent callers will wait for the same download to complete.
 * - Download writes to a .part file, then atomically moves into place.
 */
public final class DownloadFile {

    // The remote URL
    private final URL _remoteUrl;

    // The local file path
    private final Path _localPath;

    // Points at the in-flight or completed download (shared by all threads).
    private final AtomicReference<CompletableFuture<Path>> _downloadFutureRef = new AtomicReference<>();

    /**
     * Constructor.
     */
    public DownloadFile(URL remoteUrl, Path localPath)
    {
        _remoteUrl = Objects.requireNonNull(remoteUrl, "remoteUrl");
        _localPath = Objects.requireNonNull(localPath, "localPath");
    }

    /**
     * Returns the remote URL.
     */
    public URL getRemoteUrl() { return _remoteUrl; }

    /**
     * Returns the local file path, downloading it if needed.
     */
    public Path getLocalPath() throws IOException
    {
        // Fast path: already on disk
        if (Files.exists(_localPath))
            return _localPath;

        // Get loader - just wait and return if set
        CompletableFuture<Path> existing = _downloadFutureRef.get();
        if (existing != null)
            return awaitForCompletableFuture(existing);

        // Create a new future and try to "win" installing it. Just wait and return if already set
        CompletableFuture<Path> created = new CompletableFuture<>();
        if (!_downloadFutureRef.compareAndSet(null, created))
            return awaitForCompletableFuture(_downloadFutureRef.get());

        // We won: perform the download and complete the future.
        try {
            Path result = downloadUrlToLocalPath(_remoteUrl, _localPath);
            created.complete(result);
            return result;
        }

        // Allow retry on next call
        catch (Throwable t) {
            created.completeExceptionally(t);
            _downloadFutureRef.compareAndSet(created, null);
            if (t instanceof IOException ioe) throw ioe;
            if (t instanceof RuntimeException re) throw re;
            throw new IOException("Download failed", t);
        }
    }

    /**
     * Deletes the local file (if present) and invalidates cached future.
     */
    public void deleteLocalFile() throws IOException
    {
        _downloadFutureRef.set(null);
        Files.deleteIfExists(_localPath);
    }

    /**
     * Downloads remote url to local.
     */
    private static Path downloadUrlToLocalPath(URL remoteUrl, Path localPath) throws IOException
    {
        // Double-check after we "won" the CAS: maybe file appeared (e.g., created externally)
        if (Files.exists(localPath))
            return localPath;

        // Make sure parent directory exists
        Path parent = localPath.getParent();
        if (parent != null)
            Files.createDirectories(parent);

        // Download to sibling .download path
        Path downloadPath = localPath.resolveSibling(localPath.getFileName() + ".download");

        // Clean up any stale download file
        Files.deleteIfExists(downloadPath);

        // Actual download (basic URL stream). Replace with HttpClient if you want timeouts/headers.
        try (InputStream in = new BufferedInputStream(remoteUrl.openStream())) {
            Files.copy(in, downloadPath, StandardCopyOption.REPLACE_EXISTING);
        }

        // Best effort cleanup
        catch (IOException e) {
            try { Files.deleteIfExists(downloadPath); }
            catch (IOException ignore) { }
            throw e;
        }

        // Atomic move into place (best-effort; may fall back depending on FS)
        try {
            Files.move(downloadPath, localPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }

        // Handle no atomic support: Do normal move
        catch (AtomicMoveNotSupportedException e) {
            Files.move(downloadPath, localPath, StandardCopyOption.REPLACE_EXISTING);
        }

        // Return
        return localPath;
    }

    /**
     * Waits for given future.
     */
    private static <T> T awaitForCompletableFuture(CompletableFuture<T> completableFuture) throws IOException
    {
        // Return future value
        try { return completableFuture.get(); }

        // Handle thread interrupted
        catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for download", ie);
        }

        // Handle thread had exception
        catch (ExecutionException ee) {
            Throwable c = ee.getCause();
            if (c instanceof IOException ioe) throw ioe;
            if (c instanceof RuntimeException re) throw re;
            throw new IOException("Download failed", c);
        }
    }
}