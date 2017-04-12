package models.notifications

import models.confirmation.Currency

case class ReminderDetails(paymentAmount: Currency, referenceNumber: String)