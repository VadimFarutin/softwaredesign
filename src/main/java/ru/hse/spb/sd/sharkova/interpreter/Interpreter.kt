package ru.hse.spb.sd.sharkova.interpreter

import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.nio.charset.Charset
import java.nio.file.Paths

class Interpreter {
    private interface CLICommand {
        fun execute(arguments: List<String>): List<String>
    }

    private object EchoCommand : CLICommand {
        override fun execute(arguments: List<String>): List<String> {
            return listOf(arguments.joinToString(" ") + "\n")
        }
    }

    private object CatCommand : CLICommand {
        override fun execute(arguments: List<String>): List<String> {
            val result = mutableListOf<String>()

            for (argument in arguments) {
                val file = File(argument)
                if (!file.exists()) {
                    throw IncorrectArgumentException("cat", argument, "No such file or directory")
                } else if (file.isDirectory) {
                    throw IncorrectArgumentException("cat", argument, "Is a directory")
                } else {
                    val reader = InputStreamReader(FileInputStream(file), Charset.defaultCharset())
                    val stringBuilder = StringBuilder()
                    val buffer = CharArray(500)
                    var char = reader.read(buffer)
                    while (char != -1) {
                        var charList = buffer.toList().filter { it != '\u0000' }
                        while (charList.isNotEmpty()) {
                            val newlinePosition = charList.indexOf('\n')
                            if (newlinePosition != -1) {
                                stringBuilder.append(charList.take(newlinePosition + 1).joinToString(""))
                                result.add(stringBuilder.toString())
                                stringBuilder.setLength(0)
                                charList = charList.drop(newlinePosition + 1)
                            } else {
                                stringBuilder.append(charList.joinToString(""))
                                break
                            }
                        }
                        buffer.fill('\u0000')
                        char = reader.read(buffer)
                    }
                    if (stringBuilder.isNotEmpty()) {
                        result.add(stringBuilder.toString())
                    }
                }
            }

            return result
        }
    }

    abstract class WcCommand : CLICommand {
        protected fun <T, K, V> tripleToString(triple: Triple<T, K, V>): String {
            return "${triple.first} ${triple.second} ${triple.third}"
        }
    }

    private object WcPipeCommand : WcCommand() {
        private fun calculateWc(args: List<String>): Triple<Long, Long, Long> {
            var lineCount: Long = 0
            args.forEach { lineCount += it.chars().filter{ ch -> ch.toChar() == '\n' }.count() }
            val wordCount = args.stream().flatMap { it.split(Regex("\\s"))
                    .filter { word -> word.isNotEmpty() }.stream() }.count()
            var byteCount: Long = 0
            args.forEach { byteCount += it.toByteArray().size }
            return Triple(lineCount, wordCount, byteCount)
        }

        override fun execute(arguments: List<String>): List<String> {
            return listOf(tripleToString(calculateWc(arguments)) + "\n")
        }
    }

    private object WcFileCommand : WcCommand() {
        private fun calculateWc(file: File): Triple<Long, Long, Long> {
            val byteCount = file.length()
            var lineCount: Long = 0
            var wordCount: Long = 0
            file.forEachLine {
                wordCount += it.split(" ").filter { line -> line.isNotEmpty() }.size
            }
            val fileInputStream = FileInputStream(file)
            while (fileInputStream.available() > 0) {
                val char = fileInputStream.read().toChar()
                if (char == '\n') {
                    lineCount += 1
                }
            }
            return Triple(lineCount, wordCount, byteCount)
        }

        override fun execute(arguments: List<String>): List<String> {
            var totalLineCount: Long = 0
            var totalWordCount: Long = 0
            var totalByteCount: Long = 0


            val res = mutableListOf<String>()
            for (argument in arguments) {
                val file = File(argument)
                if (!file.exists()) {
                    throw IncorrectArgumentException("wc", argument, "No such file or directory")
                } else if (file.isDirectory) {
                    throw IncorrectArgumentException("wc", argument, "Is a directory")
                } else {
                    val fileWc = calculateWc(file)
                    totalLineCount += fileWc.first
                    totalWordCount += fileWc.second
                    totalByteCount += fileWc.third
                    res.add(tripleToString(fileWc) + " ${file.name}\n")
                }
            }
            if (res.size > 1) {
                res.add(tripleToString(Triple(totalLineCount, totalWordCount, totalByteCount)) + " total\n")
            }
            return res
        }
    }

