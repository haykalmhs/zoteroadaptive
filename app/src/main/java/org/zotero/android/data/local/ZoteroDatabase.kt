package org.zotero.android.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.zotero.android.data.model.Creator
import org.zotero.android.data.model.ItemType
import org.zotero.android.data.model.PaperSection
import org.zotero.android.data.model.PdfAnnotation
import org.zotero.android.data.model.ZoteroCollection
import org.zotero.android.data.model.ZoteroItem
import org.zotero.android.data.model.ZoteroNote
import org.zotero.android.data.model.ZoteroTag

private val jsonCodec = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Entity(tableName = "zotero_items")
data class ZoteroItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itemKey: String,
    val itemTypeKey: String,
    val title: String,
    val creatorsJson: String,
    val year: String,
    val publicationTitle: String,
    val venueShort: String,
    val citationCount: String,
    val publisher: String,
    val volume: String,
    val issue: String,
    val pages: String,
    val doi: String,
    val isbn: String,
    val url: String,
    val abstractNote: String,
    val extra: String,
    val sectionsJson: String,
    val collectionKeysJson: String,
    val tagsJson: String,
    val notesJson: String,
    val annotationsJson: String,
    val hasPdfAttachment: Boolean,
    val isFavorite: Boolean,
    val isInTrash: Boolean,
    val dateAdded: Long
) {
    fun toDomain(): ZoteroItem = ZoteroItem(
        id = id,
        itemKey = itemKey,
        itemType = ItemType.fromKey(itemTypeKey),
        title = title,
        creators = runCatching { jsonCodec.decodeFromString<List<Creator>>(creatorsJson) }.getOrDefault(emptyList()),
        year = year,
        publicationTitle = publicationTitle,
        venueShort = venueShort,
        citationCount = citationCount,
        publisher = publisher,
        volume = volume,
        issue = issue,
        pages = pages,
        doi = doi,
        isbn = isbn,
        url = url,
        abstractNote = abstractNote,
        extra = extra,
        sections = runCatching { jsonCodec.decodeFromString<List<PaperSection>>(sectionsJson) }.getOrDefault(emptyList()),
        collectionKeys = runCatching { jsonCodec.decodeFromString<List<String>>(collectionKeysJson) }.getOrDefault(emptyList()),
        tags = runCatching { jsonCodec.decodeFromString<List<ZoteroTag>>(tagsJson) }.getOrDefault(emptyList()),
        notes = runCatching { jsonCodec.decodeFromString<List<ZoteroNote>>(notesJson) }.getOrDefault(emptyList()),
        annotations = runCatching { jsonCodec.decodeFromString<List<PdfAnnotation>>(annotationsJson) }.getOrDefault(emptyList()),
        hasPdfAttachment = hasPdfAttachment,
        isFavorite = isFavorite,
        isInTrash = isInTrash,
        dateAdded = dateAdded
    )
}

fun ZoteroItem.toEntity(): ZoteroItemEntity = ZoteroItemEntity(
    id = id,
    itemKey = itemKey,
    itemTypeKey = itemType.key,
    title = title,
    creatorsJson = jsonCodec.encodeToString(creators),
    year = year,
    publicationTitle = publicationTitle,
    venueShort = venueShort,
    citationCount = citationCount,
    publisher = publisher,
    volume = volume,
    issue = issue,
    pages = pages,
    doi = doi,
    isbn = isbn,
    url = url,
    abstractNote = abstractNote,
    extra = extra,
    sectionsJson = jsonCodec.encodeToString(sections),
    collectionKeysJson = jsonCodec.encodeToString(collectionKeys),
    tagsJson = jsonCodec.encodeToString(tags),
    notesJson = jsonCodec.encodeToString(notes),
    annotationsJson = jsonCodec.encodeToString(annotations),
    hasPdfAttachment = hasPdfAttachment,
    isFavorite = isFavorite,
    isInTrash = isInTrash,
    dateAdded = dateAdded
)

@Entity(tableName = "zotero_collections")
data class ZoteroCollectionEntity(
    @PrimaryKey val key: String,
    val name: String,
    val parentKey: String?,
    val isSystem: Boolean,
    val description: String
) {
    fun toDomain(): ZoteroCollection = ZoteroCollection(
        key = key,
        name = name,
        parentKey = parentKey,
        isSystem = isSystem,
        description = description
    )
}

fun ZoteroCollection.toEntity(): ZoteroCollectionEntity = ZoteroCollectionEntity(
    key = key,
    name = name,
    parentKey = parentKey,
    isSystem = isSystem,
    description = description
)

@Entity(tableName = "zotero_standalone_notes")
data class ZoteroStandaloneNoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val updatedAt: String
) {
    fun toDomain(): ZoteroNote = ZoteroNote(
        id = id,
        title = title,
        content = content,
        updatedAt = updatedAt,
        parentItemId = null,
        parentItemTitle = null
    )
}

@Dao
interface ZoteroDao {
    @Query("SELECT * FROM zotero_items ORDER BY dateAdded DESC")
    fun observeAllItems(): Flow<List<ZoteroItemEntity>>

    @Query("SELECT COUNT(*) FROM zotero_items")
    suspend fun getItemCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ZoteroItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ZoteroItemEntity>)

    @Update
    suspend fun updateItem(item: ZoteroItemEntity)

    @Query("DELETE FROM zotero_items WHERE id = :id")
    suspend fun deleteItemById(id: Int)

    @Query("DELETE FROM zotero_items WHERE isInTrash = 1")
    suspend fun emptyTrash()

    @Query("SELECT * FROM zotero_collections ORDER BY isSystem DESC, name ASC")
    fun observeAllCollections(): Flow<List<ZoteroCollectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: ZoteroCollectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollections(collections: List<ZoteroCollectionEntity>)

    @Query("DELETE FROM zotero_collections WHERE key = :key AND isSystem = 0")
    suspend fun deleteCollectionByKey(key: String)

    @Query("SELECT * FROM zotero_standalone_notes ORDER BY updatedAt DESC")
    fun observeStandaloneNotes(): Flow<List<ZoteroStandaloneNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStandaloneNote(note: ZoteroStandaloneNoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStandaloneNotes(notes: List<ZoteroStandaloneNoteEntity>)

    @Query("DELETE FROM zotero_standalone_notes WHERE id = :id")
    suspend fun deleteStandaloneNoteById(id: String)
}

@Database(
    entities = [
        ZoteroItemEntity::class,
        ZoteroCollectionEntity::class,
        ZoteroStandaloneNoteEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class ZoteroDatabase : RoomDatabase() {
    abstract fun zoteroDao(): ZoteroDao

    companion object {
        @Volatile
        private var INSTANCE: ZoteroDatabase? = null

        fun getInstance(context: Context): ZoteroDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZoteroDatabase::class.java,
                    "zotero_library_v3.db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
