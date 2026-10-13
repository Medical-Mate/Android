package com.mist.medicalmate.intake.ui

import com.mist.medicalmate.R
import com.mist.medicalmate.core.designsystem.MedicalMateSeverity
import com.mist.medicalmate.core.model.IntakeStep
import com.mist.medicalmate.core.model.MAX_BRIEF_CARD_QUESTIONS
import com.mist.medicalmate.core.voice.VoiceReply
import com.mist.medicalmate.core.voice.VoiceText
import com.mist.medicalmate.core.voice.VoiceWords

/**
 * 증상 정리에서 말로 하는 일 가운데 화면이 넘겨주는 것.
 *
 * @param next 다음 단계. 강도의 표시 문구가 리소스라 화면이 풀어서 넘긴다.
 * @param severityLabel 강도의 표시 문구. 막대에 "많이 아파요, 골랐어요"라고 적는다.
 */
internal data class IntakeVoiceRoutes(
    val next: () -> Unit = {},
    val severityLabel: (MedicalMateSeverity) -> String = { it.name },
)

/**
 * 증상 정리의 말을 화면 동작으로(#284). 네 단계가 한 목적지라 단계마다 갈라 읽는다.
 *
 * - 1단계(1l): 부위 이름을 말하면 고른다. 큰 부위만 말하면 그 부위로 들어가 어디인지 묻고, 좌우가
 *   갈리면 어느 쪽인지 묻는다.
 * - 2단계(1c): **막대가 문답을 맡는다.** 말한 것은 모두 답으로 보낸다. "다음"과 "뒤로"만 명령이다.
 *   AI의 다음 질문은 화면이 막대에 건다.
 * - 3단계(1d): "많이 아파요", "7점", "4단계"로 강도를 고른다.
 * - 4단계(1i): 묻는 말을 하면 질문으로 넣는다. 지우기·고치기는 "질문"이라는 말이 있을 때만이다.
 *
 * 버튼과 같은 일을 한다. 고른 부위·보낸 답·강도·질문은 버튼으로 한 것과 같은 상태다.
 */
