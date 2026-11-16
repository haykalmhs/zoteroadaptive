package org.zotero.android.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class ItemType(val key: String, val label: String, val cslType: String) {
    JOURNAL_ARTICLE("journalArticle", "Journal Article", "article-journal"),
    BOOK("book", "Book", "book"),
    CONFERENCE_PAPER("conferencePaper", "Conference Paper", "paper-conference"),
    PREPRINT("preprint", "Preprint", "article"),
    THESIS("thesis", "Thesis", "thesis"),
    REPORT("report", "Report", "report"),
    WEBPAGE("webpage", "Web Page", "webpage");

    companion object {
        fun fromKey(key: String): ItemType =
            entries.firstOrNull { it.key == key } ?: JOURNAL_ARTICLE
    }
}

@Serializable
data class Creator(
    val firstName: String,
    val lastName: String,
    val creatorType: String = "author"
) {
    val fullName: String
        get() = if (firstName.isNotBlank()) "$firstName $lastName" else lastName

    val citationName: String
        get() = if (firstName.isNotBlank()) "$lastName, ${firstName.first()}." else lastName
}

@Serializable
data class ZoteroTag(
    val name: String,
    val colorHex: String? = null
)

@Serializable
data class ZoteroNote(
    val id: String,
    val title: String,
    val content: String,
    val updatedAt: String,
    val parentItemId: Int? = null,
    val parentItemTitle: String? = null
)

@Serializable
data class PdfAnnotation(
    val id: String,
    val pageLabel: String,
    val highlightedText: String,
    val comment: String,
    val colorHex: String = "#FFD400",
    val authorName: String = "Researcher"
)

@Serializable
data class PaperSection(
    val heading: String,
    val pageRange: String,
    val paragraphs: List<String>
)

@Serializable
data class ZoteroItem(
    val id: Int = 0,
    val itemKey: String,
    val itemType: ItemType,
    val title: String,
    val creators: List<Creator>,
    val year: String,
    val publicationTitle: String,
    val venueShort: String = "",
    val citationCount: String = "",
    val publisher: String = "",
    val volume: String = "",
    val issue: String = "",
    val pages: String = "",
    val doi: String = "",
    val isbn: String = "",
    val url: String = "",
    val abstractNote: String = "",
    val extra: String = "",
    val sections: List<PaperSection> = emptyList(),
    val collectionKeys: List<String> = emptyList(),
    val tags: List<ZoteroTag> = emptyList(),
    val notes: List<ZoteroNote> = emptyList(),
    val annotations: List<PdfAnnotation> = emptyList(),
    val hasPdfAttachment: Boolean = true,
    val isFavorite: Boolean = false,
    val isInTrash: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis()
) {
    /**
     * Author surname list matching the Gmail-inspired Zotero list:
     * e.g. "Hu, Shen, Wallis, Allen-Zhu..." or "He, Zhang, Ren, Sun"
     */
    val creatorListLine: String
        get() = when {
            creators.isEmpty() -> "Unknown Author"
            creators.size <= 4 -> creators.joinToString(", ") { it.lastName }
            else -> creators.take(4).joinToString(", ") { it.lastName } + "..."
        }

    val creatorSummary: String
        get() = when {
            creators.isEmpty() -> "Unknown Author"
            creators.size == 1 -> creators.first().lastName
            creators.size == 2 -> "${creators[0].lastName} & ${creators[1].lastName}"
            else -> "${creators.first().lastName} et al."
        }

    val displayVenueBadge: String
        get() = when {
            venueShort.isNotBlank() -> venueShort
            publicationTitle.isNotBlank() -> {
                if (publicationTitle.length <= 16) publicationTitle
                else publicationTitle.split(" ").take(2).joinToString(" ")
            }
            publisher.isNotBlank() -> publisher.take(16)
            else -> itemType.label
        }

    val citeKey: String
        get() {
            val authorSlug = creators.firstOrNull()?.lastName?.lowercase()?.filter { it.isLetter() } ?: "anon"
            val titleWord = title.split(" ")
                .firstOrNull { it.length > 3 && it.lowercase() !in setOf("the", "from", "with", "into", "over") }
                ?.lowercase()
                ?.filter { it.isLetter() } ?: "work"
            val yr = year.ifBlank { "nd" }
            return "$authorSlug$yr$titleWord"
        }
}

@Serializable
data class ZoteroCollection(
    val key: String,
    val name: String,
    val parentKey: String? = null,
    val isSystem: Boolean = false,
    val description: String = "",
    val colorHex: String? = null,
    val sortOrder: Int = 0
)

