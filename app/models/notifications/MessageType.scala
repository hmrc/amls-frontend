package models.notifications

sealed trait ContactType

case object ApplicationApproval extends ContactType
case object RenewalApproval extends ContactType

case object RejectionReasons extends ContactType
case object RevocationReasons extends ContactType
case object AutoExpiryOfRegistration extends ContactType

case object ReminderToPayForApplication extends ContactType
case object ReminderToPayForRenewal extends ContactType
case object ReminderToPayForVariation extends ContactType
case object ReminderToPayForManualCharges extends ContactType
case object RenewalReminder extends ContactType

case object MindedToReject extends ContactType
case object MindedToRevoke extends ContactType
case object NoLongerMindedToReject extends ContactType
case object NoLongerMindedToRevoke extends ContactType
case object Others extends ContactType