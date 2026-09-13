package com.mist.medicalmate.navigation

import androidx.compose.runtime.getValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.mist.medicalmate.auth.ui.SessionUiState
import com.mist.medicalmate.auth.ui.loginDestination
import com.mist.medicalmate.calendar.ui.CalendarDayDestination
import com.mist.medicalmate.calendar.ui.ScheduleAddDestination
import com.mist.medicalmate.calendar.ui.calendarDayDestination
import com.mist.medicalmate.calendar.ui.calendarDestination
import com.mist.medicalmate.calendar.ui.scheduleAddDestination
import com.mist.medicalmate.card.ui.BriefCardDestination
import com.mist.medicalmate.card.ui.RecordDetailDestination
import com.mist.medicalmate.card.ui.briefCardListDestination
import com.mist.medicalmate.card.ui.recordDestination
import com.mist.medicalmate.card.ui.recordDetailDestination
import com.mist.medicalmate.home.ui.HomeDestination
import com.mist.medicalmate.intake.ui.IntakeDestination
import com.mist.medicalmate.intake.ui.IntakeDoneDestination
import com.mist.medicalmate.intake.ui.intakeDestination
import com.mist.medicalmate.intake.ui.intakeDoneDestination
import com.mist.medicalmate.profile.ui.AccountActionCallbacks
import com.mist.medicalmate.profile.ui.HealthEditDestination
import com.mist.medicalmate.profile.ui.ProfileCompleteDestination
import com.mist.medicalmate.profile.ui.ProfileSetupDestination
import com.mist.medicalmate.profile.ui.healthEditDestination
import com.mist.medicalmate.profile.ui.myProfileDestination
import com.mist.medicalmate.profile.ui.onboardingIntroDestination
import com.mist.medicalmate.profile.ui.profileCompleteDestination
import com.mist.medicalmate.profile.ui.profileSetupDestination
import com.mist.medicalmate.visit.ui.HospitalPickDestination
import com.mist.medicalmate.visit.ui.HospitalPickPurpose
import com.mist.medicalmate.visit.ui.VisitNoteDestination
import com.mist.medicalmate.visit.ui.VisitRecordDestination
import com.mist.medicalmate.visit.ui.hospitalPickDestination
import com.mist.medicalmate.visit.ui.visitNoteDestination
import com.mist.medicalmate.visit.ui.visitRecordDestination

/**
 * 그래프 등록을 도메인별로 나눈 확장 함수들.
 *
 * `MedicalMateNavHost`에서 옮겼다. 목적지가 열아홉이 되면서 한 파일에 함수가 열하나가
 * 됐고 detekt의 파일당 상한에 닿았다. `NavHost` 파일에는 그래프 본체와 세션 경계 처리만
 * 남긴다.
 *
 * 이 함수들이 `navigation`에 있는 이유는 목적지 사이의 이동을 알아야 하기 때문이다. 라우트
 * 타입과 `composable` 등록은 기능 패키지에 있고, 여기서는 어느 화면에서 어느 화면으로
 * 가는지만 정한다. 조합 루트라서 모든 기능을 참조해도 되는 유일한 패키지다.
 */
/**
 * 진입과 온보딩. Figma 흐름은 `1o → 1a-2 → 1b-1~1b-4`다.
 *
 * [session]을 받는 곳은 로그인 화면 하나다. 자동 로그인을 확인하지 못하고 왔는지에 따라
 * 문구가 달라서, 그 판단을 화면이 아니라 세션 상태가 한다.
 */
internal fun NavGraphBuilder.entryDestinations(
    navController: NavHostController,
    session: SessionUiState,
    onAuthenticated: (onboardingRequired: Boolean) -> Unit,
    onOnboardingCompleted: () -> Unit,
) {
    loginDestination(
        restoreFailed = session == SessionUiState.RestoreFailed,
        onAuthenticated = onAuthenticated,
    )
    onboardingIntroDestination(
        onDoneClick = { navController.navigate(ProfileSetupDestination) },
    )
    profileSetupDestination(
        onCompleted = { navController.navigate(ProfileCompleteDestination) },
        onExit = { navController.popBackStack() },
    )
    profileCompleteDestination(
        onFinished = {
            onOnboardingCompleted()
            navController.resetTo(HomeDestination(justRegistered = true))
        },
    )
}

/**
 * 증상 문답과 그 끝의 갈림길. Figma 흐름은 `1l·1c·1d·1i → 1c-5`다.
 *
 * 1c-5에서 브리핑 카드와 진료 전 병원 찾기로 갈린다. 병원을 먼저 찾아도 결국 카드로 오므로
 * 두 길이 같은 곳에서 만난다.
 */
