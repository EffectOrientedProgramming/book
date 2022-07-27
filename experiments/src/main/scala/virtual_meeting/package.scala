package virtual_meeting


trait Rule
object Rule:
  trait MaximumContinuousSpeaker extends Rule
  trait DismissNonParticipants extends Rule
  trait MaximumParticipants extends Rule
  trait MaximumLength extends Rule
  trait OneSpeakerAtATime extends Rule
  trait MaximumStartDelay extends Rule

  trait CompositeRule extends Rule

trait Participant

trait ParticipantAction
object ParticipantAction:
  trait Speak extends ParticipantAction
  trait Emoji extends ParticipantAction
  trait Chat extends ParticipantAction

trait ParticipantStatus
object ParticipantStatus:
  trait VideoOnly extends ParticipantStatus
  trait AudioOnly extends ParticipantStatus


trait CorrectiveAction
object CorrectiveAction:
  trait Disband extends CorrectiveAction
  trait MuteParticipant extends CorrectiveAction
  trait DismissParticipant extends CorrectiveAction
  trait Split extends CorrectiveAction

case class MeetingMoment(
                          actions: Set[ParticipantAction]
                        )