    private object GrepCommand : CLICommand {
        private const val generalRegexTemplate = "(?s).*%s.*"
        private const val caseInsensitiveRegexTemplate = "(?i)%s"
        private const val wholeWordRegexTemplate = "\\b%s\\b"

        private var caseInsensitive = false
        private var entireWord = false
        private var nLinesAfter = 0
        private lateinit var regexString: String

        private fun grep(regexString: String, lines: List<String>): List<String> {
            val result = mutableListOf<String>()
            val regexForUsage = if (caseInsensitive) {
                Regex(caseInsensitiveRegexTemplate.format(generalRegexTemplate.format(regexString)))
            } else {
                Regex(generalRegexTemplate.format(regexString))
            }
            lines.forEach { if (regexForUsage.matches(it)) result.add(it) }
            return result
        }

        private fun wholeWordGrep(regexString: String, lines: List<String>): List<String> {
            return grep(wholeWordRegexTemplate.format(regexString), lines)
        }

        private fun afterMatchGrep(regexString: String, lines: List<String>): List<String> {
            val resultingLines = LinkedHashSet<String>()
            val newRegexString = generalRegexTemplate
                    .format(if (entireWord) wholeWordRegexTemplate.format(regexString) else regexString)
            val regexForUsage = if (caseInsensitive) Regex(caseInsensitiveRegexTemplate.format(newRegexString))
                                      else Regex(newRegexString)
            for (i in 0 until lines.size) {
                if (regexForUsage.matches(lines[i])) {
                    resultingLines.add(lines[i])
                    for (j in 1..nLinesAfter) {
                        if (i + j >= lines.size) {
                            break
                        }
                        resultingLines.add(lines[i + j])
                    }
                }
            }
            return resultingLines.toList()
        }

        override fun execute(arguments: List<String>): List<String> {
            var result = emptyList<String>()
            try {
                if (nLinesAfter > 0) {
                    result = afterMatchGrep(regexString, arguments)
                } else {
                    result = grep(regexString, arguments)
                    if (entireWord) {
                        result = wholeWordGrep(regexString, result)
                    }
                }

            } catch (e: UninitializedPropertyAccessException) {}

            return result
        }

        fun setArguments(caseInsensitive: Boolean, entireWord: Boolean, nLinesAfter: Int, regexString: String) {
            this.caseInsensitive = caseInsensitive
            this.entireWord = entireWord
            this.nLinesAfter = nLinesAfter
            this.regexString = regexString
        }

        fun clearArguments() {
            caseInsensitive = false
            entireWord = false
            nLinesAfter = 0
        }
    }

    private object PwdCommand : CLICommand {
        override fun execute(arguments: List<String>): List<String> {
            return listOf(System.getProperty("user.dir"))
        }
    }

    private object CdCommand : CLICommand {
        override fun execute(arguments: List<String>): List<String> {
            val currentDirectory = System.getProperty("user.dir")
            val newDirectory = arguments.getOrElse(0) { "./" }

            val newCanonicalPath = if (Paths.get(newDirectory).isAbsolute) {
                File(newDirectory).canonicalFile.canonicalPath
            } else {
                File(currentDirectory, newDirectory).canonicalFile.canonicalPath
            }

            System.setProperty("user.dir", newCanonicalPath)

            return emptyList()
        }
    }

    private object LsCommand : CLICommand {
        override fun execute(arguments: List<String>): List<String> {
            val currentDirectory = System.getProperty("user.dir")
            val path = arguments.getOrElse(0) { "./" }

            val file = if (Paths.get(path).isAbsolute) {
                File(path).canonicalFile
            } else {
                File(currentDirectory, path).canonicalFile
            }

            val files = if (file.isDirectory) {
                file.listFiles()
            } else {
                arrayOf(file)
            }

            return files.map(File::getName)
        }
    }

    private object ExitCommand : CLICommand {
        override fun execute(arguments: List<String>): List<String> {
            System.exit(0)
            return emptyList()
        }
    }

    private object ExternalCommand: CLICommand {
        override fun execute(arguments: List<String>): List<String> {
            val processBuilder = ProcessBuilder(arguments)
            processBuilder.start()
            return emptyList()
        }
    }

