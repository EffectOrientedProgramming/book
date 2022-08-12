package virtual_meeting

import zio._
import zio.stream._

trait Rule
object Rule:
  trait MaximumContinuousSpeaker extends Rule
  trait DismissNonParticipants   extends Rule
  trait MaximumParticipants      extends Rule
  trait MaximumLength            extends Rule
  trait OneSpeakerAtATime        extends Rule
  trait MaximumStartDelay        extends Rule

  trait CompositeRule extends Rule

trait Participant

trait ParticipantAction
object ParticipantAction:
  trait Speak    extends ParticipantAction
  trait Emoji    extends ParticipantAction
  trait TextChat extends ParticipantAction

trait ParticipantStatus
object ParticipantStatus:
  trait VideoAndAudio  extends ParticipantStatus
  trait VideoOnly      extends ParticipantStatus
  trait AudioOnly      extends ParticipantStatus
  trait NoVideoNoAudio extends ParticipantStatus

trait CorrectiveAction
object CorrectiveAction:
  trait Disband         extends CorrectiveAction
  trait WarnParticipant extends CorrectiveAction
  trait MuteParticipant extends CorrectiveAction
  trait DismissParticipant
      extends CorrectiveAction
  trait SplitMeeting extends CorrectiveAction

case class MeetingMoment(
    actions: Set[ParticipantAction]
)

trait MeetingEnforcer:
  def process(
      meetingMoment: MeetingMoment
  ): Option[CorrectiveAction]

case class Meeting(
    frames: zio.stream.ZStream[
      Any,
      Nothing,
      MeetingMoment
    ]
)

case class Meeting2(
    frames: Ref[Set[zio.stream.ZStream[
      Any,
      Nothing,
      MeetingMoment
    ]]]
)
