package models

import play.api.libs.json.Json

case class BCAddress(
                      line_1: String,
                      line_2: String,
                      line_3: Option[String] = None,
                      line_4: Option[String] = None,
                      postcode: Option[String] = None,
                      country: String) {

  override def toString = {
    val line3display = line_3.map(line3 => s"$line3, ").getOrElse("")
    val line4display = line_4.map(line4 => s"$line4, ").getOrElse("")
    val postcodeDisplay = postcode.map(postcode1 => s"$postcode1, ").getOrElse("")
    s"$line_1, $line_2, $line3display$line4display$postcodeDisplay$country"
  }
}

object BCAddress {
  implicit val formats = Json.format[BCAddress]
}

case class BusinessCustomerDetails(businessName: String,
                                   businessType: Option[String],
                                   businessAddress: BCAddress,
                                   sapNumber: String,
                                   safeId: String,
                                   agentReferenceNumber: Option[String],
                                   firstName: Option[String] = None,
                                   lastName: Option[String] = None)

object BusinessCustomerDetails {
  implicit val formats = Json.format[BusinessCustomerDetails]
}

case class BusinessHasWebsite(hasWebsite: Boolean, website: Option[String])

object BusinessHasWebsite {
  implicit val formats = Json.format[BusinessHasWebsite]
}

case class TelephoningBusiness(businessPhoneNumber: String,
                               mobileNumber: Option[String])

object TelephoningBusiness {
  implicit val formats = Json.format[TelephoningBusiness]
}


case class RegisteredOffice(isRegisteredOffice: Boolean, isCorrespondenceAddressSame: Boolean)

object RegisteredOffice {

  implicit val formats = Json.format[RegisteredOffice]

  def applyString(t: String): RegisteredOffice = {
    val regOffice: Seq[Boolean] = t.split(",").map(_.trim.toBoolean).toSeq
    RegisteredOffice(regOffice(0), regOffice(1))
  }

  def unapplyString(registeredOffice: RegisteredOffice) = {
    Some(s"${registeredOffice.isRegisteredOffice},${registeredOffice.isCorrespondenceAddressSame}")
  }

}