package ru.hse.spb.sd.sharkova.interpreter

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.IOException

class InterpreterTest {
    private val file1Lines = listOf("text\n", "line\n", "\n", "the previous line was an empty one\n")
    private val file2Lines = listOf("So long as there shall exist, by virtue of law and custom, " +
            "decrees of damnation pronounced by society, artificially creating hells amid the civilization of earth, " +
            "and adding the element of human fate to divine destiny; " +
            "so long as the three great problems of the century—the degradation of man through pauperism, " +
            "the corruption of woman through hunger, the crippling of children through lack of light—are unsolved; " +
            "so long as social asphyxia is possible in any part of the world;—in other words, " +
            "and with a still wider significance, so long as ignorance and poverty exist on earth, " +
            "books of the nature of Les Misérables cannot fail to be of use.")
    private val file1Wc = "4 9 46"
    private val file2Wc = "0 109 651"
    
    private val parser = CLIParser()

    @Test
    fun testEcho() {
        val res = parser.parseInput("echo text")
        assertEquals(listOf("text\n"), res)
    }

    @Test
    fun testEchoMultipleArguments() {
        val res = parser.parseInput("echo some more text")
        assertEquals(listOf("some more text\n"), res)
    }

    @Test
    fun testEchoWithSameNameArguments() {
        val res = parser.parseInput("echo echo echo echo")
        assertEquals(listOf("echo echo echo\n"), res)
    }

    @Test
    fun testCat() {
        val res = parser.parseInput("cat src/test/resources/file1.txt")
        assertEquals(file1Lines, res)
    }

    @Test
    fun testCatMultipleArguments() {
        val res = parser.parseInput("cat src/test/resources/file1.txt src/test/resources/file2.txt")
        val expected = mutableListOf<String>()
        expected.addAll(file1Lines)
        expected.addAll(file2Lines)

        assertEquals(expected, res)
    }

    @Test
    fun testOneFileWc() {
        val res1 = parser.parseInput("wc src/test/resources/file1.txt")
        assertEquals(listOf("$file1Wc file1.txt\n"), res1)
        val res2 = parser.parseInput("wc src/test/resources/file2.txt")
        assertEquals(listOf("$file2Wc file2.txt\n"), res2)
    }

    @Test
    fun testMultipleFilesWc() {
        val res = parser.parseInput("wc src/test/resources/file1.txt src/test/resources/file2.txt")
        assertEquals(listOf("$file1Wc file1.txt\n", "$file2Wc file2.txt\n", "4 118 697 total\n"), res)
    }

    @Test
    fun testPwd() {
        val res = parser.parseInput("pwd")
        val expected = "pwd".runCommand()
        assertEquals(listOf(expected), res)
    }

    @Test
    fun testCdNoArguments() {
        val expectedOutput = emptyList<String>()
        val expectedPath = System.getProperty("user.dir")
        val res = parser.parseInput("cd")
        assertEquals(expectedOutput, res)
        assertEquals(expectedPath, System.getProperty("user.dir"))
    }

    @Test
    fun testCdSubdirectory() {
        val expectedOutput = emptyList<String>()
        val expectedPath = System.getProperty("user.dir") + File.separator + "src"
        val res = parser.parseInput("cd src")
        assertEquals(expectedOutput, res)
        assertEquals(expectedPath, System.getProperty("user.dir"))
    }

    @Test
    fun testLsNoArguments() {
        val root = System.getProperty("user.dir")
        System.setProperty("user.dir", root + File.separator + "src")
        val expectedOutput = listOf("main", "test")
        val res = parser.parseInput("ls")
        assertEquals(expectedOutput, res)
    }

    @Test
    fun testLsSubdirectory() {
        val expectedOutput = listOf("main", "test")
        val res = parser.parseInput("ls src")
        assertEquals(expectedOutput, res)
    }

    // run this one separately because it literally exits
    /*@Test
    fun testExit() {
        parser.parseInput("exit")
        // if we haven't exited the interpreter, the test will fail
        fail()
    }*/

    @Test
    fun testPipeEchoCat() {
        val res = parser.parseInput("echo echo echo echo|cat src/test/resources/file1.txt")
        assertEquals(file1Lines, res)
    }

    @Test
    fun testPipeCatEcho() {
        val res = parser.parseInput("cat src/test/resources/file1.txt | echo echo echo echo")
        assertEquals(listOf("echo echo echo\n"), res)
    }

    @Test
    fun testPipeCatWc() {
        val res = parser.parseInput("cat src/test/resources/file1.txt|wc")
        assertEquals(listOf(file1Wc + "\n"), res)
    }

    @Test
    fun testPipeCatMultipleFilesWc() {
        val res = parser.parseInput("cat src/test/resources/file1.txt src/test/resources/file2.txt   | wc")
        assertEquals(listOf("4 118 697\n"), res)
    }

    @Test
    fun testPipeEchoWc() {
        val res = parser.parseInput("echo echo echo | wc")
        assertEquals(listOf("1 2 10\n"), res)
    }