internal class IntakeVoiceCommands(
    private val state: () -> IntakeUiState,
    private val update: ((IntakeUiState) -> IntakeUiState) -> Unit,
    private val send: (String) -> Unit,
    private val back: () -> Unit,
) {
    /** 좌우를 물어 둔 부위. 다음 말이 "오른쪽"이면 그중에서 고른다. */
    private var askingSide: List<BodyMapSelection> = emptyList()

    fun handle(text: String, routes: IntakeVoiceRoutes): VoiceReply = when {
        isBackVoice(text) -> goBack()
        else ->
            when (state().step) {
                IntakeStep.BODY_PART -> body(text, routes)
                IntakeStep.SYMPTOM_CHAT -> chat(text, routes)
                IntakeStep.SEVERITY -> severity(text, routes)
                IntakeStep.QUESTIONS -> questions(text, routes)
            }
    }

    /** 앞 단계로. 첫 화면에서는 전역 "뒤로"가 증상 정리를 나간다. */
    private fun goBack(): VoiceReply {
        if (!state().canGoBack) return VoiceReply.NotMine
        back()
        return VoiceReply.Done(R.string.voice_intake_back, question = state().voiceQuestion())
    }

    private fun body(text: String, routes: IntakeVoiceRoutes): VoiceReply {
        val current = state()
        return when {
            isNextVoice(text) && current.canLeaveBodyPart -> {
                routes.next()
                VoiceReply.Done(R.string.voice_intake_chat_start)
            }

            isNextVoice(text) -> VoiceReply.Done(R.string.voice_intake_part_first, question = current.voiceQuestion())
            isResetVoice(text) -> {
                askingSide = emptyList()
                update { it.copy(bodyMap = it.bodyMap.copy(focus = null, selection = null)) }
                VoiceReply.Done(R.string.voice_intake_part_reset, question = state().voiceQuestion())
            }

            // 부위 이름을 몰라 말하지 못하는 사람에게 고를 수 있는 곳을 다시 읽어 준다.
            isUnsureVoice(text) -> VoiceReply.Done(R.string.voice_intake_part_list, question = current.voiceQuestion())

            else -> pick(text, current.bodyMap.focus)
        }
    }

    /** 말에서 부위를 찾는다. 좌우를 물어 둔 참이면 "오른쪽"만으로도 고른다. */
    private fun pick(text: String, focus: BodyMapSelection?): VoiceReply {
        val side = sideOf(text)
        val found = pickBodyPartByVoice(text, focus)
        val pending = askingSide.firstOrNull { it.side == side }
        return when {
            found is BodyVoicePick.Picked -> picked(found.selection)
            found is BodyVoicePick.Focus -> {
                askingSide = emptyList()
                update { it.copy(bodyMap = it.bodyMap.copy(focus = found.anchor)) }
                VoiceReply.Done(
                    R.string.voice_intake_part_focus,
                    listOf(found.anchor.title()),
                    question = state().voiceQuestion(),
                )
            }

            found is BodyVoicePick.AskSide -> {
                askingSide = found.choices
                val choice = found.choices.first()
                VoiceReply.Done(
                    R.string.voice_intake_part_side,
                    listOf(bodyMapLabelOf(choice.zoneId ?: choice.anchorId)),
                    question = VoiceText(R.string.voice_intake_ask_side),
                )
            }

            pending != null && pending.zoneId == null ->
                // 팔·다리처럼 큰 부위의 좌우였다. 그 쪽으로 들어가 어디인지 묻는다.
                pick(bodyMapLabelOf(pending.anchorId), pending)

            pending != null -> picked(pending)
            else -> VoiceReply.NotMine
        }
    }

    /**
     * 부위를 골랐다. 그 부위가 속한 큰 부위로 들어가 고른 자리가 보이게 한다. 손으로 짚을 때와 같은
     * 화면이다.
     */
    private fun picked(selection: BodyMapSelection): VoiceReply {
        askingSide = emptyList()
        val mirrored = bodyMapAnchorOf(selection.anchorId).detail?.mirrored == true
        val focus =
            selection.takeIf { it.zoneId != null }?.let {
                BodyMapSelection(it.anchorId, side = if (mirrored) it.side else BodyMapSide.CENTER)
            }
        update { it.copy(bodyMap = it.bodyMap.copy(focus = focus, selection = selection)) }
        return VoiceReply.Done(
            R.string.voice_intake_picked,
            listOf(selection.title()),
            question = VoiceText(R.string.voice_intake_ask_next_part),
        )
    }

    /**
     * 문답. 말한 것은 답이다. 문답이 끝났으면 "다음"으로 넘어가고, 끝나기 전의 "다음"은 아직 묻는
     * 중이라고 알린다. 앞에서 보낸 답에 AI가 아직 답하지 않았으면 기다리게 한다.
     */
    private fun chat(text: String, routes: IntakeVoiceRoutes): VoiceReply {
        val current = state()
        val next = isNextVoice(text) && VoiceWords.compact(text).startsWith(NEXT_WORD)
        return when {
            current.chatFinished && isNextVoice(text) -> {
                routes.next()
                VoiceReply.Done(R.string.voice_intake_moved_on, question = state().voiceQuestion())
            }

            current.chatFinished ->
                VoiceReply.Done(
                    R.string.voice_intake_chat_over,
                    question = VoiceText(R.string.voice_intake_ask_chat_next),
                )

            next -> VoiceReply.Done(
                R.string.voice_intake_still_asking,
                question = VoiceText(R.string.voice_intake_ask_answer),
            )
            current.awaitingReply -> VoiceReply.Done(R.string.voice_intake_waiting)
            current.sessionId == null -> VoiceReply.Done(R.string.voice_intake_no_session)
            else -> {
                send(text.trim())
                VoiceReply.Done(R.string.voice_intake_sent, question = VoiceText(R.string.voice_intake_wait_reply))
            }
        }
    }

    private fun severity(text: String, routes: IntakeVoiceRoutes): VoiceReply {
        val severity = parseSeverityVoice(text)
        return when {
            isNextVoice(text) -> {
                routes.next()
                VoiceReply.Done(R.string.voice_intake_moved_on, question = state().voiceQuestion())
            }

            severity == null -> VoiceReply.NotMine
            else -> {
                update { it.copy(severity = severity) }
                VoiceReply.Done(
                    R.string.voice_intake_picked,
                    listOf(routes.severityLabel(severity)),
                    question = VoiceText(R.string.voice_intake_ask_next_severity),
                )
            }
        }
    }

    private fun questions(text: String, routes: IntakeVoiceRoutes): VoiceReply {
        val more = VoiceText(R.string.voice_intake_ask_more_questions)
        if (isNextVoice(text)) {
            routes.next()
            return VoiceReply.Done(R.string.voice_intake_finished)
        }
        return when (val voice = parseQuestionVoice(text)) {
            null -> VoiceReply.NotMine
            is QuestionVoice.Add -> addQuestion(voice.text, more)
            is QuestionVoice.Edit -> editQuestion(voice, more)
            is QuestionVoice.Remove -> {
                val list = state().questions
                val index = list.questionIndexByVoice(voice.ordinal, voice.words).singleOrNull()
                if (index == null) {
                    VoiceReply.Done(R.string.voice_intake_question_which, question = more)
                } else {
                    update { it.copy(questions = it.questions.filterIndexed { i, _ -> i != index }) }
                    VoiceReply.Done(R.string.voice_intake_question_removed, listOf(list[index]), question = more)
                }
            }
        }
    }

    /** 글이 비면 더하지 않고 무엇을 물을지 묻는다. 다 찼으면 그 사실을 알린다. */
    private fun addQuestion(text: String, more: VoiceText): VoiceReply = when {
        text.isEmpty() -> VoiceReply.Done(R.string.voice_intake_question_ask)
        state().questionsFull ->
            VoiceReply.Done(R.string.voice_intake_question_full, listOf(MAX_BRIEF_CARD_QUESTIONS.toString()))

        else -> {
            update { it.copy(questions = it.questions + text) }
            VoiceReply.Done(R.string.voice_intake_question_added, listOf(text), question = more)
        }
    }

    /** 고칠 줄은 몇 번째로 말한다. 질문이 하나뿐이면 말하지 않아도 그 줄이다. */
    private fun editQuestion(voice: QuestionVoice.Edit, more: VoiceText): VoiceReply {
        val list = state().questions
        val index =
            voice.ordinal?.let { if (it == VoiceWords.LAST) list.lastIndex else it } ?: list.indices.singleOrNull()
        return when {
            voice.text.isEmpty() -> VoiceReply.Done(R.string.voice_intake_question_ask)
            index == null || index !in list.indices -> VoiceReply.Done(
                R.string.voice_intake_question_which,
                question = more,
            )
            else -> {
                update { it.copy(questions = it.questions.mapIndexed { i, q -> if (i == index) voice.text else q }) }
                VoiceReply.Done(R.string.voice_intake_question_edited, listOf(voice.text), question = more)
            }
        }
    }
}

