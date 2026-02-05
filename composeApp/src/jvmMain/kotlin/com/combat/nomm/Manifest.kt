package com.combat.nomm

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.concurrent.ThreadLocalRandom

typealias Manifest = List<Extension>

@Serializable
data class Extension(
    val id: String,
    val displayName: String = id,
    val description: String = "",
    val tags: List<String> = emptyList(),
    val infoUrl: String? = null,
    val urls: List<UrlReference> = emptyList(),
    val authors: List<String> = emptyList(),
    val artifacts: List<Artifact>,
    val downloadCount: Int? = null,
    @Transient val real: Boolean = true
)

@Serializable
data class Artifact(
    val fileName: String? = null,
    val version: Version,
    val category: String? = null,
    val type: String? = null,
    val gameVersion: String? = null,
    val downloadUrl: String,
    val hash: String? = null,
    val extends: PackageReference? = null,
    val dependencies: List<PackageReference> = emptyList(),
    val incompatibilities: List<PackageReference> = emptyList()
)

@Serializable
data class UrlReference(
    val name: String,
    val url: String,
)

@Serializable
data class PackageReference(
    val id: String,
    val version: Version? = null
) {
}

@Serializable(with = VersionSerializer::class)
class Version(vararg components: Int) : Comparable<Version> {

    private val parts: List<Int> = components.toList()

    override fun toString(): String = parts.joinToString(".")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Version) return false
        return this.parts == other.parts
    }

    override fun hashCode(): Int = parts.hashCode()

    override fun compareTo(other: Version): Int {
        val maxLength = maxOf(this.parts.size, other.parts.size)
        for (i in 0 until maxLength) {
            val thisPart = this.parts.getOrElse(i) { 0 }
            val otherPart = other.parts.getOrElse(i) { 0 }
            if (thisPart != otherPart) {
                return thisPart.compareTo(otherPart)
            }
        }
        return 0
    }
}


fun fetchFakeManifest(): List<Extension> {
    val latinWords = arrayOf(
        "lorem", "ipsum", "dolor", "sit", "amet", "consectetur", "adipiscing", "elit",
        "terra", "nova", "ignis", "aqua", "ventus", "lux", "umbra", "vita"
    )

    val rnd = ThreadLocalRandom.current()
    val modCount = rnd.nextInt(1000, 3000)

    val manifest = ArrayList<Extension>(modCount)
    val allIds = Array(modCount) { i -> "pkg_$i" }
    val upperLatin = Array(latinWords.size) { latinWords[it].uppercase() }

    for (i in 0 until modCount) {
        val pkgId = allIds[i]
        val author = upperLatin[rnd.nextInt(16)]
        val name1 = latinWords[rnd.nextInt(16)]
        val name2 = latinWords[rnd.nextInt(16)]
        val pkgName = "$name1 $name2"

        val versionCount = rnd.nextInt(10, 30)
        val artifacts = ArrayList<Artifact>(versionCount)

        for (v in 0 until versionCount) {
            val depCount = rnd.nextInt(10, 20)
            val deps = ArrayList<PackageReference>(depCount)
            repeat(depCount) {
                deps.add(PackageReference(allIds[rnd.nextInt(modCount)], Version(1, rnd.nextInt(10), 0)))
            }

            val fastHash = java.lang.Long.toHexString(rnd.nextLong()) + java.lang.Long.toHexString(rnd.nextLong())

            artifacts.add(Artifact(
                fileName = "$pkgId-$v.zip",
                version = Version(1, v, 0),
                category = "Release",
                type = "Mod",
                gameVersion = "0.31",
                downloadUrl = "https://cdn.ex.com/$author/$pkgId-$v.zip",
                hash = fastHash,
                extends = null,
                dependencies = deps,
                incompatibilities = emptyList()
            ))
        }

        // Optimized Random Tags: 1-4 random single words from latinWords
        val tagCount = rnd.nextInt(1, 5)
        val tags = ArrayList<String>(tagCount)
        repeat(tagCount) {
            tags.add(latinWords[rnd.nextInt(16)])
        }

        manifest.add(Extension(
            id = pkgId,
            displayName = pkgName,
            description = "Description for $pkgName: ${latinWords[rnd.nextInt(16)]} ${latinWords[rnd.nextInt(16)]}",
            tags = tags,
            infoUrl = "https://ex.com/$author/$pkgId",
            authors = listOf(author),
            artifacts = artifacts,
            downloadCount = rnd.nextInt(1, 100000000),
            real = false,
        ))
    }
    return manifest
}