    @Test
    fun testPipeWcEcho() {
        val res = parser.parseInput("wc src/test/resources/file1.txt | echo echo echo")
        assertEquals(listOf("echo echo\n"), res)
    }

    @Test
    fun testPipeDoubleWc() {
        val res = parser.parseInput("wc src/test/resources/file1.txt | wc")
        assertEquals(listOf("1 4 17\n"), res)
    }

    @Test
    fun testCatNonexistentFile() {
        val res = parser.parseInput("cat file1.txt")
        assertEquals(listOf("cat: file1.txt: No such file or directory"), res)
    }

    @Test
    fun testCatDirectory() {
        val res = parser.parseInput("cat src/test/resources")
        assertEquals(listOf("cat: src/test/resources: Is a directory"), res)
    }

    @Test
    fun testWcNonexistentFile() {
        val res = parser.parseInput("wc file1.txt")
        assertEquals(listOf("wc: file1.txt: No such file or directory"), res)
    }

    @Test
    fun testWcDirectory() {
        val res = parser.parseInput("wc src/test")
        assertEquals(listOf("wc: src/test: Is a directory"), res)
    }

    @Test
    fun testWcNonexistentFileInPipe() {
        val res = parser.parseInput("echo some text | wc src/file1.txt | echo more text")
        assertEquals(listOf("more text\n", "wc: src/file1.txt: No such file or directory"), res)
    }

    @Test
    fun testVariableSubstitutionEcho() {
        parser.parseInput("x=texttext")
        val res = parser.parseInput("echo \$x")
        assertEquals(listOf("texttext\n"), res)
    }

    @Test
    fun testMultipleVariableSubstitutionEcho() {
        parser.parseInput("x=texttext")
        parser.parseInput("y=x")
        val res1 = parser.parseInput("echo \$y")
        assertEquals(listOf("x\n"), res1)
        parser.parseInput("y=\$x")
        val res2 = parser.parseInput("echo \$y")
        assertEquals(listOf("texttext\n"), res2)
    }

    @Test
    fun testSubstituteFilenameCat() {
        val res = parser.parseInput("x=src/test/resources/file1.txt | cat \$x")
        assertEquals(file1Lines, res)
    }

    @Test
    fun testSubstitutePwd() {
        val res = parser.parseInput("x=pwd \$x")
        val expected = "pwd".runCommand()
        assertEquals(listOf(expected), res)
    }

    @Test
    fun testNoIdentifierDollarSignEcho() {
        val res = parser.parseInput("echo \$не_идентификатор")
        assertEquals(listOf("\$не_идентификатор\n"), res)
    }

    @Test
    fun testDoubleQuotesEcho() {
        val res = parser.parseInput("echo \"text with spaces\"")
        assertEquals(listOf("text with spaces\n"), res)
    }

    @Test
    fun testMultipleDoubleQuotesEcho() {
        val res = parser.parseInput("echo \"\"text with spaces\"\" and more \"and even more\"")
        assertEquals(listOf("text with spaces and more and even more\n"), res)
    }

    @Test (expected = MismatchedQuotesException::class)
    fun testMismatchedDoubleQuotesEcho() {
        parser.parseInput("echo \"\"x\"")
    }

    @Test
    fun testDoubleQuotesWithSubstitutionEcho() {
        parser.parseInput("x=text")
        val res1 = parser.parseInput("echo \"\$x\"")
        assertEquals(listOf("text\n"), res1)
        val res2 = parser.parseInput("echo \"$\"x\"\"")
        assertEquals(listOf("\$ x\n"), res2)
    }

    @Test
    fun testSingleQuotes() {
        val res = parser.parseInput("echo \'text\'")
        assertEquals(listOf("text\n"), res)
    }

    @Test
    fun testNestedQuotes() {
        val res1 = parser.parseInput("echo \'\"text\"\'")
        assertEquals(listOf("text\n"), res1)
        val res2 = parser.parseInput("echo \"\'text\'\"")
        assertEquals(listOf("text\n"), res2)
    }

    @Test
    fun testSingleQuotesWithSubstitutionEcho() {
        parser.parseInput("x=text")
        val res = parser.parseInput("echo \'\$x\'")
        assertEquals(listOf("\$x\n"), res)
    }

    @Test
    fun testQuotesAssignment() {
        parser.parseInput("x=\"text with spaces\"")
        val res = parser.parseInput("echo \$x")
        assertEquals(listOf("text with spaces\n"), res)
    }


    @Test
    fun testGrepSingleFile() {
        val res = parser.parseInput("grep word src/test/resources/grep1.txt")
        assertEquals(listOf("this is a word", "more words your way!"), res)
    }

    @Test
    fun testGrepMultipleFiles() {
        val res = parser.parseInput("grep word src/test/resources/grep1.txt src/test/resources/grep2.txt")
        assertEquals(listOf("grep1.txt:this is a word", "grep1.txt:more words your way!", "grep2.txt:and this word"), res)
    }

