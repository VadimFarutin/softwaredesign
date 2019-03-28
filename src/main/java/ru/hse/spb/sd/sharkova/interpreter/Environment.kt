package ru.hse.spb.sd.sharkova.interpreter

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object Environment {
    private var currentDirectory = Paths.get(".").toAbsolutePath().normalize()

    fun getCurrentDirectory(): Path = currentDirectory

    fun setCurrentDirectory(directory: String) {
        val newDirectory = currentDirectory.resolve(directory)

        if (!Files.exists(newDirectory) || !Files.isDirectory(newDirectory)) {
            throw WrongDirectoryException()
        }

        currentDirectory = newDirectory.toAbsolutePath().normalize()
    }

    fun getFile(path: String): File = currentDirectory.resolve(path).toFile()
}
