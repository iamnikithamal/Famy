package com.famy.tree.domain.usecase

import android.content.Context
import android.net.Uri
import com.famy.tree.domain.model.FamilyMember
import com.famy.tree.domain.model.Gender
import com.famy.tree.domain.model.Relationship
import com.famy.tree.domain.model.RelationshipKind
import com.famy.tree.domain.repository.FamilyMemberRepository
import com.famy.tree.domain.repository.FamilyTreeRepository
import com.famy.tree.domain.repository.RelationshipRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@Serializable
data class ExportData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val trees: List<TreeExportData> = emptyList()
)

@Serializable
data class TreeExportData(
    val name: String,
    val description: String? = null,
    val members: List<MemberExportData> = emptyList(),
    val relationships: List<RelationshipExportData> = emptyList()
)

@Serializable
data class MemberExportData(
    val localId: Long,
    val firstName: String,
    val lastName: String? = null,
    val maidenName: String? = null,
    val nickname: String? = null,
    val gender: String,
    val birthDate: Long? = null,
    val birthPlace: String? = null,
    val deathDate: Long? = null,
    val deathPlace: String? = null,
    val isLiving: Boolean = true,
    val biography: String? = null,
    val occupation: String? = null,
    val generation: Int = 0,
    val customFields: Map<String, String> = emptyMap()
)

@Serializable
data class RelationshipExportData(
    val memberId: Long,
    val relatedMemberId: Long,
    val type: String
)

class ExportDataUseCase @Inject constructor(
    private val treeRepository: FamilyTreeRepository,
    private val memberRepository: FamilyMemberRepository,
    private val relationshipRepository: RelationshipRepository
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun exportToJson(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        val trees = treeRepository.getAllTrees()
        val exportData = ExportData(
            trees = trees.map { tree ->
                val members = memberRepository.getMembersByTree(tree.id)
                val relationships = relationshipRepository.getRelationshipsByTree(tree.id)

                TreeExportData(
                    name = tree.name,
                    description = tree.description,
                    members = members.map { member ->
                        MemberExportData(
                            localId = member.id,
                            firstName = member.firstName,
                            lastName = member.lastName,
                            maidenName = member.maidenName,
                            nickname = member.nickname,
                            gender = member.gender.name,
                            birthDate = member.birthDate,
                            birthPlace = member.birthPlace,
                            deathDate = member.deathDate,
                            deathPlace = member.deathPlace,
                            isLiving = member.isLiving,
                            biography = member.biography,
                            occupation = member.occupation,
                            generation = member.generation,
                            customFields = member.customFields
                        )
                    },
                    relationships = relationships.map { rel ->
                        RelationshipExportData(
                            memberId = rel.memberId,
                            relatedMemberId = rel.relatedMemberId,
                            type = rel.type.name
                        )
                    }
                )
            }
        )

        val jsonString = json.encodeToString(exportData)
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(jsonString.toByteArray())
        }
    }

    suspend fun exportToGedcom(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        val trees = treeRepository.getAllTrees()
        val gedcomBuilder = StringBuilder()
        val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.ENGLISH)

        gedcomBuilder.appendLine("0 HEAD")
        gedcomBuilder.appendLine("1 SOUR Famy")
        gedcomBuilder.appendLine("2 VERS 1.0.0")
        gedcomBuilder.appendLine("1 GEDC")
        gedcomBuilder.appendLine("2 VERS 5.5.1")
        gedcomBuilder.appendLine("2 FORM LINEAGE-LINKED")
        gedcomBuilder.appendLine("1 CHAR UTF-8")

        trees.forEach { tree ->
            val members = memberRepository.getMembersByTree(tree.id)
            val relationships = relationshipRepository.getRelationshipsByTree(tree.id)

            val memberIdMap = mutableMapOf<Long, String>()
            members.forEachIndexed { index, member ->
                val gedcomId = "I${index + 1}"
                memberIdMap[member.id] = gedcomId

                gedcomBuilder.appendLine("0 @$gedcomId@ INDI")
                gedcomBuilder.appendLine("1 NAME ${member.firstName}${member.lastName?.let { " /$it/" } ?: ""}")
                gedcomBuilder.appendLine("2 GIVN ${member.firstName}")
                member.lastName?.let { gedcomBuilder.appendLine("2 SURN $it") }

                when (member.gender) {
                    Gender.MALE -> gedcomBuilder.appendLine("1 SEX M")
                    Gender.FEMALE -> gedcomBuilder.appendLine("1 SEX F")
                    else -> {}
                }

                member.birthDate?.let { birthDate ->
                    gedcomBuilder.appendLine("1 BIRT")
                    gedcomBuilder.appendLine("2 DATE ${dateFormat.format(Date(birthDate)).uppercase()}")
                    member.birthPlace?.let { gedcomBuilder.appendLine("2 PLAC $it") }
                }

                if (!member.isLiving && member.deathDate != null) {
                    gedcomBuilder.appendLine("1 DEAT")
                    gedcomBuilder.appendLine("2 DATE ${dateFormat.format(Date(member.deathDate)).uppercase()}")
                    member.deathPlace?.let { gedcomBuilder.appendLine("2 PLAC $it") }
                }

                member.occupation?.let {
                    gedcomBuilder.appendLine("1 OCCU $it")
                }

                member.biography?.let {
                    gedcomBuilder.appendLine("1 NOTE $it")
                }
            }

            val familyGroups = mutableMapOf<String, MutableList<Relationship>>()
            relationships.filter { it.type == RelationshipKind.SPOUSE }.forEach { spouseRel ->
                val key = listOf(spouseRel.memberId, spouseRel.relatedMemberId).sorted().joinToString("-")
                familyGroups.getOrPut(key) { mutableListOf() }.add(spouseRel)
            }

            var familyIndex = 1
            familyGroups.forEach { (_, spouseRelations) ->
                if (spouseRelations.isNotEmpty()) {
                    val famId = "F$familyIndex"
                    gedcomBuilder.appendLine("0 @$famId@ FAM")

                    val spouse1 = spouseRelations.first()
                    memberIdMap[spouse1.memberId]?.let { gedcomBuilder.appendLine("1 HUSB @$it@") }
                    memberIdMap[spouse1.relatedMemberId]?.let { gedcomBuilder.appendLine("1 WIFE @$it@") }

                    val parentIds = setOf(spouse1.memberId, spouse1.relatedMemberId)
                    relationships.filter { rel ->
                        rel.type == RelationshipKind.CHILD && rel.memberId in parentIds
                    }.forEach { childRel ->
                        memberIdMap[childRel.relatedMemberId]?.let {
                            gedcomBuilder.appendLine("1 CHIL @$it@")
                        }
                    }

                    familyIndex++
                }
            }
        }

        gedcomBuilder.appendLine("0 TRLR")

        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(gedcomBuilder.toString().toByteArray())
        }
    }
}