    @Test
    fun testGrepCaseInsensitive() {
        val res = parser.parseInput("grep -i word src/test/resources/grep1.txt")
        assertEquals(listOf("this is a word", "This is a Word.",
                "These are some sad, sad WORDS!!!", "more words your way!"), res)
    }

    @Test
    fun testGrepWholeWords() {
        val res = parser.parseInput("grep -w word src/test/resources/grep1.txt")
        assertEquals(listOf("this is a word"), res)
    }

    @Test
    fun testNLinesAfter() {
        val res = parser.parseInput("grep -A 2 word src/test/resources/grep1.txt")
        assertEquals(listOf("this is a word", "This is a Word.",
                "this is a sad,  sad world", "more words your way!"), res)
    }

    @Test
    fun testPipeGrep() {
        val res = parser.parseInput("cat src/test/resources/grep1.txt | grep word")
        assertEquals(listOf("this is a word\n", "more words your way!"), res)
    }

    @Test
    fun testGrepCaseInsensitiveWholeWord() {
        val res1 = parser.parseInput("grep -w -i word src/test/resources/grep1.txt")
        assertEquals(listOf("this is a word", "This is a Word."), res1)
        // arguments input order doesn't matter
        val res2 = parser.parseInput("grep -i -w word src/test/resources/grep1.txt")
        assertEquals(listOf("this is a word", "This is a Word."), res2)
    }

    @Test
    fun testGrepCaseInsensitiveNLinesAfter() {
        val res = parser.parseInput("grep -i -A 2 this src/test/resources/grep1.txt")
        assertEquals(listOf("this is a word", "This is a Word.", "this is a sad,  sad world",
                "These are some sad, sad WORDS!!!", "43 49825 34 0965 26 48 23.4 89,9"), res )
    }

    @Test
    fun testGrepWholeWordNLinesAfter() {
        val res = parser.parseInput("grep -w -A 2 word src/test/resources/grep1.txt")
        assertEquals(listOf("this is a word", "This is a Word.", "this is a sad,  sad world"), res)
    }

    @Test
    fun testGrepCaseInsensitiveWholeWordNLinesAfter() {
        val res = parser
                .parseInput("grep -i -w -A 1 word src/test/resources/grep1.txt src/test/resources/grep2.txt")
        assertEquals(listOf("grep1.txt:this is a word", "grep1.txt:This is a Word.",
                "grep1.txt:this is a sad,  sad world", "grep2.txt:and this word",
                "grep2.txt:in this text with     spaces"), res)
    }

    @Test
    fun testGrepDoubleQuotes() {
        val res = parser.parseInput("grep \"is a\" src/test/resources/grep1.txt")
        assertEquals(listOf("this is a word", "This is a Word.", "this is a sad,  sad world"), res)
    }

    @Test
    fun testGrepRegularExpression1() {
        val res = parser.parseInput("grep \"sad,[ ]*sad\" src/test/resources/grep1.txt")
        assertEquals(listOf("this is a sad,  sad world", "These are some sad, sad WORDS!!!"), res)
    }

    @Test
    fun testGrepRegularExpression2() {
        val res = parser.parseInput("grep \"wor.*d\" src/test/resources/grep1.txt")
        assertEquals(listOf("this is a word", "this is a sad,  sad world", "more words your way!"), res)
    }

    @Test
    fun testGrepRegularExpressionCaseInsensitive() {
        val res = parser.parseInput("grep -i \"wor.*d\" src/test/resources/grep1.txt")
        assertEquals(listOf("this is a word", "This is a Word.", "this is a sad,  sad world",
                "These are some sad, sad WORDS!!!", "more words your way!"), res)
    }

    @Test
    fun testGrepArgumentsOrder() {
        val res1 = parser.parseInput("grep -i word src/test/resources/grep1.txt")
        val res2 = parser.parseInput("grep word -i src/test/resources/grep1.txt")
        val res3 = parser.parseInput("grep word src/test/resources/grep1.txt -i")
        assertEquals(res1, res2)
        assertEquals(res2, res3)
    }

    @Test(expected = NotEnoughArgumentsException::class)
    fun testGrepNoRegexProvided() {
        parser.parseInput("grep src/test/resources/grep1.txt")
    }

    @Test
    fun testDoubleGrep() {
        val res = parser.parseInput("grep -i word src/test/resources/grep1.txt | grep this")
        assertEquals(listOf("this is a word"), res)
    }

    @Test
    fun testGrepNonexistentFile() {
        val res = parser.parseInput("grep -i word grep1.txt")
        assertEquals(listOf("grep: grep1.txt: No such file or directory"), res)
    }


    // method taken from: https://stackoverflow.com/a/41495542/7735110
    private fun String.runCommand(): String {
        try {
            val parts = this.split("\\s".toRegex())
            val process = ProcessBuilder(*parts.toTypedArray())
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .start()
            return process.inputStream.bufferedReader().readLine()
        } catch (e: IOException) {
            e.printStackTrace()
            fail()
        }

        return ""
    }
}