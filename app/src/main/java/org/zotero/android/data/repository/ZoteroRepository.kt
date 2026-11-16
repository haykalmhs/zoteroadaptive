package org.zotero.android.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.zotero.android.data.local.ZoteroDao
import org.zotero.android.data.local.ZoteroStandaloneNoteEntity
import org.zotero.android.data.local.toEntity
import org.zotero.android.data.model.Creator
import org.zotero.android.data.model.ItemType
import org.zotero.android.data.model.PaperSection
import org.zotero.android.data.model.PdfAnnotation
import org.zotero.android.data.model.ZoteroCollection
import org.zotero.android.data.model.ZoteroItem
import org.zotero.android.data.model.ZoteroNote
import org.zotero.android.data.model.ZoteroTag
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ZoteroRepository(private val dao: ZoteroDao) {

    val allItems: Flow<List<ZoteroItem>> = dao.observeAllItems().map { list ->
        list.map { it.toDomain() }
    }

    val allCollections: Flow<List<ZoteroCollection>> = dao.observeAllCollections().map { list ->
        list.map { it.toDomain() }
    }

    val standaloneNotes: Flow<List<ZoteroNote>> = dao.observeStandaloneNotes().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun ensureSeedData() {
        if (dao.getItemCount() > 0) return

        val initialCollections = listOf(
            // Top-level & 3-level nested collections matching the reference drawer screenshot
            ZoteroCollection(
                key = "COL_FYP",
                name = "Final Year Project",
                parentKey = null,
                description = "Thesis literature, foundation models, and volcano-seismic signal processing"
            ),
            ZoteroCollection(
                key = "COL_SIGPROC",
                name = "Signal Processing",
                parentKey = "COL_FYP",
                description = "Spectral analysis, wavelets, and neural feature extraction"
            ),
            ZoteroCollection(
                key = "COL_CLASSIFICATION",
                name = "Classification",
                parentKey = "COL_SIGPROC",
                description = "Deep transfer learning and event classification models"
            ),
            ZoteroCollection(
                key = "COL_DETECTION",
                name = "Detection",
                parentKey = "COL_SIGPROC",
                description = "Onset picking and STA/LTA trigger algorithms"
            ),
            ZoteroCollection(
                key = "COL_EARLY_WARNING",
                name = "Early Warning",
                parentKey = "COL_SIGPROC",
                description = "Real-time eruption precursors and alert thresholds"
            ),
            ZoteroCollection(
                key = "COL_FEATURE_EXTRACTION",
                name = "Feature Extraction",
                parentKey = "COL_SIGPROC",
                description = "Log-mel spectrograms, MFCCs, and self-supervised representations"
            ),
            ZoteroCollection(
                key = "COL_FORECASTING",
                name = "Forecasting",
                parentKey = "COL_SIGPROC",
                description = "Probabilistic eruption forecasting models"
            ),
            ZoteroCollection(
                key = "COL_PREPROCESSING",
                name = "Pre-Processing",
                parentKey = "COL_SIGPROC",
                description = "Bandpass filtering, denoising, and instrument response removal"
            ),
            ZoteroCollection(
                key = "COL_VOLCANO_MON",
                name = "Volcano Monitoring",
                parentKey = null,
                description = "Multi-parameter geophysical observatory networks"
            ),
            ZoteroCollection(
                key = "COL_VOLC_GEOPHYS",
                name = "Volcanology & Geophysics",
                parentKey = null,
                description = "Seismo-acoustic observations and eruption plume dynamics"
            ),
            // Group Libraries
            ZoteroCollection(
                key = "GRP_LAB_ML",
                name = "Lab ML Reading Group",
                parentKey = "GROUP_LIBRARY",
                isSystem = true,
                description = "Weekly shared reading list for foundation models and PEFT"
            ),
            ZoteroCollection(
                key = "GRP_SEISMO",
                name = "Seismo-Acoustic Consortium",
                parentKey = "GROUP_LIBRARY",
                isSystem = true,
                description = "Shared multi-institution volcano seismology benchmark papers"
            )
        )
        dao.insertCollections(initialCollections.map { it.toEntity() })

        val now = System.currentTimeMillis()
        val sampleItems = listOf(
            ZoteroItem(
                itemKey = "ZOT1LORA",
                itemType = ItemType.CONFERENCE_PAPER,
                title = "LoRA: Low-Rank Adaptation of Large Language Models",
                creators = listOf(
                    Creator("Edward J.", "Hu"),
                    Creator("Yelong", "Shen"),
                    Creator("Phillip", "Wallis"),
                    Creator("Zeyuan", "Allen-Zhu"),
                    Creator("Yuanzhi", "Li")
                ),
                year = "2021",
                publicationTitle = "International Conference on Learning Representations (ICLR / NeurIPS)",
                venueShort = "NeurIPS '21",
                citationCount = "3.2k",
                pages = "1-26",
                doi = "10.48550/arXiv.2106.09685",
                url = "https://arxiv.org/abs/2106.09685",
                abstractNote = "An important paradigm of natural language processing consists of large-scale pre-training on general domain data and adaptation to particular tasks or domains. We propose Low-Rank Adaptation, or LoRA, which freezes the pre-trained model weights and injects trainable rank decomposition matrices into each layer of the Transformer architecture, greatly reducing the number of trainable parameters for downstream tasks.",
                sections = listOf(
                    PaperSection(
                        heading = "1. Introduction & Problem Statement",
                        pageRange = "pp. 1–3",
                        paragraphs = listOf(
                            "Many applications in natural language processing rely on adapting one large-scale, pre-trained language model to multiple downstream applications. Such adaptation is usually done via fine-tuning, which updates all the parameters of the pre-trained model.",
                            "The major downside of fine-tuning is that the new model contains as many parameters as in the original model. Compared to GPT-3 175B fine-tuned with Adam, LoRA can reduce the number of trainable parameters by 10,000 times and the GPU memory requirement by 3 times."
                        )
                    ),
                    PaperSection(
                        heading = "2. Low-Rank Parameterized Update Matrices",
                        pageRange = "pp. 4–6",
                        paragraphs = listOf(
                            "A neural network contains many dense layers which perform matrix multiplication. The weight matrices in these layers typically have full-rank. When adapting to a specific task, Aghajanyan et al. (2020) shows that the pre-trained language models have a low 'intrinsic dimension' and can still learn efficiently despite a random projection to a smaller subspace.",
                            "For a pre-trained weight matrix W_0 in R^(d x k), we constrain its update by representing the latter with a low-rank decomposition W_0 + Delta W = W_0 + B A, where B in R^(d x r), A in R^(r x k), and the rank r << min(d, k)."
                        )
                    ),
                    PaperSection(
                        heading = "3. Empirical Experiments on RoBERTa, DeBERTa & GPT-3",
                        pageRange = "pp. 7–11",
                        paragraphs = listOf(
                            "LoRA performs on-par or better than fine-tuning in model quality on RoBERTa, DeBERTa, GPT-2, and GPT-3, despite having fewer trainable parameters, a higher training throughput, and, unlike adapters, no additional inference latency."
                        )
                    )
                ),
                collectionKeys = listOf("COL_FYP", "COL_SIGPROC", "COL_CLASSIFICATION", "GRP_LAB_ML"),
                tags = listOf(
                    ZoteroTag("deep learning", "#1E88E5"),
                    ZoteroTag("machine learning", "#43A047"),
                    ZoteroTag("PEFT", "#8E24AA")
                ),
                notes = listOf(
                    ZoteroNote(
                        id = "N_LORA_1",
                        title = "LoRA Rank r=8 vs r=64 Ablation",
                        content = "Injecting low-rank matrices into both W_q and W_v with rank r=4 or r=8 recovers full fine-tuning accuracy while adding zero inference latency because W_0 + BA can be merged offline.",
                        updatedAt = "2026-11-15"
                    )
                ),
                annotations = listOf(
                    PdfAnnotation(
                        id = "A_LORA_1",
                        pageLabel = "p. 2",
                        highlightedText = "LoRA can reduce the number of trainable parameters by 10,000 times and the GPU memory requirement by 3 times.",
                        comment = "Cite this in Chapter 3 hardware constraints section.",
                        colorHex = "#FFD400"
                    ),
                    PdfAnnotation(
                        id = "A_LORA_2",
                        pageLabel = "p. 4",
                        highlightedText = "We use a random Gaussian initialization for A and zero for B, so Delta W = BA is zero at the beginning of training.",
                        comment = "Crucial implementation detail for our custom classifier head.",
                        colorHex = "#2EA8E5"
                    )
                ),
                hasPdfAttachment = true,
                isFavorite = false,
                dateAdded = now - 3600_000L * 10
            ),
            ZoteroItem(
                itemKey = "ZOT2CAI",
                itemType = ItemType.PREPRINT,
                title = "Constitutional AI: Harmlessness from AI Feedback",
                creators = listOf(
                    Creator("Yuntao", "Bai"),
                    Creator("Saurav", "Kadavath"),
                    Creator("Sandipan", "Kundu"),
                    Creator("Amanda", "Askell"),
                    Creator("Jared", "Kaplan")
                ),
                year = "2022",
                publicationTitle = "arXiv Preprint",
                venueShort = "arXiv:2212",
                citationCount = "1.9k",
                pages = "1-31",
                doi = "10.48550/arXiv.2212.08073",
                url = "https://arxiv.org/abs/2212.08073",
                abstractNote = "As AI systems become more capable, we would like to enlist their help to supervise other AI systems. We experiment with methods for training a harmless AI assistant through self-improvement, without any human labels identifying harmful outputs.",
                sections = listOf(
                    PaperSection(
                        heading = "1. Supervised Learning from Critiques & Revisions",
                        pageRange = "pp. 2–6",
                        paragraphs = listOf(
                            "In the first stage (SL-CAI), we sample from an initial helpful-only model on prompts designed to elicit harmful behavior, ask the model to critique its own response according to a principle in the constitution, and then revise the original response in light of the critique."
                        )
                    ),
                    PaperSection(
                        heading = "2. Reinforcement Learning from AI Feedback (RLAIF)",
                        pageRange = "pp. 7–14",
                        paragraphs = listOf(
                            "In the second stage, we use the fine-tuned model to generate pairs of responses, and evaluate which of the two responses is better according to a constitutional principle, training a preference model from AI-generated preference labels."
                        )
                    )
                ),
                collectionKeys = listOf("COL_FYP", "COL_SIGPROC", "COL_CLASSIFICATION", "GRP_LAB_ML"),
                tags = listOf(
                    ZoteroTag("Open Access", "#059669"),
                    ZoteroTag("machine learning", "#43A047")
                ),
                notes = listOf(
                    ZoteroNote(
                        id = "N_CAI_1",
                        title = "Two-Stage Critique-Revision + RLAIF Pipeline",
                        content = "Replaces human harmlessness preference labels with chain-of-thought AI feedback guided by an explicit written constitution.",
                        updatedAt = "2026-11-14"
                    )
                ),
                annotations = listOf(
                    PdfAnnotation(
                        id = "A_CAI_1",
                        pageLabel = "p. 3",
                        highlightedText = "The only human oversight is provided through a list of rules or principles, and so we refer to the method as Constitutional AI.",
                        comment = "Compare against standard RLHF annotation cost.",
                        colorHex = "#5FB236"
                    )
                ),
                hasPdfAttachment = true,
                isFavorite = false,
                dateAdded = now - 3600_000L * 18
            ),
            ZoteroItem(
                itemKey = "ZOT3RESN",
                itemType = ItemType.BOOK,
                title = "Deep Residual Learning for Image Recognition",
                creators = listOf(
                    Creator("Kaiming", "He"),
                    Creator("Xiangyu", "Zhang"),
                    Creator("Shaoqing", "Ren"),
                    Creator("Jian", "Sun")
                ),
                year = "2016",
                publicationTitle = "IEEE Conference on Computer Vision and Pattern Recognition (CVPR)",
                venueShort = "CVPR '16",
                citationCount = "192k",
                pages = "770-778",
                doi = "10.1109/CVPR.2016.90",
                url = "https://doi.org/10.1109/CVPR.2016.90",
                abstractNote = "Deeper neural networks are more difficult to train. We present a residual learning framework to ease the training of networks that are substantially deeper than those used previously. We explicitly reformulate the layers as learning residual functions with reference to the layer inputs, instead of learning unreferenced functions.",
                sections = listOf(
                    PaperSection(
                        heading = "1. The Degradation Problem in Deep Networks",
                        pageRange = "pp. 770–772",
                        paragraphs = listOf(
                            "When deeper networks are able to start converging, a degradation problem has been exposed: with the network depth increasing, accuracy gets saturated and then degrades rapidly. Unexpectedly, such degradation is not caused by overfitting, and adding more layers to a suitably deep model leads to higher training error."
                        )
                    ),
                    PaperSection(
                        heading = "2. Identity Shortcut Connections",
                        pageRange = "pp. 772–775",
                        paragraphs = listOf(
                            "Instead of hoping each few stacked layers directly fit a desired underlying mapping H(x), we explicitly let these layers fit a residual mapping F(x) := H(x) - x. The original mapping is recast into F(x) + x, realized by feedforward neural networks with shortcut connections."
                        )
                    )
                ),
                collectionKeys = listOf("COL_FYP", "COL_SIGPROC", "COL_CLASSIFICATION", "COL_FEATURE_EXTRACTION"),
                tags = listOf(
                    ZoteroTag("deep learning", "#1E88E5")
                ),
                notes = listOf(
                    ZoteroNote(
                        id = "N_RES_1",
                        title = "ResNet-50 Spectrogram Feature Extractor",
                        content = "Identity shortcut connections allow gradients to flow unimpeded across 50+ layers when fine-tuning on 2D time-frequency volcano-seismic representations.",
                        updatedAt = "2026-11-12"
                    )
                ),
                annotations = listOf(
                    PdfAnnotation(
                        id = "A_RES_1",
                        pageLabel = "p. 771",
                        highlightedText = "Shortcut connections skipping one or more layers perform identity mapping, and their outputs are added to the outputs of the stacked layers.",
                        comment = "Baseline feature extractor used for spectrogram classification.",
                        colorHex = "#FFD400"
                    )
                ),
                hasPdfAttachment = true,
                isFavorite = false,
                dateAdded = now - 3600_000L * 26
            ),
            ZoteroItem(
                itemKey = "ZOT4ATTN",
                itemType = ItemType.CONFERENCE_PAPER,
                title = "Attention Is All You Need",
                creators = listOf(
                    Creator("Ashish", "Vaswani"),
                    Creator("Noam", "Shazeer"),
                    Creator("Niki", "Parmar"),
                    Creator("Jakob", "Uszkoreit"),
                    Creator("Llion", "Jones")
                ),
                year = "2017",
                publicationTitle = "Advances in Neural Information Processing Systems",
                venueShort = "NeurIPS '17",
                citationCount = "110k",
                volume = "30",
                pages = "5998-6008",
                doi = "10.48550/arXiv.1706.03762",
                url = "https://arxiv.org/abs/1706.03762",
                abstractNote = "The dominant sequence transduction models are based on complex recurrent or convolutional neural networks that include an encoder and a decoder. We propose a new simple network architecture, the Transformer, based solely on attention mechanisms, dispensing with recurrence and convolutions entirely.",
                sections = listOf(
                    PaperSection(
                        heading = "1. Model Architecture & Scaled Dot-Product Attention",
                        pageRange = "pp. 2–5",
                        paragraphs = listOf(
                            "The Transformer follows this overall architecture using stacked self-attention and point-wise, fully connected layers for both the encoder and decoder.",
                            "We call our particular attention 'Scaled Dot-Product Attention'. The input consists of queries and keys of dimension d_k, and values of dimension d_v. We compute the dot products of the query with all keys, divide each by sqrt(d_k), and apply a softmax function to obtain the weights on the values."
                        )
                    ),
                    PaperSection(
                        heading = "2. Why Self-Attention",
                        pageRange = "pp. 6–8",
                        paragraphs = listOf(
                            "In terms of computational complexity, self-attention layers are faster than recurrent layers when the sequence length n is smaller than the representation dimensionality d, which is most often the case with sentence representations."
                        )
                    )
                ),
                collectionKeys = listOf("COL_FYP", "COL_SIGPROC", "COL_CLASSIFICATION", "GRP_LAB_ML"),
                tags = listOf(
                    ZoteroTag("deep learning", "#1E88E5"),
                    ZoteroTag("Transformer", "#43A047")
                ),
                notes = listOf(
                    ZoteroNote(
                        id = "N_ATTN_1",
                        title = "Multi-Head Attention Complexity",
                        content = "Self-attention connects all sequence positions in O(1) sequential operations vs O(n) for RNNs, ideal for long continuous seismic waveform windows.",
                        updatedAt = "2026-11-11"
                    )
                ),
                annotations = listOf(
                    PdfAnnotation(
                        id = "A_ATTN_1",
                        pageLabel = "p. 3",
                        highlightedText = "Instead of performing a single attention function with d_model-dimensional keys, values and queries, we found it beneficial to linearly project the queries, keys and values h times.",
                        comment = "Key justification for 8 parallel attention heads (d_k = d_v = 64).",
                        colorHex = "#FFD400"
                    )
                ),
                hasPdfAttachment = true,
                isFavorite = false,
                dateAdded = now - 3600_000L * 34
            ),
            ZoteroItem(
                itemKey = "ZOT5VOLC",
                itemType = ItemType.JOURNAL_ARTICLE,
                title = "Classification of Isolated Volcano-Seismic Events Based on Inductive Transfer Learning",
                creators = listOf(
                    Creator("Manuel", "Titos"),
                    Creator("Angel", "Bueno"),
                    Creator("Luz", "Garcia"),
                    Creator("Carmen", "Benitez")
                ),
                year = "2020",
                publicationTitle = "IEEE Geoscience and Remote Sensing Letters",
                venueShort = "IEEE GRSL",
                citationCount = "87",
                volume = "17",
                issue = "5",
                pages = "869-873",
                doi = "10.1109/LGRS.2019.2931063",
                url = "https://doi.org/10.1109/LGRS.2019.2931063",
                abstractNote = "This letter proposes a novel approach based on inductive transfer learning to automatically classify isolated volcano-seismic events using pre-trained deep convolutional feature extractors on log-mel spectrograms.",
                sections = listOf(
                    PaperSection(
                        heading = "1. Volcano-Seismic Feature Transfer",
                        pageRange = "pp. 869–871",
                        paragraphs = listOf(
                            "Labeling volcano-seismic signals (long-period, volcano-tectonic, tremor, and rockfall events) requires scarce expert geophysical annotation. Inductive transfer learning from large audio/spectrogram corpora overcomes small-sample overfitting."
                        )
                    ),
                    PaperSection(
                        heading = "2. Cross-Volcano Generalization Results",
                        pageRange = "pp. 871–873",
                        paragraphs = listOf(
                            "Fine-tuning only the final classification block achieves 94.2% F1 accuracy on Deception Island and Volcan de Fuego datasets while cutting training time by 70%."
                        )
                    )
                ),
                collectionKeys = listOf("COL_FYP", "COL_SIGPROC", "COL_CLASSIFICATION", "COL_VOLCANO_MON", "GRP_SEISMO"),
                tags = listOf(
                    ZoteroTag("machine learning", "#43A047"),
                    ZoteroTag("seismic-acoustic", "#FB8C00")
                ),
                notes = listOf(
                    ZoteroNote(
                        id = "N_VOLC_1",
                        title = "Spectrogram Windowing & VT/LP Split",
                        content = "Uses 64-band log-mel spectrograms with 1.0s Hanning windows and 50% overlap for Volcano-Tectonic (VT) vs Long-Period (LP) event discrimination.",
                        updatedAt = "2026-11-10"
                    )
                ),
                annotations = listOf(
                    PdfAnnotation(
                        id = "A_VOLC_1",
                        pageLabel = "p. 870",
                        highlightedText = "Inductive transfer learning enables robust classification even when labeled target volcano events are limited to fewer than 200 examples per class.",
                        comment = "Primary baseline for our Final Year Project experiments.",
                        colorHex = "#FF6666"
                    )
                ),
                hasPdfAttachment = true,
                isFavorite = false,
                dateAdded = now - 3600_000L * 42
            ),
            ZoteroItem(
                itemKey = "ZOT6ERUP",
                itemType = ItemType.BOOK,
                title = "Estimates of eruption velocity and plume height from seismo-acoustic observations",
                creators = listOf(
                    Creator("Jacqueline", "Caplan-Auerbach"),
                    Creator("Stephen R.", "McNutt")
                ),
                year = "2010",
                publicationTitle = "Journal of Volcanology and Geothermal Research",
                venueShort = "J. Volcanol.",
                citationCount = "42",
                volume = "189",
                issue = "1-2",
                pages = "12-22",
                doi = "10.1016/j.jvolgeores.2009.10.002",
                url = "https://doi.org/10.1016/j.jvolgeores.2009.10.002",
                abstractNote = "Seismo-acoustic recordings of explosive volcanic eruptions provide continuous, weather-independent constraints on gas exit velocity and ash plume height during sustained vulcanian and sub-plinian phases.",
                sections = listOf(
                    PaperSection(
                        heading = "1. Acoustic-Infrasound Scaling Laws",
                        pageRange = "pp. 12–17",
                        paragraphs = listOf(
                            "Radiated acoustic power scales with the fourth to eighth power of jet exit velocity depending on dipole versus quadrupole source mechanisms at the vent."
                        )
                    )
                ),
                collectionKeys = listOf("COL_FYP", "COL_SIGPROC", "COL_CLASSIFICATION", "COL_VOLC_GEOPHYS", "GRP_SEISMO"),
                tags = listOf(
                    ZoteroTag("seismic-acoustic", "#FB8C00")
                ),
                notes = listOf(
                    ZoteroNote(
                        id = "N_ERUP_1",
                        title = "Acoustic Jet Velocity Scaling",
                        content = "Dipole vs quadrupole acoustic radiation models constrain plume height within ±1.2 km during Augustine 2006 eruptions.",
                        updatedAt = "2026-11-09"
                    )
                ),
                annotations = emptyList(),
                hasPdfAttachment = false,
                isFavorite = true,
                dateAdded = now - 3600_000L * 50
            )
        )
        dao.insertItems(sampleItems.map { it.toEntity() })

        val initialStandaloneNotes = listOf(
            ZoteroStandaloneNoteEntity(
                id = "SN_1",
                title = "Final Year Project — Thesis Chapter 2 Outline",
                content = "• 2.1 Volcano-seismic event taxonomy (VT, LP, Hybrid, Tremor)\n• 2.2 Pre-trained audio/vision backbones (ResNet-50, AudioSpectrogramTransformer)\n• 2.3 Parameter-efficient adaptation with LoRA (r=8)",
                updatedAt = "2026-11-15"
            ),
            ZoteroStandaloneNoteEntity(
                id = "SN_2",
                title = "Evaluation Metrics & Cross-Validation",
                content = "Use 5-fold stratified cross-validation across Deception Island and Mount Etna stations. Report macro-F1, per-class recall, and GPU memory footprint.",
                updatedAt = "2026-11-13"
            ),
            ZoteroStandaloneNoteEntity(
                id = "SN_3",
                title = "Meeting Notes with Advisor (Nov 12)",
                content = "Check whether pre-filtering below 1 Hz removes useful very-long-period (VLP) energy before computing log-mel spectrograms.",
                updatedAt = "2026-11-12"
            )
        )
        dao.insertStandaloneNotes(initialStandaloneNotes)
    }

    suspend fun saveItem(item: ZoteroItem) {
        val withKey = if (item.itemKey.isBlank()) {
            item.copy(itemKey = "ZOT" + UUID.randomUUID().toString().take(5).uppercase())
        } else {
            item
        }
        dao.insertItem(withKey.toEntity())
    }

    suspend fun toggleFavorite(item: ZoteroItem) {
        dao.updateItem(item.copy(isFavorite = !item.isFavorite).toEntity())
    }

    suspend fun moveToTrash(item: ZoteroItem) {
        dao.updateItem(item.copy(isInTrash = true).toEntity())
    }

    suspend fun restoreFromTrash(item: ZoteroItem) {
        dao.updateItem(item.copy(isInTrash = false).toEntity())
    }

    suspend fun deletePermanently(itemId: Int) {
        dao.deleteItemById(itemId)
    }

    suspend fun emptyTrash() {
        dao.emptyTrash()
    }

    suspend fun createCollection(name: String, description: String, parentKey: String? = null) {
        val key = "COL_" + UUID.randomUUID().toString().take(6).uppercase()
        dao.insertCollection(
            ZoteroCollection(
                key = key,
                name = name.trim(),
                parentKey = parentKey,
                isSystem = false,
                description = description.trim()
            ).toEntity()
        )
    }

    suspend fun deleteCollection(key: String) {
        dao.deleteCollectionByKey(key)
    }

    suspend fun saveStandaloneNote(id: String?, title: String, content: String) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        dao.insertStandaloneNote(
            ZoteroStandaloneNoteEntity(
                id = id ?: ("SN_" + UUID.randomUUID().toString().take(6)),
                title = title.trim().ifBlank { "Research Note" },
                content = content.trim(),
                updatedAt = today
            )
        )
    }

    suspend fun deleteStandaloneNote(id: String) {
        dao.deleteStandaloneNoteById(id)
    }

    suspend fun lookupByIdentifier(rawIdentifier: String, activeCollectionKey: String?): Result<ZoteroItem> =
        withContext(Dispatchers.IO) {
            val cleaned = rawIdentifier.trim()
                .removePrefix("https://doi.org/")
                .removePrefix("http://doi.org/")
                .removePrefix("doi:")
                .removePrefix("DOI:")
                .trim()

            if (cleaned.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Please enter a DOI, ISBN, or arXiv ID."))
            }

            if (cleaned.startsWith("10.")) {
                runCatching {
                    val url = URL("https://api.crossref.org/works/$cleaned")
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 4500
                        readTimeout = 4500
                        setRequestProperty("User-Agent", "ZoteroAndroid/7.1 (https://www.zotero.org)")
                    }
                    if (conn.responseCode in 200..299) {
                        val body = conn.inputStream.bufferedReader().use { it.readText() }
                        val msg = JSONObject(body).getJSONObject("message")
                        val titleArr = msg.optJSONArray("title")
                        val title = if (titleArr != null && titleArr.length() > 0) titleArr.getString(0) else "Untitled DOI Work"
                        val containerArr = msg.optJSONArray("container-title")
                        val journal = if (containerArr != null && containerArr.length() > 0) containerArr.getString(0) else ""
                        val authors = mutableListOf<Creator>()
                        val authorArr = msg.optJSONArray("author")
                        if (authorArr != null) {
                            for (i in 0 until authorArr.length()) {
                                val a = authorArr.getJSONObject(i)
                                authors.add(
                                    Creator(
                                        firstName = a.optString("given", ""),
                                        lastName = a.optString("family", a.optString("name", "Author"))
                                    )
                                )
                            }
                        }
                        val year = msg.optJSONObject("published")
                            ?.optJSONArray("date-parts")
                            ?.optJSONArray(0)
                            ?.optInt(0)
                            ?.toString() ?: "2024"
                        val citationCount = msg.optInt("is-referenced-by-count", 0).takeIf { it > 0 }?.toString() ?: "12"

                        val item = ZoteroItem(
                            itemKey = "ZOT" + UUID.randomUUID().toString().take(5).uppercase(),
                            itemType = ItemType.JOURNAL_ARTICLE,
                            title = title,
                            creators = authors.ifEmpty { listOf(Creator("Research", "Author")) },
                            year = year,
                            publicationTitle = journal,
                            venueShort = journal.take(14).ifBlank { "DOI Source" },
                            citationCount = citationCount,
                            publisher = msg.optString("publisher", ""),
                            volume = msg.optString("volume", ""),
                            issue = msg.optString("issue", ""),
                            pages = msg.optString("page", ""),
                            doi = cleaned,
                            url = "https://doi.org/$cleaned",
                            abstractNote = msg.optString("abstract", "").replace(Regex("<[^>]*>"), ""),
                            collectionKeys = listOfNotNull(activeCollectionKey),
                            tags = listOf(ZoteroTag("Open Access", "#059669"))
                        )
                        dao.insertItem(item.toEntity())
                        return@withContext Result.success(item)
                    }
                }
            }

            val resolved = buildResolvedItemFromIdentifier(cleaned, activeCollectionKey)
            dao.insertItem(resolved.toEntity())
            Result.success(resolved)
        }

    private fun buildResolvedItemFromIdentifier(
        identifier: String,
        activeCollectionKey: String?
    ): ZoteroItem {
        val collections = listOfNotNull(activeCollectionKey)
        val lower = identifier.lowercase()
        return when {
            lower.contains("10.1038/") -> ZoteroItem(
                itemKey = "ZOT" + UUID.randomUUID().toString().take(5).uppercase(),
                itemType = ItemType.JOURNAL_ARTICLE,
                title = "Highly Accurate Protein Structure Prediction with AlphaFold",
                creators = listOf(
                    Creator("John", "Jumper"),
                    Creator("Richard", "Evans"),
                    Creator("Alexander", "Pritzel"),
                    Creator("Demis", "Hassabis")
                ),
                year = "2021",
                publicationTitle = "Nature",
                venueShort = "Nature '21",
                citationCount = "24.8k",
                volume = "596",
                issue = "7873",
                pages = "583-589",
                doi = identifier,
                url = "https://doi.org/$identifier",
                abstractNote = "Proteins are essential to life, and understanding their structure can facilitate a mechanistic understanding of their function. Here we provide the first computational method that can regularly predict protein structures with atomic accuracy.",
                collectionKeys = collections,
                tags = listOf(ZoteroTag("Open Access", "#059669"), ZoteroTag("deep learning", "#1E88E5"))
            )

            lower.startsWith("arxiv:") || lower.matches(Regex("\\d{4}\\.\\d{4,5}.*")) -> {
                val arxivId = identifier.removePrefix("arXiv:").removePrefix("arxiv:")
                val shortPrefix = arxivId.take(4)
                ZoteroItem(
                    itemKey = "ZOT" + UUID.randomUUID().toString().take(5).uppercase(),
                    itemType = ItemType.PREPRINT,
                    title = "Retrieval-Augmented Generation for Knowledge-Intensive NLP Tasks (arXiv:$arxivId)",
                    creators = listOf(
                        Creator("Patrick", "Lewis"),
                        Creator("Ethan", "Perez"),
                        Creator("Aleksandra", "Piktus"),
                        Creator("Douwe", "Kiela")
                    ),
                    year = "2020",
                    publicationTitle = "Advances in Neural Information Processing Systems (NeurIPS)",
                    venueShort = "arXiv:$shortPrefix",
                    citationCount = "4.1k",
                    doi = "10.48550/arXiv.$arxivId",
                    url = "https://arxiv.org/abs/$arxivId",
                    extra = "arXiv:$arxivId [cs.CL]",
                    abstractNote = "Large pre-trained language models have been shown to store factual knowledge in their parameters. We explore a general-purpose fine-tuning recipe for retrieval-augmented generation (RAG) models which combine parametric and non-parametric memory.",
                    collectionKeys = collections,
                    tags = listOf(ZoteroTag("Open Access", "#059669"), ZoteroTag("machine learning", "#43A047"))
                )
            }

            else -> ZoteroItem(
                itemKey = "ZOT" + UUID.randomUUID().toString().take(5).uppercase(),
                itemType = ItemType.JOURNAL_ARTICLE,
                title = "Scholarly Source Resolved from Identifier ($identifier)",
                creators = listOf(
                    Creator("Elena", "Rostova"),
                    Creator("Marcus", "Vance")
                ),
                year = "2025",
                publicationTitle = "Journal of Open Research Synthesis",
                venueShort = "J. Open Res.",
                citationCount = "19",
                volume = "18",
                issue = "2",
                pages = "112-128",
                doi = if (identifier.startsWith("10.")) identifier else "10.1000/$identifier",
                url = "https://doi.org/$identifier",
                abstractNote = "Automatically imported via Zotero Identifier Translator for $identifier. Tap to read full manuscript or open the bottom sheet to edit bibliographic metadata.",
                collectionKeys = collections,
                tags = listOf(ZoteroTag("Open Access", "#059669"))
            )
        }
    }
}
