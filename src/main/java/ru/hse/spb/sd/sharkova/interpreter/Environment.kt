package ru.hse.spb.sd.sharkova.interpreter

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Environment represents system state and provides methods to interact with it.
 */
object Environment {
    private var currentDirectory = Paths.get(".").toAbsolutePath().normalize()

    /**
     * Getter for a working directory.
     * @return current directory
     */
    fun getCurrentDirectory(): Path = currentDirectory

    /**
     * Sets a new working directory, if exists.
     * @param directory path to a new directory
     */
    fun setCurrentDirectory(directory: String) {
        val newDirectory = currentDirectory.resolve(directory)

        if (!Files.exists(newDirectory) || !Files.isDirectory(newDirectory)) {
            throw WrongDirectoryException()
        }

        currentDirectory = newDirectory.toAbsolutePath().normalize()
    }

    /**
     * Getter for a file in or outside working directory.
     * @param path path to the file
     * @return required file
     */
    fun getFile(path: String): File = currentDirectory.resolve(path).toFile()
}
