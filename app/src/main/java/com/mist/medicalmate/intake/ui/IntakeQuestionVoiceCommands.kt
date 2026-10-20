package com.mist.medicalmate.intake.ui

import com.mist.medicalmate.R
import com.mist.medicalmate.core.model.MAX_BRIEF_CARD_QUESTIONS
import com.mist.medicalmate.core.voice.VoiceReply
import com.mist.medicalmate.core.voice.VoiceText
import com.mist.medicalmate.core.voice.VoiceWords

/**
 * 추천 질문 고르기(1c-D)에서 말로 하는 일(#310). 묻는 말을 하면 질문으로 넣고, "두 번째 질문 담아"면 그 추천을
 * 담는다. 빼기·고치기는 "질문"이라는 말이 있을 때만이다. "다음"이면 정리를 마친다.
 *
 * [IntakeVoiceCommands]에서 떼어냈다. 한 클래스가 세 단계의 말을 모두 들면 함수가 열한 개를 넘는다.
 */
internal class IntakeQuestionVoiceCommands(
    private val state: () -> IntakeUiState,
    private val update: ((IntakeUiState) -> IntakeUiState) -> Unit,
) {
    /**
     * "질문 추가"만 말해 무엇을 물을지 물어 둔 참인지(#336). 그러면 다음 말이 묻는 꼴이 아니어도 그 질문이다 — 손으로
     * 빈 칸을 누른 것과 같다. 전에는 물어 놓고 다음 말이 묻는 꼴이 아니면 "못 알아들었어요"가 됐다.
     */
    private var awaitingQuestion = false

    fun handle(text: String, routes: IntakeVoiceRoutes): VoiceReply {
        val more = VoiceText(R.string.voice_intake_ask_more_questions)
        val waiting = awaitingQuestion
        awaitingQuestion = false
        val parsed = parseQuestionVoice(text)
        // 물어 둔 참이면 묻는 꼴이 아니어도 질문이다.
        val voice = if (waiting && parsed == null) QuestionVoice.Add(text.trim()) else parsed
        if (isNextVoice(text)) return finish(routes)
        return when (voice) {
            null -> VoiceReply.NotMine
            is QuestionVoice.Add -> addQuestion(voice.text, more)
            QuestionVoice.PickAll -> pickAll(more)
            is QuestionVoice.Pick -> pickQuestion(voice, more)
            is QuestionVoice.Edit -> editQuestion(voice, more)
            is QuestionVoice.Remove -> removeQuestion(voice, more)
        }
    }

    /**
     * "다음"으로 정리를 마친다.
     *
     * **담은 질문이 없으면 먼저 묻는다**(#320). 추천 질문은 체크하지 않은 채 서서, 말로 하던 사람이 "다음"만 하면
     * 질문 없는 카드가 됐다. 빈 칸에 적는 중이면 적던 것부터 보내게 한다 — 그때는 화면에 다음 버튼이 없다.
     */
    private fun finish(routes: IntakeVoiceRoutes): VoiceReply {
        val current = state()
        return when {
            current.writingQuestion -> VoiceReply.Done(R.string.voice_intake_writing_first)
            current.questions.isEmpty() && current.questionRows.isNotEmpty() ->
                VoiceReply.Confirm(R.string.voice_intake_confirm_no_questions) { routes.next() }

            else -> {
                routes.next()
                VoiceReply.Done(R.string.voice_intake_finished)
            }
        }
    }

    /** "다 담아 줘". 위에서부터 상한까지 담는다. */
    private fun pickAll(more: VoiceText): VoiceReply {
        update { current ->
            var room = MAX_BRIEF_CARD_QUESTIONS - current.questionRows.count { it.selected }
            current.copy(
                questionRows = current.questionRows.map { row ->
                    if (!row.selected && room > 0) {
                        room--
                        row.copy(selected = true)
                    } else {
                        row
                    }
                },
            ).focusing(IntakeFocusTarget.Questions)
        }
        return VoiceReply.Done(
            R.string.voice_intake_question_all,
            listOf(state().questions.size.toString()),
            question = more,
        )
    }

    /** 글이 비면 더하지 않고 무엇을 물을지 묻는다. 다 찼으면 그 사실을 알린다. 직접 적은 것이라 체크된 채 붙는다. */
    private fun addQuestion(text: String, more: VoiceText): VoiceReply = when {
        text.isEmpty() -> {
            awaitingQuestion = !state().questionsFull
            if (state().questionsFull) {
                VoiceReply.Done(R.string.voice_intake_question_full, listOf(MAX_BRIEF_CARD_QUESTIONS.toString()))
            } else {
                VoiceReply.Done(R.string.voice_intake_question_ask)
            }
        }
        state().questionsFull ->
            VoiceReply.Done(R.string.voice_intake_question_full, listOf(MAX_BRIEF_CARD_QUESTIONS.toString()))

        else -> {
            update {
                it.copy(questionRows = it.questionRows + IntakeQuestion(text, selected = true, custom = true))
                    .focusing(IntakeFocusTarget.Question(it.questionRows.size))
            }
            VoiceReply.Done(R.string.voice_intake_question_added, listOf(text), question = more)
        }
    }

    /** 몇 번째 줄을 담는다. 이미 담겨 있으면 그대로 알린다. 다 찼으면 담지 않는다. */
    private fun pickQuestion(voice: QuestionVoice.Pick, more: VoiceText): VoiceReply {
        val rows = state().questionRows
        val index = (
            if (voice.ordinal ==
                VoiceWords.LAST
            ) {
                rows.lastIndex
            } else {
                voice.ordinal
            }
            ).takeIf { it in rows.indices }
        return when {
            index == null -> VoiceReply.Done(R.string.voice_intake_question_which, question = more)
            !rows[index].selected && state().questionsFull ->
                VoiceReply.Done(R.string.voice_intake_question_full, listOf(MAX_BRIEF_CARD_QUESTIONS.toString()))

            else -> {
                update {
                    it.withRow(index) { row -> row.copy(selected = true) }.focusing(IntakeFocusTarget.Question(index))
                }
                VoiceReply.Done(R.string.voice_intake_question_added, listOf(rows[index].text), question = more)
            }
        }
    }

    /** 직접 적은 줄은 지우고 추천은 체크만 푼다 — 추천은 다시 담을 수 있게 남긴다. */
    private fun removeQuestion(voice: QuestionVoice.Remove, more: VoiceText): VoiceReply {
        val rows = state().questionRows
        val index = rows.map { it.text }.questionIndexByVoice(voice.ordinal, voice.words).singleOrNull()
            ?: return VoiceReply.Done(R.string.voice_intake_question_which, question = more)
        update { current ->
            val next =
                if (rows[index].custom) {
                    current.copy(questionRows = current.questionRows.filterIndexed { i, _ -> i != index })
                } else {
                    current.withRow(index) { it.copy(selected = false) }
                }
            next.focusing(IntakeFocusTarget.Questions)
        }
        return VoiceReply.Done(R.string.voice_intake_question_removed, listOf(rows[index].text), question = more)
    }

    /** 고칠 줄은 몇 번째로 말한다. 질문이 하나뿐이면 말하지 않아도 그 줄이다. 고친 줄은 직접 적은 것이 된다. */
    private fun editQuestion(voice: QuestionVoice.Edit, more: VoiceText): VoiceReply {
        val rows = state().questionRows
        val index =
            voice.ordinal?.let { if (it == VoiceWords.LAST) rows.lastIndex else it } ?: rows.indices.singleOrNull()
        return when {
            voice.text.isEmpty() -> VoiceReply.Done(R.string.voice_intake_question_ask)
            index == null || index !in rows.indices -> VoiceReply.Done(
                R.string.voice_intake_question_which,
                question = more,
            )
            else -> {
                update {
                    it.withRow(index) { row -> row.copy(text = voice.text, selected = true, custom = true) }
                        .focusing(IntakeFocusTarget.Question(index))
                }
                VoiceReply.Done(R.string.voice_intake_question_edited, listOf(voice.text), question = more)
            }
        }
    }
}

/** [index] 줄만 바꾼다. */
private fun IntakeUiState.withRow(index: Int, change: (IntakeQuestion) -> IntakeQuestion): IntakeUiState =
    copy(questionRows = questionRows.mapIndexed { i, row -> if (i == index) change(row) else row })
