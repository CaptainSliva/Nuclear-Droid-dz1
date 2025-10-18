
import com.github.demidko.aot.WordformMeaning.lookupForMeanings
import java.io.File
import java.nio.file.Paths

val listOfCommonWords = mapOf<String, MutableList<String>>(
    "Срок использования" to mutableListOf(),
    "Достоинства" to mutableListOf(),
    "Недостатки" to mutableListOf(),
    "Комментарий" to mutableListOf()
)
val listOfUnCommonWords = mapOf<String, MutableList<String>>(
    "Срок использования" to mutableListOf(),
    "Достоинства" to mutableListOf(),
    "Недостатки" to mutableListOf(),
    "Комментарий" to mutableListOf()
)

lateinit var currentSection: Sections
val countOfCommonWords = mutableMapOf<String, MutableSet<Recall>>(
    "Срок использования" to mutableSetOf(),
    "Достоинства" to mutableSetOf(),
    "Недостатки" to mutableSetOf(),
    "Комментарий" to mutableSetOf()
)
val countOfUnCommonWords = mutableMapOf<String, MutableSet<Recall>>(
    "Срок использования" to mutableSetOf(),
    "Достоинства" to mutableSetOf(),
    "Недостатки" to mutableSetOf(),
    "Комментарий" to mutableSetOf()
)

val inputFile = File("recalls.txt")
var fileContent = listOf<String>()

val outputCommonFile = File("analizeCommonWords.txt")
val outputUnCommonFile = File("analizeUnCommonWords.txt")
val regex = Regex("[a-zA-Zа-яА-ЯёЁ0-9]+")
fun main(args: Array<String>) {
    if (inputFile.exists()) {
        fileContent = inputFile.bufferedReader().readLines()
        println("Созданы файлы \"analizeCommonWords.txt\" и \"analizeUnCommonWords.txt\"")
    }
    else {
        println("Не найден файл recalls.txt")
        return
    }

    fileContent.forEachIndexed { index, trashLine ->
        val currentString = mutableListOf<String>()
        val line = trashLine.replace("    Срок использования: ", "Срокиспользования:")
        if (line != "") {
            currentString.addAll(line.split(" "))
            currentString.forEach{
                var cleanString = regex.find(it)?.value?:""

                if (index != fileContent.size-2) {
                    when {
                        "Срокиспользования:" in line -> {
                            currentSection = Sections.USE_TIME
                            cleanString = cleanString.replace("Срокиспользования", "")
                        }
                        it == "Достоинства" && fileContent[index + 1] == "" -> {
                            currentSection = Sections.DIGNITYES
                            return@forEach
                        }
                        it == "Недостатки" && fileContent[index + 1] == "" -> {
                            currentSection = Sections.DEFECTS
                            return@forEach
                        }
                        it == "Комментарий" && fileContent[index + 1] == "" -> {
                            currentSection = Sections.COMMENT
                            return@forEach
                        }
                    }
                }
                var morphString = ""
                var tryFlag = true

                if (cleanString != "") {
                    try {
                        morphString = lookupForMeanings(cleanString)[0].lemma.toString()
                    }catch (e: Exception) {
                        morphString = cleanString
                        tryFlag = false
                    }

                    when (tryFlag) {
                        true -> {
                            when (currentSection) {
                                Sections.USE_TIME -> listOfCommonWords["Срок использования"]?.add(morphString)
                                Sections.DIGNITYES -> listOfCommonWords["Достоинства"]?.add(morphString)
                                Sections.DEFECTS -> listOfCommonWords["Недостатки"]?.add(morphString)
                                Sections.COMMENT -> listOfCommonWords["Комментарий"]?.add(morphString)
                            }
                        }
                        false -> {
                            when (currentSection) {
                                Sections.USE_TIME -> listOfUnCommonWords["Срок использования"]?.add(morphString)
                                Sections.DIGNITYES -> listOfUnCommonWords["Достоинства"]?.add(morphString)
                                Sections.DEFECTS -> listOfUnCommonWords["Недостатки"]?.add(morphString)
                                Sections.COMMENT -> listOfUnCommonWords["Комментарий"]?.add(morphString)
                            }
                        }
                    }
                }

            }
        }
    }

    listOfCommonWords.forEach { (key, value) ->
        value.forEach { word ->
            countOfCommonWords[key]?.add(Recall(word, value.count{it == word}))
        }
    }

    sortWords(countOfCommonWords)

    writeFile(countOfCommonWords, outputCommonFile)

    //---------------------------UNCOMMON---------------------------

    listOfUnCommonWords.forEach { (key, value) ->
        value.forEach { word ->
            countOfUnCommonWords[key]?.add(Recall(word, value.count{it == word}))
        }
    }

    sortWords(countOfUnCommonWords)

    writeFile(countOfUnCommonWords, outputUnCommonFile)

}

private fun sortWords(countOfWords: MutableMap<String, MutableSet<Recall>>) {
    countOfWords.forEach { key, valuesList ->
        val maxValue = 0
        valuesList.forEachIndexed { index, value ->
            if (value.count > maxValue) {
                countOfWords[key] = valuesList.sortedBy { it.count }.toSet() as MutableSet<Recall>
            }
        }
    }
}

private fun writeFile(countOfWords: MutableMap<String, MutableSet<Recall>>, file: File) {
    file.printWriter().use { out ->
        countOfWords.forEach { key, valuesList ->
            out.println("\n"+key)
            out.println("[")
            valuesList.forEach { value ->
                out.println(value)
            }
            out.println("]")
        }
    }
}