    /**
     * This method executes the 'echo' command,
     * which outputs its arguments and adds a newline at the end.
     * @param args list of command arguments
     * @return a list with a single string which contains arguments joined by a space with a newline at the end
     */
    fun executeEcho(args: List<String>): List<String> {
        return EchoCommand.execute(args)
    }

    /**
     * This method executes the 'cat' command,
     * which outputs the content of the provided files.
     * @param filenames list of files
     * @return list of lines from provided files
     */
    fun executeCat(filenames: List<String>): List<String> {
        return CatCommand.execute(filenames)
    }

    /**
     * This method executes the 'wc' command for input received via pipe.
     * 'wc' outputs the number of lines, words, and bytes in the input.
     * @param args input lines
     * @return a line containing the number of lines, words, and bytes in the input with a newline attached
     */
    fun executePipeWc(args: List<String>): List<String> {
        return WcPipeCommand.execute(args)
    }

    /**
     * This method executed the 'wc' command for files.
     * 'wc' outputs the number of lines, words, and bytes in the input.
     * @param filenames list of files
     * @return a list of lines containing line, word, and byte count for each file and total count
     */
    fun executeFileWc(filenames: List<String>): List<String> {
        return WcFileCommand.execute(filenames)
    }

    /**
     * This method executes the 'grep' command for input received via pipe.
     * 'grep' outputs lines that contain the provided regular expressions.
     * @param regexString regex to match
     * @param lines input lines
     * @param caseInsensitive makes matching case insensitive
     * @param entireWord only match the exact whole word, no substrings
     * @param nLinesAfter output extra n lines after
     * @return list of lines that contain the regex
     */
    fun executePipeGrep(regexString: String, lines: List<String>,
                        caseInsensitive: Boolean = false, entireWord: Boolean = false,
                        nLinesAfter: Int = 0): List<String> {
        GrepCommand.setArguments(caseInsensitive, entireWord, nLinesAfter, regexString)
        val result = GrepCommand.execute(lines)
        GrepCommand.clearArguments()
        return result
    }

    /**
     * This method executes the 'grep' command for file input.
     * 'grep' outputs lines that contain the provided regular expressions.
     * @param regexString regex to match
     * @param filenames files for matching
     * @param caseInsensitive makes matching case insensitive
     * @param entireWord only match the exact whole word, no substrings
     * @param nLinesAfter output extra n lines after
     * @return list of lines that contain the regex
     */
    fun executeFileGrep(regexString: String, filenames: List<String>,
                        caseInsensitive: Boolean = false, entireWord: Boolean = false,
                        nLinesAfter: Int = 0): List<String> {
        val result = mutableListOf<String>()

        GrepCommand.setArguments(caseInsensitive, entireWord, nLinesAfter, regexString)
        for (filename in filenames) {
            val file = File(filename)
            if (!file.exists()) {
                throw IncorrectArgumentException("grep", filename, "No such file or directory")
            } else if (file.isDirectory) {
                throw IncorrectArgumentException("grep", filename, "Is a directory")
            } else {
                val fileResult = GrepCommand.execute(file.readLines())
                if (filenames.size > 1) {
                    fileResult.forEach { result.add("${file.name}:$it") }
                } else {
                    fileResult.forEach { result.add(it) }
                }
            }
        }

        GrepCommand.clearArguments()

        return result
    }

    /**
     * This method executes the 'pwd' command,
     * which outputs the working directory.
     * @return a list containing a single string which represents the current directory
     */
    fun executePwd(): List<String> {
        return PwdCommand.execute(emptyList())
    }

    /**
     * This method executes the 'cd' command,
     * which changes the working directory to a given path
     * or to the root if called without arguments.
     */
    fun executeCd(args: List<String>): List<String> {
        return CdCommand.execute(args)
    }

    /**
     * This method executes the 'ls' command,
     * which outputs content of the working directory.
     * @return a list containing names of files and directories in the current directory
     */
    fun executeLs(args: List<String>): List<String> {
        return LsCommand.execute(args)
    }

    /**
     * This method executed the 'exit' command,
     * which stops the interpreter.
     */
    fun executeExit(): List<String> {
        return ExitCommand.execute(emptyList())
    }

    /**
     * This method executes a command which isn't among the commands listed above.
     * @param externalCommand command and its arguments
     */
    fun executeExternalCommand(externalCommand: List<String>): List<String> {
        return ExternalCommand.execute(externalCommand)
    }
}