internal fun NavGraphBuilder.intakeDestinations(navController: NavHostController) {
    intakeDestination(
        onCompleted = { sessionId -> navController.navigate(IntakeDoneDestination(sessionId)) },
        onExit = { navController.popBackStack() },
    )
    intakeDoneDestination(
        // 카드를 여기서 만들지 않는다. 만들 문답만 넘기고 카드 화면이 만든다. 병원을 먼저
        // 찾고 오는 길과 한 자리에서 만나게 하려는 것이다.
        onCardRequested = { sessionId -> navController.navigate(BriefCardDestination(sessionId = sessionId)) },
        onHospitalClick = { sessionId ->
            navController.navigate(
                HospitalPickDestination(purpose = HospitalPickPurpose.BEFORE_VISIT, sessionId = sessionId),
            )
        },
        onExit = { navController.popBackStack() },
    )
}

/**
 * 내 정보와 건강 정보 수정. Figma 흐름은 `1s-1 → 1s-2`다.
 *
 * 홈 헤더의 아바타에서 들어온다. 하단 탭이 세 개로 확정돼서 내 정보는 탭이 아니다.
 *
 * 계정 동작(로그아웃·회원탈퇴)이 여기로 내려온다. 홈에 임시로 붙어 있던 것이고 1s-1이
 * 그 자리다. 수행은 `SessionViewModel`이 하고 `MainActivity`가 연결한다.
 */
internal fun NavGraphBuilder.profileDestinations(
    navController: NavHostController,
    accountActions: AccountActionCallbacks,
) {
    myProfileDestination(
        accountActions = accountActions,
        onHealthEdit = { navController.navigate(HealthEditDestination) },
        onExit = { navController.popBackStack() },
    )
    healthEditDestination(
        onSaved = { navController.popBackStack() },
        onExit = { navController.popBackStack() },
    )
}

/**
 * 진료 후 기록 플로우. Figma 흐름은 `1m → 1p → 1q-1 → 1k`다.
 *
 * 캘린더 일자 화면의 "진료 후 기록하기"에서 들어온다. 진료가 끝난 날에 그 날짜를 열어
 * 적는 것이 자연스러운 경로다.
 *
 * 마지막 화면에서 홈으로 나갈 때 백스택을 비운다. 저장이 끝난 흐름을 뒤로 가기로 다시
 * 밟으면 같은 기록을 두 번 저장하게 된다.
 */
/** 캘린더 세 목적지. 월(1r-1) · 일자(1r-2) · 일정 추가(1r-4)다. */
internal fun NavGraphBuilder.calendarDestinations(navController: NavHostController) {
    calendarDestination(
        onDayOpen = { date -> navController.navigate(CalendarDayDestination(date.toString())) },
        onCardOpen = { cardId -> navController.navigate(BriefCardDestination(cardId)) },
        onAddClick = { navController.navigate(ScheduleAddDestination()) },
        onTabSelect = navController::selectTab,
    )
    calendarDayDestination(
        onCardOpen = { cardId -> navController.navigate(BriefCardDestination(cardId)) },
        // 이 날 일정에 걸린 카드에 기록이 붙는다. 서버가 카드 하나에 기록 하나를 받는다.
        // 카드 제목도 함께 간다. 1p가 무엇으로 진료받았는지를 그 값으로 적는다.
        onRecordAdd = { cardId, cardTitle, visitedOn ->
            navController.navigate(
                HospitalPickDestination(
                    cardId = cardId,
                    cardTitle = cardTitle,
                    // 오늘이 아니라 그 일자다. 어제 진료를 오늘 적어도 기록은 그 날에 남는다.
                    visitedOn = visitedOn.toString(),
                ),
            )
        },
        onRecordOpen = { recordId -> navController.navigate(RecordDetailDestination(recordId)) },
        // 1r-2-A의 다음 일정이 시간만 비어 있는 상태다. 확정하러 가면 병원이 이미 채워진
        // 일정 추가(1r-4-B)가 열린다.
        onScheduleConfirm = { clinic -> navController.navigate(ScheduleAddDestination(hospitalName = clinic)) },
        onExit = { navController.popBackStack() },
    )
    scheduleAddDestination(
        onHospitalPick = {
            navController.navigate(HospitalPickDestination(purpose = HospitalPickPurpose.SCHEDULE))
        },
        // 새 카드는 증상 문답부터다. 만들고 나면 그 카드가 목록에 들어온다.
        onCardNew = { navController.navigate(IntakeDestination()) },
        onSaved = { navController.popBackStack() },
        onExit = { navController.popBackStack() },
    )
}

