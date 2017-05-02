package git

import java.io.File
import org.eclipse.jgit.api.Git

import org.gradle.api.*
import org.gradle.api.tasks.*
import org.gradle.script.lang.kotlin.*

open class GitClone : DefaultTask() {

    @get:Input
    var uri: String? = null

    @get:Input
    var ref: String? = null

    @get:OutputDirectory
    var cloneDir: File? = null

    @TaskAction
    fun gitClone(): Unit {
        cloneDir!!.deleteRecursively()
        var clone: Git? = null
        try
        {
            clone = Git.cloneRepository()
                .setURI(uri!!)
                .setDirectory(cloneDir!!)
                .setBranchesToClone(listOf(ref!!))
                .setBranch(ref!!)
                .call()
        }
        finally
        {
            clone?.close()
        }
    }
}
