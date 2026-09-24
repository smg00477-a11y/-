package com.example.ai.prompts

import com.example.ai.model.RetrievedChunk

object AiPromptRepository {

    const val SYSTEM_ANTI_HALLUCINATION_PROMPT = """
أنت «رقيم AI»، المساعد المعرفي والقرائي الذكي للكتب والمخطوطات العربية، تعمل بالكامل محلياً وبدون إنترنت على هاتف المستخدم.

قواعد الاستدلال الصارمة (Anti-Hallucination Rules):
1. أجب حصرياً ومباشرةً بالاعتماد على المقاطع المسترجعة المرفقة من الكتاب.
2. لا تخترع أو تلفق أو تفترض أي وقائع، أحداث، أسماء، تواريخ، اقتباسات، أو أرقام صفحات غير موجودة في النص المرفق.
3. إذا لم تجد في النص المرفق ما يجيب بدقة عن سؤال المستخدم، أجب بوضوح تام ودون حرج:
   "لا أجد في النص المتاح ما يكفي للإجابة عن هذا السؤال."
4. اذكر دائماً مصادر المعلومات المعتمد عليها (رقم الفصل والصفحات).
5. ميز بوضوح بين النص المنقول بدقة كشاهد وبين التلخيص أو الاستنتاج.
"""

    fun buildQaPrompt(question: String, chunks: List<RetrievedChunk>, bookTitle: String): String {
        val contextBuilder = StringBuilder()
        contextBuilder.append("الكتاب: ").append(bookTitle).append("\n\n")
        contextBuilder.append("المقاطع المسترجعة من الكتاب:\n")

        chunks.forEachIndexed { index, chunk ->
            contextBuilder.append("--- مقطع ").append(index + 1).append(" ---\n")
            if (!chunk.chapterTitle.isNullOrBlank()) {
                contextBuilder.append("الفصل: ").append(chunk.chapterTitle).append("\n")
            }
            contextBuilder.append("الصفحة الرقمية: ").append(chunk.physicalPageNumber)
            if (!chunk.printedPageNumber.isNullOrBlank()) {
                contextBuilder.append(" (الصفحة المطبوعة: ").append(chunk.printedPageNumber).append(")")
            }
            contextBuilder.append("\nالنص:\n").append(chunk.text.trim()).append("\n\n")
        }

        contextBuilder.append("سؤال المستخدم:\n").append(question).append("\n\n")
        contextBuilder.append("المطلوب: أجب عن السؤال بإيجاز ودقة بالاعتماد فقط على المقاطع أعلاه مع ذكر أرقام الصفحات المستند إليها.")
        return contextBuilder.toString()
    }

    fun buildPageSummaryPrompt(pageText: String, pageIndex: Int): String {
        return """
قم بتلخيص هذه الصفحة (الصفحة ${pageIndex + 1}) تلخيصاً مركزاً في فقرة واحدة أو فقرتين، ثم استخرج 2 إلى 3 نقاط رئيسية. اعتمد حصراً على النص التالي:

النص:
$pageText
""".trimIndent()
    }

    fun buildChapterSummaryPrompt(chapterTitle: String, combinedText: String): String {
        return """
قم بإعداد ملخص منظم وشامل للفصل بعنوان «$chapterTitle»، يتضمن:
1. الفكرة العامة للفصل.
2. الأفكار والمحاور الرئيسية كنقاط موجزة.
3. أهم المصطلحات والمفاهيم الواردة.

النص المستخرج من الفصل:
$combinedText
""".trimIndent()
    }

    fun buildBookSummaryPrompt(bookTitle: String, chapterSummaries: List<String>): String {
        val summariesText = chapterSummaries.joinToString("\n\n")
        return """
قم بإعداد ملخص شامل ومركّز لكتاب «$bookTitle»، مستنداً إلى ملخصات فصوله:

$summariesText

المطلوب:
- تقديم موجز عن موضوع الكتاب وهدفه العام.
- أهم المحاور والنتائج المستخلصة.
""".trimIndent()
    }
}