/**
 * 이 단계에서 말로 할 일을 묻는 말. 단계에 들어설 때 막대에 건다. 문답은 AI의 질문이 그 자리라
 * 따로 묻지 않는다.
 */
internal fun IntakeUiState.voiceQuestion(): VoiceText? = when (step) {
    IntakeStep.BODY_PART -> bodyMap.focus.bodyVoiceQuestion()
    IntakeStep.SYMPTOM_CHAT -> null
    IntakeStep.SEVERITY -> VoiceText(R.string.voice_intake_ask_severity, listOf(bodyPartSubject))
    IntakeStep.QUESTIONS -> VoiceText(R.string.voice_intake_ask_questions)
}

/**
 * 부위를 물을 때 고를 수 있는 곳을 함께 읽어 준다(#284). 큰 부위로 들어가 있으면 그 안의 상세
 * 부위를, 아니면 큰 부위를 읽는다.
 *
 * 인체도만 봐서는 팔 안에 어디를 짚을 수 있는지 알 수 없고, 사람은 부위 이름을 모를 수 있다. 목록을
 * 들으면 이름으로도 "세 번째"로도 고를 수 있다.
 */
private fun BodyMapSelection?.bodyVoiceQuestion(): VoiceText {
    if (this == null) {
        return VoiceText(R.string.voice_intake_ask_part, listOf(bodyVoiceAnchors().voiceList()))
    }
    val zones = bodyVoiceZones(anchorId).voiceList()
    return VoiceText(R.string.voice_intake_ask_zone, listOf(title(), zones))
}

/** 막대에 한 줄에 하나씩 적는다. 한 줄에 늘어놓으면 아홉 개가 이어져 읽기 어렵다. */
private fun List<Pair<String, String>>.voiceList(): String = joinToString("\n") { "· ${it.second}" }

/** 문답에서 "다음"은 이 말로 시작할 때만이다. "없어요"는 AI 질문에 대한 답일 수 있다. */
private const val NEXT_WORD = "다음"
