import com.google.api.client.util.DateTime
import com.google.api.services.drive.Drive
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

class DBManager {
    val DBPath = "${System.getProperty("user.home")}/fileindex.sqlite"
    val DBFile = File(DBPath)
    val TMP_DB_PATH = "tmpdb.sqlite"
    val TMP_DB_FILE = File(TMP_DB_PATH)

    private val con: Connection

    init {
        Class.forName("org.sqlite.JDBC")
        con = DriverManager.getConnection("jdbc:sqlite:$DBPath")
    }

    fun find(query: String): List<FileRow> {
        val patternSearch = con.prepareStatement("SELECT * FROM files WHERE filename LIKE ?;")
        patternSearch.setString(1, "%$query%")
        patternSearch.execute()
        val resultList = ArrayList<FileRow>()
        val result = patternSearch.resultSet
        if (!result.isClosed) {
            val fileNameIndex = result.findColumn("filename")
            val idIndex = result.findColumn("id")
            val tagIndex = result.findColumn("tag")
            val majorDriveIndex = result.findColumn("major_drive")
            val minorDriveIndex = result.findColumn("minor_drive")
            val pathIndex = result.findColumn("path")
            while (result.next()) {
                val id = result.getInt(idIndex)
                val filename = result.getString(fileNameIndex)
                val tag = result.getString(tagIndex)
                val major_drive = result.getString(majorDriveIndex)
                val minor_drive = result.getString(minorDriveIndex)
                val path = result.getString(pathIndex)
                resultList.add(FileRow(id, filename, tag, major_drive, minor_drive, path))
            }
            result.close()
        }
        patternSearch.close()
        return resultList
    }

    fun syncDB(drive: Drive): SyncState {
        initDBIfNot()
        val merged: Boolean
        val shouldUpload: Boolean
        val lastModified: DateTime? = DriveHelper.downloadDB(drive, TMP_DB_FILE)
        val lastModifiedLocal =
            LocalDateTime.ofInstant(Instant.ofEpochMilli(DBFile.lastModified()), TimeZone.getDefault().toZoneId())
        if (lastModified != null) {
            merged = merge(DBFile, DBFile, TMP_DB_FILE)
            shouldUpload = merged
        } else {
            merged = false
            shouldUpload = true
        }
        // DBFile is successfully created / merged
        if (shouldUpload) {
            DriveHelper.uploadDB(drive, DBFile)
        }
        return SyncState.Success(merged, shouldUpload, lastModified, lastModifiedLocal, LocalDateTime.now())
    }

    private fun initDBIfNot() {
        if (!con.isClosed) {
            con.createStatement().execute(
                """create table if not exists files
                (
                    id integer not null
                        constraint files_pk
                            primary key autoincrement,
                    filename string not null,
                    tag string,
                    major_drive string not null,
                    minor_drive string,
                    path string,
                    constraint files_pk_2
                        unique (filename, major_drive, minor_drive, path)
                );
                
                create index if not exists files_filename_index
                    on files (filename);
                
                create unique index if not exists files_id_uindex
                    on files (id);
            """.trimIndent()
            )
        }

    }


    private fun merge(to: File, file1: File, file2: File): Boolean {
        println("Merging $file1 and $file2 to $to")
        val con1 = DriverManager.getConnection("jdbc:sqlite:${file1.canonicalPath}")
        val con2 = DriverManager.getConnection("jdbc:sqlite:${file2.canonicalPath}")
        return false
    }

    fun beginIndex(): PreparedStatement {
        return con.prepareStatement("INSERT INTO files(filename, tag, major_drive, minor_drive, path) VALUES (?, ?, ?, ?, ?) ON CONFLICT  DO NOTHING")
    }

    fun finishIndex(addIndex: PreparedStatement) {
        addIndex.clearParameters()
        addIndex.executeBatch()
        addIndex.close()
    }

    fun close() {
        con.close()
    }
}