internal fun NavGraphBuilder.visitDestinations(navController: NavHostController) {
    hospitalPickDestination(
        // 진료 후(1m). 고른 병원 이름과 붙일 카드를 메모 화면으로 넘긴다.
        onPicked = { cardId, cardTitle, visitedOn, hospital ->
            navController.navigate(
                VisitNoteDestination(
                    clinic = hospital.name,
                    cardId = cardId,
                    cardTitle = cardTitle,
                    visitedOn = visitedOn,
                ),
            )
        },
        // 진료 전(1m-B)에서 카드로. cardId가 있으면 카드의 `변경`에서 온 것이라 고른
        // 병원만 남기고 그 카드로 돌아간다. 엔트리를 갈아치우면 그 화면이 편집 중이던 값을
        // 잃는다. cardId가 없으면 문답을 마치고 온 것이라 그 문답으로 카드를 만든다.
        onCardRequested = { cardId, sessionId, hospital ->
            if (cardId == null) {
                navController.navigate(
                    BriefCardDestination(
                        sessionId = sessionId,
                        hospitalName = hospital?.name,
                        hospitalAddress = hospital?.address,
                    ),
                )
            } else {
                navController.popWithResult(
                    NavResult.HOSPITAL_NAME to hospital?.name,
                    NavResult.HOSPITAL_ADDRESS to hospital?.address,
                )
            }
        },
        // 일정 추가(1r-4)의 병원 필드에서 온 것. 고른 이름만 남기고 뒤로 간다. 엔트리를
        // 갈아치우면 그 화면의 ViewModel이 정리되면서 적어 둔 날짜·시간·할 일이 사라진다.
        onScheduleRequested = { hospital ->
            navController.popWithResult(NavResult.HOSPITAL_NAME to hospital?.name)
        },
        onExit = { navController.popBackStack() },
    )
    visitNoteDestination(
        onSaved = { clinic, cardId, note, visitedOn ->
            navController.navigate(
                VisitRecordDestination(clinic = clinic, cardId = cardId, note = note, visitedOn = visitedOn),
            )
        },
        onExit = { navController.popBackStack() },
    )
    visitRecordDestination(
        // 저장하면 흐름이 시작된 캘린더 일자로 돌아간다. 그 화면이 다시 읽으면서 방금 남긴
        // 기록이 "이 날 기록"으로 선다(1r-2-A). 시안에 있던 이번 진료 정리(1k)는 사라졌다.
        onSaved = { navController.popBackStack<CalendarDayDestination>(inclusive = false) },
        // 지운 기록의 화면에 남을 수 없다. 한 단계만 pop하면 방금 적은 메모 화면(1p)으로
        // 돌아가는데, 거기서 저장하면 지운 것을 다시 만든다. 그래서 흐름이 시작된 캘린더
        // 일자까지 되돌린다.
        onDeleted = { navController.popBackStack<CalendarDayDestination>(inclusive = false) },
        onExit = { navController.popBackStack() },
    )
}

/**
 * 기록 탭과 그 아래 화면.
 *
 * 목록에서 상세로, 상세의 "카드 열기"에서 브리핑 카드로 이어진다. 이 묶음만 따로 뺀 이유는
 * [MedicalMateNavHost]의 길이다. 화면이 늘면 도메인별로 이렇게 나눈다.
 */
internal fun NavGraphBuilder.recordDestinations(navController: NavHostController) {
    recordDestination(
        onItemClick = { recordId -> navController.navigate(RecordDetailDestination(recordId)) },
        onStartIntakeClick = { navController.navigate(IntakeDestination()) },
        onTabSelect = navController::selectTab,
    )
    recordDetailDestination(onBackClick = { navController.popBackStack() })
    // 1j-4. 기록 탭이 아니라 홈의 "전체 보기"에서 들어오는데, 목록과 줄이 기록과 같은
    // 짜임이라 여기 함께 둔다.
    briefCardListDestination(
        // 병원 이름을 함께 나른다. 카드 상세 응답에 병원이 없고 목록 응답에만 있다.
        // 서버가 카드에 실어 주면 이 인자가 사라진다(Backend#84).
        onCardClick = { cardId, clinic ->
            navController.navigate(BriefCardDestination(cardId = cardId, hospitalName = clinic))
        },
        onStartIntakeClick = { navController.navigate(IntakeDestination()) },
        onExit = { navController.popBackStack() },
    )
}

/**
 * 방금 만든 카드의 임시 id.
 *
 * 문답을 마치면 서버가 카드를 만들고 그 id를 준다. 그 호출이 아직 없어서 자리만 채운다.
 * 카드 화면은 지금 id를 보지 않고 픽스처를 그린다.
 */

/**
 * 방금 만든 방문의 임시 id.
 *
 * 메모를 저장하면 서버가 방문을 만들고 그 id를 준다. 그 호출이 아직 없어서 자리만 채운다.
 */
