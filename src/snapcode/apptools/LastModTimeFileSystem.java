package snapcode.apptools;
import snap.util.ArrayUtils;
import snap.util.ListUtils;
import snap.util.StringUtils;
import snap.web.WebFile;
import java.util.*;

/**
 * This class is a ProjectFileSystem that shows files separated by LastModifiedTime.
 */
public class LastModTimeFileSystem extends ProjectFileSystem {

    // Map of date buckets to files
    private Map<DateBucket, List<WebFile>> _bucketFiles = new HashMap<>();

    // Shared instance
    private static LastModTimeFileSystem _shared;

    // Constants for date buckets
    public enum DateBucket { Today, Yesterday, PreviousWeek, PreviousMonth, PreviousYear, PreviousEon, OtherFiles}

    // List of source file types
    private static final List<String> SOURCE_FILE_TYPES = List.of("java", "jepl", "jmd", "snp");

    /**
     * Constructor.
     */
    public LastModTimeFileSystem()
    {
        super();
    }

    /**
     * Override to search all root file bucket files for given web file.
     */
    @Override
    public ProjectFile getProjectFileForFile(WebFile aFile)
    {
        if (aFile == null) return null;
        if (aFile.isRoot())
            return getProjectFileForRootFile(aFile);

        // Get root file bucket files
        ProjectFile rootFile = getProjectFileForRootFile(aFile.getSite().getRootDir());
        List<ProjectFile> bucketFiles = rootFile.getFiles();

        // Search all bucket directories for project file with matching web file
        for (ProjectFile bucketFile : bucketFiles) {
            ProjectFile projectFile = ListUtils.findMatch(bucketFile.getFiles(), file -> file.getFile() == aFile);
            if (projectFile != null)
                return projectFile;
        }

        // Return not found
        return null;
    }

    /**
     * Returns the list of child files for given file.
     */
    @Override
    public synchronized List<ProjectFile> getChildFilesForFile(ProjectFile projectFile)
    {
        // Get web file - complain if not root
        WebFile webFile = projectFile.getFile();
        if (!webFile.isRoot())
            throw new RuntimeException("getChildFilesForFile called for unexpected file: " + webFile);

        // Reload bucket files, map buckets to bucket files and return
        _bucketFiles.clear();
        addWebFileToBucket(webFile);
        return ArrayUtils.mapNonNullToList(DateBucket.values(), db -> createProjectFileForDateBucket(projectFile, db));
    }

    /**
     * Creates a project file for given date bucket.
     */
    private ProjectFile createProjectFileForDateBucket(ProjectFile parentFile, DateBucket dateBucket)
    {
        // Get files for bucket (if no files, just return null)
        List<WebFile> bucketFiles = getDateBucketList(dateBucket);
        if (bucketFiles.isEmpty())
            return null;

        // Sort files by LastModTime
        bucketFiles.sort(Comparator.comparingLong(WebFile::getLastModTime));

        // Create bucket project file and create/add child files to it
        ProjectFile projectFile = new ProjectFile(this, parentFile, null);
        projectFile._isDir = true;
        projectFile._text = StringUtils.fromCamelCase(dateBucket.name());
        projectFile._childFiles = ListUtils.map(bucketFiles, file -> createProjectFile(projectFile, file));

        // Return
        return projectFile;
    }

    /**
     * Adds a file to a date bucket.
     */
    private void addWebFileToBucket(WebFile webFile)
    {
        // Handle directory
        if (webFile.isDir()) {
            if (!webFile.getName().equals("bin"))
                webFile.getFiles().forEach(this::addWebFileToBucket);
            return;
        }

        // Skip hidden files
        String name = webFile.getName();
        if (name.startsWith("."))
            return;

        // Get DateBucket for file
        DateBucket dateBucket = getDateBucketForDate(webFile.getLastModDate());
        if (!SOURCE_FILE_TYPES.contains(webFile.getFileType()))
            dateBucket = DateBucket.OtherFiles;

        // Get bucket list and add file
        List<WebFile> bucketList = getDateBucketList(dateBucket);
        bucketList.add(webFile);
    }

    /**
     * Returns a DateBucket for given web file.
     */
    private List<WebFile> getDateBucketList(DateBucket dateBucket)
    {
        return _bucketFiles.computeIfAbsent(dateBucket, k -> new ArrayList<>());
    }

    /**
     * Returns a DateBucket for given date.
     */
    public static DateBucket getDateBucketForDate(Date aDate)
    {
        // Get calendar for date
        Calendar input = Calendar.getInstance();
        input.setTime(aDate);

        // Get calendar for now
        Calendar now = Calendar.getInstance();

        // Today
        if (isSameDay(input, now))
            return DateBucket.Today;

        // Yesterday
        Calendar yesterday = (Calendar) now.clone();
        yesterday.add(Calendar.DATE, -1);
        if (isSameDay(input, yesterday))
            return DateBucket.Yesterday;

        // Previous Week
        Calendar lastWeek = (Calendar) now.clone();
        lastWeek.add(Calendar.WEEK_OF_YEAR, -1);
        if (input.after(startOfWeek(lastWeek)) && input.before(startOfWeek(now)))
            return DateBucket.PreviousWeek;

        // Previous Month
        Calendar lastMonth = (Calendar) now.clone();
        lastMonth.add(Calendar.MONTH, -1);
        if (input.get(Calendar.YEAR) == lastMonth.get(Calendar.YEAR) && input.get(Calendar.MONTH) == lastMonth.get(Calendar.MONTH))
            return DateBucket.PreviousMonth;

        // Previous Year
        Calendar lastYear = (Calendar) now.clone();
        lastYear.add(Calendar.YEAR, -1);
        if (input.get(Calendar.YEAR) == lastYear.get(Calendar.YEAR))
            return DateBucket.PreviousYear;

        // Return
        return DateBucket.PreviousEon;
    }

    /**
     * Returns whether given calendars are on the same days.
     */
    private static boolean isSameDay(Calendar c1, Calendar c2)
    {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * Returns whether given calendars are on the same days.
     */
    private static Calendar startOfWeek(Calendar cal)
    {
        Calendar start = (Calendar) cal.clone();
        start.set(Calendar.DAY_OF_WEEK, start.getFirstDayOfWeek());
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        return start;
    }

    /**
     * Returns a shared instance.
     */
    public static LastModTimeFileSystem getShared()
    {
        if (_shared != null) return _shared;
        return _shared = new LastModTimeFileSystem();
    }
}