enum class SortField(val label: String) {
    DATE_ADDED("Date Added"),
    YEAR("Year (Newest)"),
    TITLE("Title (A–Z)"),
    CREATOR("Author (A–Z)")
}

enum class CitationStyle(val label: String, val shortLabel: String) {
    APA_7("APA 7th Edition", "APA 7"),
    MLA_9("MLA 9th Edition", "MLA 9"),
    CHICAGO_17("Chicago 17th (Author-Date)", "Chicago"),
    IEEE("IEEE", "IEEE"),
    HARVARD("Harvard Reference Format", "Harvard"),
    BIBTEX("BibTeX (.bib)", "BibTeX"),
    RIS("RIS Format (.ris)", "RIS")
}

object CitationFormatter {
    fun formatBibliography(item: ZoteroItem, style: CitationStyle, index: Int = 1): String {
        return when (style) {
            CitationStyle.APA_7 -> {
                val authors = if (item.creators.isEmpty()) "Anonymous." else {
                    item.creators.joinToString(", ") { it.citationName }
                }
                val yr = if (item.year.isNotBlank()) "(${item.year})." else "(n.d.)."
                val venue = buildString {
                    if (item.publicationTitle.isNotBlank()) {
                        append(" ${item.publicationTitle}")
                        if (item.volume.isNotBlank()) append(", ${item.volume}")
                        if (item.issue.isNotBlank()) append("(${item.issue})")
                        if (item.pages.isNotBlank()) append(", ${item.pages}")
                        append(".")
                    } else if (item.publisher.isNotBlank()) {
                        append(" ${item.publisher}.")
                    }
                }
                val doiPart = when {
                    item.doi.isNotBlank() -> " https://doi.org/${item.doi.removePrefix("https://doi.org/")}"
                    item.url.isNotBlank() -> " ${item.url}"
                    else -> ""
                }
                "$authors $yr ${item.title}.$venue$doiPart".trim()
            }

            CitationStyle.MLA_9 -> {
                val authors = when {
                    item.creators.isEmpty() -> "Anonymous."
                    item.creators.size == 1 -> "${item.creators[0].lastName}, ${item.creators[0].firstName}."
                    item.creators.size == 2 -> "${item.creators[0].lastName}, ${item.creators[0].firstName}, and ${item.creators[1].fullName}."
                    else -> "${item.creators[0].lastName}, ${item.creators[0].firstName}, et al."
                }
                val container = if (item.publicationTitle.isNotBlank()) " ${item.publicationTitle}," else ""
                val vol = if (item.volume.isNotBlank()) " vol. ${item.volume}," else ""
                val no = if (item.issue.isNotBlank()) " no. ${item.issue}," else ""
                val yr = if (item.year.isNotBlank()) " ${item.year}," else ""
                val pp = if (item.pages.isNotBlank()) " pp. ${item.pages}." else "."
                val doi = if (item.doi.isNotBlank()) " https://doi.org/${item.doi}" else ""
                "$authors \"${item.title}.\"$container$vol$no$yr$pp$doi".replace(",.", ".").trim()
            }

            CitationStyle.CHICAGO_17 -> {
                val authors = if (item.creators.isEmpty()) "Anonymous" else {
                    item.creators.mapIndexed { idx, c ->
                        if (idx == 0) "${c.lastName}, ${c.firstName}" else c.fullName
                    }.joinToString(", ")
                }
                val yr = item.year.ifBlank { "n.d." }
                val venue = if (item.publicationTitle.isNotBlank()) {
                    val volIssue = buildString {
                        if (item.volume.isNotBlank()) append(" ${item.volume}")
                        if (item.issue.isNotBlank()) append(", no. ${item.issue}")
                        if (item.pages.isNotBlank()) append(": ${item.pages}")
                    }
                    " ${item.publicationTitle}$volIssue."
                } else if (item.publisher.isNotBlank()) {
                    " ${item.publisher}."
                } else ""
                val doi = if (item.doi.isNotBlank()) " https://doi.org/${item.doi}." else ""
                "$authors. $yr. \"${item.title}.\"$venue$doi".trim()
            }

            CitationStyle.IEEE -> {
                val authors = if (item.creators.isEmpty()) "Anonymous" else {
                    item.creators.joinToString(", ") { c ->
                        val init = c.firstName.firstOrNull()?.let { "$it. " } ?: ""
                        "$init${c.lastName}"
                    }
                }
                val venue = if (item.publicationTitle.isNotBlank()) ", ${item.publicationTitle}" else ""
                val vol = if (item.volume.isNotBlank()) ", vol. ${item.volume}" else ""
                val no = if (item.issue.isNotBlank()) ", no. ${item.issue}" else ""
                val pp = if (item.pages.isNotBlank()) ", pp. ${item.pages}" else ""
                val yr = if (item.year.isNotBlank()) ", ${item.year}" else ""
                val doi = if (item.doi.isNotBlank()) ", doi: ${item.doi}" else ""
                "[$index] $authors, \"${item.title}\"$venue$vol$no$pp$yr$doi."
            }

            CitationStyle.HARVARD -> {
                val authors = if (item.creators.isEmpty()) "Anon" else {
                    item.creators.joinToString(", ") { it.citationName }
                }
                val yr = item.year.ifBlank { "n.d." }
                val venue = if (item.publicationTitle.isNotBlank()) " ${item.publicationTitle}," else ""
                val volIssue = buildString {
                    if (item.volume.isNotBlank()) append(" ${item.volume}")
                    if (item.issue.isNotBlank()) append("(${item.issue})")
                    if (item.pages.isNotBlank()) append(", pp. ${item.pages}")
                }
                "$authors ($yr) '${item.title}',$venue$volIssue.".replace(",.", ".").trim()
            }

            CitationStyle.BIBTEX -> {
                val bibType = when (item.itemType) {
                    ItemType.JOURNAL_ARTICLE -> "article"
                    ItemType.BOOK -> "book"
                    ItemType.CONFERENCE_PAPER -> "inproceedings"
                    ItemType.THESIS -> "phdthesis"
                    ItemType.REPORT -> "techreport"
                    else -> "misc"
                }
                val authors = item.creators.joinToString(" and ") { "${it.lastName}, ${it.firstName}" }
                buildString {
                    appendLine("@$bibType{${item.citeKey},")
                    appendLine("  title = {${item.title}},")
                    if (authors.isNotBlank()) appendLine("  author = {$authors},")
                    if (item.year.isNotBlank()) appendLine("  year = {${item.year}},")
                    if (item.publicationTitle.isNotBlank()) {
                        val fieldName = if (item.itemType == ItemType.CONFERENCE_PAPER) "booktitle" else "journal"
                        appendLine("  $fieldName = {${item.publicationTitle}},")
                    }
                    if (item.publisher.isNotBlank()) appendLine("  publisher = {${item.publisher}},")
                    if (item.volume.isNotBlank()) appendLine("  volume = {${item.volume}},")
                    if (item.issue.isNotBlank()) appendLine("  number = {${item.issue}},")
                    if (item.pages.isNotBlank()) appendLine("  pages = {${item.pages}},")
                    if (item.doi.isNotBlank()) appendLine("  doi = {${item.doi}},")
                    if (item.url.isNotBlank()) appendLine("  url = {${item.url}}")
                    append("}")
                }
            }

            CitationStyle.RIS -> {
                val risType = when (item.itemType) {
                    ItemType.JOURNAL_ARTICLE -> "JOUR"
                    ItemType.BOOK -> "BOOK"
                    ItemType.CONFERENCE_PAPER -> "CONF"
                    ItemType.THESIS -> "THES"
                    ItemType.REPORT -> "RPRT"
                    else -> "GEN"
                }
                buildString {
                    appendLine("TY  - $risType")
                    appendLine("TI  - ${item.title}")
                    item.creators.forEach { c ->
                        appendLine("AU  - ${c.lastName}, ${c.firstName}")
                    }
                    if (item.year.isNotBlank()) appendLine("PY  - ${item.year}")
                    if (item.publicationTitle.isNotBlank()) appendLine("T2  - ${item.publicationTitle}")
                    if (item.volume.isNotBlank()) appendLine("VL  - ${item.volume}")
                    if (item.issue.isNotBlank()) appendLine("IS  - ${item.issue}")
                    if (item.pages.isNotBlank()) appendLine("SP  - ${item.pages}")
                    if (item.doi.isNotBlank()) appendLine("DO  - ${item.doi}")
                    item.tags.forEach { t -> appendLine("KW  - ${t.name}") }
                    append("ER  - ")
                }
            }
        }
    }

    fun formatInTextCitation(item: ZoteroItem, style: CitationStyle, index: Int = 1): String {
        val yr = item.year.ifBlank { "n.d." }
        return when (style) {
            CitationStyle.IEEE -> "[$index]"
            CitationStyle.MLA_9 -> "(${item.creatorSummary}${if (item.pages.isNotBlank()) " ${item.pages.substringBefore("-")}" else ""})"
            CitationStyle.BIBTEX -> "\\cite{${item.citeKey}}"
            else -> "(${item.creatorSummary}, $yr)"
        }
    }
}