class ImportDataUseCase @Inject constructor(
    private val treeRepository: FamilyTreeRepository,
    private val memberRepository: FamilyMemberRepository,
    private val relationshipRepository: RelationshipRepository
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun importFromJson(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader().readText()
        } ?: throw Exception("Cannot read file")

        val exportData = json.decodeFromString<ExportData>(jsonString)

        exportData.trees.forEach { treeData ->
            val tree = treeRepository.createTree(treeData.name, treeData.description)
            val oldToNewIdMap = mutableMapOf<Long, Long>()

            treeData.members.forEach { memberData ->
                val member = memberRepository.createMember(
                    FamilyMember(
                        treeId = tree.id,
                        firstName = memberData.firstName,
                        lastName = memberData.lastName,
                        maidenName = memberData.maidenName,
                        nickname = memberData.nickname,
                        gender = Gender.fromString(memberData.gender),
                        birthDate = memberData.birthDate,
                        birthPlace = memberData.birthPlace,
                        deathDate = memberData.deathDate,
                        deathPlace = memberData.deathPlace,
                        isLiving = memberData.isLiving,
                        biography = memberData.biography,
                        occupation = memberData.occupation,
                        generation = memberData.generation,
                        customFields = memberData.customFields
                    )
                )
                oldToNewIdMap[memberData.localId] = member.id
            }

            treeData.relationships.forEach { relData ->
                val newMemberId = oldToNewIdMap[relData.memberId]
                val newRelatedId = oldToNewIdMap[relData.relatedMemberId]
                if (newMemberId != null && newRelatedId != null) {
                    relationshipRepository.createRelationship(
                        Relationship(
                            memberId = newMemberId,
                            relatedMemberId = newRelatedId,
                            type = RelationshipKind.valueOf(relData.type)
                        )
                    )
                }
            }
        }
    }

    suspend fun importFromGedcom(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        val gedcomContent = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader().readText()
        } ?: throw Exception("Cannot read file")

        val lines = gedcomContent.lines()
        val tree = treeRepository.createTree("Imported Family Tree")
        val individualMap = mutableMapOf<String, FamilyMember>()
        val gedcomToDbIdMap = mutableMapOf<String, Long>()

        var currentIndividualId: String? = null
        var currentName = ""
        var currentGender = Gender.UNKNOWN
        var currentBirthDate: Long? = null
        var currentBirthPlace: String? = null
        var currentDeathDate: Long? = null
        var currentDeathPlace: String? = null
        var inBirthBlock = false
        var inDeathBlock = false

        for (line in lines) {
            val parts = line.trim().split(" ", limit = 3)
            if (parts.isEmpty()) continue

            val level = parts[0].toIntOrNull() ?: continue
            val tag = if (parts.size > 1) parts[1] else ""
            val value = if (parts.size > 2) parts[2] else ""

            when {
                level == 0 && tag.startsWith("@I") && value == "INDI" -> {
                    saveCurrentIndividual(
                        currentIndividualId, currentName, currentGender,
                        currentBirthDate, currentBirthPlace, currentDeathDate, currentDeathPlace,
                        tree.id, memberRepository, gedcomToDbIdMap
                    )

                    currentIndividualId = tag.removeSurrounding("@")
                    currentName = ""
                    currentGender = Gender.UNKNOWN
                    currentBirthDate = null
                    currentBirthPlace = null
                    currentDeathDate = null
                    currentDeathPlace = null
                    inBirthBlock = false
                    inDeathBlock = false
                }
                level == 1 && tag == "NAME" && currentIndividualId != null -> {
                    currentName = value.replace("/", "").trim()
                    inBirthBlock = false
                    inDeathBlock = false
                }
                level == 1 && tag == "SEX" && currentIndividualId != null -> {
                    currentGender = when (value.uppercase()) {
                        "M" -> Gender.MALE
                        "F" -> Gender.FEMALE
                        else -> Gender.UNKNOWN
                    }
                    inBirthBlock = false
                    inDeathBlock = false
                }
                level == 1 && tag == "BIRT" && currentIndividualId != null -> {
                    inBirthBlock = true
                    inDeathBlock = false
                }
                level == 1 && tag == "DEAT" && currentIndividualId != null -> {
                    inBirthBlock = false
                    inDeathBlock = true
                }
                level == 2 && tag == "DATE" -> {
                    val parsedDate = parseGedcomDate(value)
                    if (inBirthBlock) currentBirthDate = parsedDate
                    else if (inDeathBlock) currentDeathDate = parsedDate
                }
                level == 2 && tag == "PLAC" -> {
                    if (inBirthBlock) currentBirthPlace = value
                    else if (inDeathBlock) currentDeathPlace = value
                }
                level == 0 && tag.startsWith("@F") -> {
                    saveCurrentIndividual(
                        currentIndividualId, currentName, currentGender,
                        currentBirthDate, currentBirthPlace, currentDeathDate, currentDeathPlace,
                        tree.id, memberRepository, gedcomToDbIdMap
                    )
                    currentIndividualId = null
                }
            }
        }

        saveCurrentIndividual(
            currentIndividualId, currentName, currentGender,
            currentBirthDate, currentBirthPlace, currentDeathDate, currentDeathPlace,
            tree.id, memberRepository, gedcomToDbIdMap
        )

        parseGedcomFamilies(lines, gedcomToDbIdMap, relationshipRepository)
    }

    private suspend fun saveCurrentIndividual(
        gedcomId: String?,
        name: String,
        gender: Gender,
        birthDate: Long?,
        birthPlace: String?,
        deathDate: Long?,
        deathPlace: String?,
        treeId: Long,
        memberRepository: FamilyMemberRepository,
        idMap: MutableMap<String, Long>
    ) {
        if (gedcomId == null || name.isBlank()) return

        val nameParts = name.split(" ", limit = 2)
        val firstName = nameParts.firstOrNull() ?: name
        val lastName = nameParts.getOrNull(1)

        val member = memberRepository.createMember(
            FamilyMember(
                treeId = treeId,
                firstName = firstName,
                lastName = lastName,
                gender = gender,
                birthDate = birthDate,
                birthPlace = birthPlace,
                deathDate = deathDate,
                deathPlace = deathPlace,
                isLiving = deathDate == null
            )
        )
        idMap[gedcomId] = member.id
    }

    private suspend fun parseGedcomFamilies(
        lines: List<String>,
        idMap: Map<String, Long>,
        relationshipRepository: RelationshipRepository
    ) {
        var inFamily = false
        var husbandId: String? = null
        var wifeId: String? = null
        val childIds = mutableListOf<String>()

        for (line in lines) {
            val parts = line.trim().split(" ", limit = 3)
            if (parts.isEmpty()) continue

            val level = parts[0].toIntOrNull() ?: continue
            val tag = if (parts.size > 1) parts[1] else ""
            val value = if (parts.size > 2) parts[2] else ""

            when {
                level == 0 && tag.startsWith("@F") && value == "FAM" -> {
                    processFamilyRelationships(
                        husbandId, wifeId, childIds, idMap, relationshipRepository
                    )
                    inFamily = true
                    husbandId = null
                    wifeId = null
                    childIds.clear()
                }
                level == 1 && tag == "HUSB" && inFamily -> {
                    husbandId = value.removeSurrounding("@")
                }
                level == 1 && tag == "WIFE" && inFamily -> {
                    wifeId = value.removeSurrounding("@")
                }
                level == 1 && tag == "CHIL" && inFamily -> {
                    childIds.add(value.removeSurrounding("@"))
                }
                level == 0 && !tag.startsWith("@F") -> {
                    processFamilyRelationships(
                        husbandId, wifeId, childIds, idMap, relationshipRepository
                    )
                    inFamily = false
                }
            }
        }

        processFamilyRelationships(husbandId, wifeId, childIds, idMap, relationshipRepository)
    }

    private suspend fun processFamilyRelationships(
        husbandId: String?,
        wifeId: String?,
        childIds: List<String>,
        idMap: Map<String, Long>,
        relationshipRepository: RelationshipRepository
    ) {
        val husbandDbId = husbandId?.let { idMap[it] }
        val wifeDbId = wifeId?.let { idMap[it] }

        if (husbandDbId != null && wifeDbId != null) {
            relationshipRepository.createRelationship(
                Relationship(memberId = husbandDbId, relatedMemberId = wifeDbId, type = RelationshipKind.SPOUSE)
            )
            relationshipRepository.createRelationship(
                Relationship(memberId = wifeDbId, relatedMemberId = husbandDbId, type = RelationshipKind.SPOUSE)
            )
        }

        childIds.forEach { childGedcomId ->
            val childDbId = idMap[childGedcomId] ?: return@forEach

            husbandDbId?.let { parentId ->
                relationshipRepository.createRelationship(
                    Relationship(memberId = parentId, relatedMemberId = childDbId, type = RelationshipKind.CHILD)
                )
                relationshipRepository.createRelationship(
                    Relationship(memberId = childDbId, relatedMemberId = parentId, type = RelationshipKind.PARENT)
                )
            }
            wifeDbId?.let { parentId ->
                relationshipRepository.createRelationship(
                    Relationship(memberId = parentId, relatedMemberId = childDbId, type = RelationshipKind.CHILD)
                )
                relationshipRepository.createRelationship(
                    Relationship(memberId = childDbId, relatedMemberId = parentId, type = RelationshipKind.PARENT)
                )
            }
        }
    }

    private fun parseGedcomDate(dateString: String): Long? {
        val formats = listOf(
            SimpleDateFormat("d MMM yyyy", Locale.ENGLISH),
            SimpleDateFormat("MMM yyyy", Locale.ENGLISH),
            SimpleDateFormat("yyyy", Locale.ENGLISH)
        )

        val cleanDate = dateString.uppercase().replace("ABT ", "").replace("EST ", "").replace("BEF ", "").replace("AFT ", "")

        for (format in formats) {
            try {
                return format.parse(cleanDate)?.time
            } catch (_: Exception) {}
        }
        return null
    }
}
