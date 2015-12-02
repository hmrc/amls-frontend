package models

import play.api.libs.json.Json

case class BusinessHasWebsite(hasWebsite: Boolean, website: Option[String])

object BusinessHasWebsite {
  implicit val formats = Json.format[BusinessHasWebsite]
}

case class TelephoningBusiness(businessPhoneNumber: String,
                               mobileNumber: Option[String])

object TelephoningBusiness {
  implicit val formats = Json.format[TelephoningBusiness]
}

case class BusinessWithVAT(hasVAT: Boolean, VATNum: Option[String])

object BusinessWithVAT {
  implicit val formats = Json.format[BusinessWithVAT]
}

object BusinessHasEmail {
  implicit val formats = Json.format[BusinessHasEmail]
}

case class BusinessHasEmail(email:String)

case class RegisteredForMLR(hasDigitalMLR: Boolean, hasNonDigitalMLR:Boolean , mlrNumber: Option[String], prevMlrNumber: Option[String])

object RegisteredForMLR {
  implicit val formats = Json.format[RegisteredForMLR]
  def applyString(hasMLR: (Boolean, Boolean),  mlrNumber: Option[String], prevMlrNumber: Option[String]): RegisteredForMLR = {
    RegisteredForMLR(hasMLR._1, hasMLR._2, mlrNumber, prevMlrNumber)
  }
  def unapplyString(registeredForMLR: RegisteredForMLR): (Option[( (Boolean, Boolean) , Option[String], Option[String])]) = {
    val tuple = (registeredForMLR.hasDigitalMLR, registeredForMLR.hasNonDigitalMLR)
    Some(Tuple3(tuple, registeredForMLR.mlrNumber, registeredForMLR.prevMlrNumber) )
  }
}
