package models.responsiblepeople

import play.api.libs.json.Json

case class ResponsiblePersonAddressHistory(currentAddress: Option[ResponsiblePersonAddress] = None,
                                           additionalAddress: Option[ResponsiblePersonAddress] = None,
                                           additionalExtraAddress: Option[ResponsiblePersonAddress] = None) {

  def currentAddress(add: ResponsiblePersonAddress): ResponsiblePersonAddressHistory =
    this.copy(currentAddress = Some(add))

  def additionalAddress(add: ResponsiblePersonAddress): ResponsiblePersonAddressHistory =
    this.copy(additionalAddress = Some(add))

  def additionalExtraAddress(add: ResponsiblePersonAddress): ResponsiblePersonAddressHistory =
    this.copy(additionalExtraAddress = Some(add))


  def isComplete: Boolean = currentAddress.isDefined

}

object ResponsiblePersonAddressHistory {

  implicit val format = Json.format[ResponsiblePersonAddressHistory]

}