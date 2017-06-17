package git

import java.io.File
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.LsRemoteCommand
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

import org.gradle.api.*
import org.gradle.api.tasks.*

open class GitClone : DefaultTask() {

    @get:Input
    var uri: String? = null

    @get:Internal
    var ref: String? = null

    @get:OutputDirectory
    var cloneDir: File? = null

    private
    val lastCommitHashFile: File =
        File(temporaryDir, "lastCommitHash")

    init {
        outputs.upToDateWhen {
            if (!lastCommitHashFile.isFile) {
                false
            } else {
                val remoteCommitHash = retrieveRemoteRefCommitHash()
                val lastCommitHash = lastCommitHashFile.readText()
                lastCommitHash == remoteCommitHash
            }
        }
    }

    @TaskAction
    fun gitClone(): Unit {
        cloneDir!!.deleteRecursively()
        var clone: Git? = null
        try {
            clone = Git.cloneRepository()
                .setURI(uri!!)
                .setDirectory(cloneDir!!)
                .setBranchesToClone(listOf(ref!!))
                .setBranch(ref!!)
                .call()
        } finally {
            clone?.close()
        }
        lastCommitHashFile.writeText(readCloneCurrentCommitHash())
    }

    private
    fun readCloneCurrentCommitHash(): String {
        var repo: Repository? = null
        try {
            repo = FileRepositoryBuilder().setWorkTree(cloneDir).build()
            return repo.findRef(ref!!)!!.objectId.name
        } finally {
            repo?.close()
        }
    }

    private
    fun retrieveRemoteRefCommitHash(): String =
        LsRemoteCommand(null)
            .setRemote(uri!!)
            .setHeads(true)
            .call().find { it.name.substringAfter("refs/heads/") == ref!! }?.objectId?.name ?: ref!!
}
