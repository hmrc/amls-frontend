package models.notifications

import org.joda.time.LocalDate


case class EndDateDetails(endDate:LocalDate, referenceNumber: Option[String])
