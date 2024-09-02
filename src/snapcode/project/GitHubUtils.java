package snapcode.project;
import snap.web.WebFile;
import snap.web.WebURL;

/**
 * Utilities for GitHub API.
 */
public class GitHubUtils {

    /**
     * Downloads a github zip file.
     */
    public static WebURL downloadGithubZipFile(WebURL repoURL)
    {
        // Get Zip bytes
        String PROXY_URL = "https://corsproxy.io/?";
        String repoUrlAddr = repoURL.getString().replace(".git", "");
        String zipUrlAddr = PROXY_URL + repoUrlAddr.replace("github.com", "codeload.github.com") + "/zip/refs/heads/master";
        zipUrlAddr += "?vers=" + (System.currentTimeMillis() % 3600000); // Defeat caching
        WebURL zipUrl = WebURL.getURL(zipUrlAddr); assert (zipUrl != null);
        byte[] zipBytes = zipUrl.getBytes();
        if (zipBytes == null)
            throw new RuntimeException("Can't download git zip file");

        // Get zip
        String zipName = repoURL.getFilename().replace(".git", ".zip");
        String zipPath = "/files/ZipFiles/" + zipName;
        WebURL zipUrl2 = WebURL.getURL(zipPath); assert (zipUrl2 != null);
        WebFile zipFile = zipUrl2.createFile(false);
        zipFile.setBytes(zipBytes);
        zipFile.save();
        return zipUrl2;
    }

    /**
     * Returns a zip file for given repo URL.
     */
    public static WebFile getZipFileForRepoURL(WebURL repoURL)
    {
        // Get repo URL
        String repoUrlAddr = repoURL.getString().replace(".git", "");
        if (repoUrlAddr.startsWith("git:"))
            repoUrlAddr = repoUrlAddr.replace("git:", "https:");

        // Get Zip bytes
        String apiUrlAddr = repoUrlAddr.replace("github.com/", "api.github.com/repos/");
        String zipUrlAddr = apiUrlAddr + "/zipball";
        System.out.println("Download: " + zipUrlAddr);
        WebURL zipUrl = WebURL.getURL(zipUrlAddr); assert (zipUrl != null);
        byte[] zipBytes = zipUrl.getBytes();
        if (zipBytes == null)
            throw new RuntimeException("Can't download git zip file");

        // Create zip file
        String repoName = repoURL.getFilenameSimple();
        String zipName = repoName + ".zip";
        WebFile zipFile = WebFile.createTempFileForName(zipName, false);
        zipFile.setBytes(zipBytes);
        zipFile.save();

        // Return
        return zipFile;
    }
}