/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models.estateagentbusiness

import config.ApplicationConfig
import models.amp.Amp
import models.registrationprogress.{Completed, NotStarted, Section, Started}
import play.api.libs.json._
import play.api.mvc.Call
import typeclasses.MongoKey
import uk.gov.hmrc.http.cache.client.CacheMap

final case class Eab(data: JsObject = Json.obj(),
                     hasChanged: Boolean = false,
                     hasAccepted: Boolean = false) {

  /**
    * Provides a means of setting data that will update the hasChanged flag
    *
    * Set data via this method and NOT directly in the constructor
    */
  def data(p: JsObject): Eab =
    this.copy(data = p, hasChanged = hasChanged || this.data != p, hasAccepted = hasAccepted && this.data == p)

  def get[A](path: JsPath)(implicit rds: Reads[A]): Option[A] =
    Reads.optionNoError(Reads.at(path)).reads(data).getOrElse(None)

  def valueAt(path: JsPath): String = {
    get[JsValue](path).getOrElse(Eab.notPresent).toString().toLowerCase()
  }

  def accept: Eab = this.copy(hasAccepted = true)

  def isComplete: Boolean =
    isServicesComplete &&
    isRedressSchemeComplete &&
    isProtectionSchemeComplete &&
    isEstateAgentActPenaltyComplete &&
    isProfessionalBodyPenaltyComplete &&
    hasAccepted

  private[estateagentbusiness] def isServicesComplete: Boolean = (data \ "eabServicesProvided").as[List[String]].nonEmpty

  private[estateagentbusiness] def isRedressSchemeComplete: Boolean = {
    val services = (data \ "eabServicesProvided").as[List[String]]
    val scheme = get[String](Eab.redressScheme)
    (services, scheme) match {
      case (x, _) if !x.contains("residential") => true
      case (_, Some(x)) if x.nonEmpty => true
      case _ => false
    }
  }

  private[estateagentbusiness] def isProtectionSchemeComplete: Boolean = {
    val services = (data \ "eabServicesProvided").as[List[String]]
    val scheme = get[Boolean](Eab.clientMoneyProtectionScheme)
    (services, scheme) match {
      case (x, _) if !x.contains("lettings") => true
      case (_, Some(_)) => true
      case _ => false
    }
  }

  private[estateagentbusiness] def booleanAndDetailComplete(boolOpt: Option[Boolean], detailOpt: Option[String]):Boolean =
    (boolOpt, detailOpt) match {
      case (Some(true), Some(detail)) if detail.nonEmpty => true
      case (Some(false), _) => true
      case _ => false
    }

  private[estateagentbusiness] def isEstateAgentActPenaltyComplete: Boolean =
    booleanAndDetailComplete(get[Boolean](Eab.penalisedEstateAgentsAct), get[String](Eab.penalisedEstateAgentsActDetail))

  private[estateagentbusiness] def isProfessionalBodyPenaltyComplete: Boolean =
    booleanAndDetailComplete(get[Boolean](Eab.penalisedProfessionalBody), get[String](Eab.penalisedProfessionalBodyDetail))
}

object Eab {

  val eabServicesProvided             = JsPath \ "eabServicesProvided"
  val redressScheme                   = JsPath \ "redressScheme"
  val redressSchemeDetail             = JsPath \ "redressSchemeDetail"
  val clientMoneyProtectionScheme     = JsPath \ "clientMoneyProtectionScheme"
  val penalisedEstateAgentsAct        = JsPath \ "penalisedEstateAgentsAct"
  val penalisedEstateAgentsActDetail  = JsPath \ "penalisedEstateAgentsActDetail"
  val penalisedProfessionalBody       = JsPath \ "penalisedProfessionalBody"
  val penalisedProfessionalBodyDetail = JsPath \ "penalisedProfessionalBodyDetail"
  val notPresent                      = "null"

  val redirectCallType       = "GET"
  val key                    = "eab"

  private def generateRedirect(destinationUrl: String) = {
    Call(redirectCallType, destinationUrl)
  }

  // TODO: Update the URLs once config work is complete

  def section(implicit cache: CacheMap): Section = {
    val notStarted = Section(key, NotStarted, false, generateRedirect(ApplicationConfig.ampWhatYouNeedUrl))
    cache.getEntry[Amp](key).fold(notStarted) {
      model =>
        if (model.isComplete && model.hasAccepted) {
          Section(key, Completed, model.hasChanged, generateRedirect(ApplicationConfig.ampSummeryUrl))
        } else {
          Section(key, Started, model.hasChanged, generateRedirect(ApplicationConfig.ampWhatYouNeedUrl))
        }
    }
  }

  implicit val mongoKey = new MongoKey[Eab] {
    override def apply(): String = key
  }

  implicit lazy val reads: Reads[Eab] = {

    val servicesTransform = (__ \ 'data ++ eabServicesProvided).json.copyFrom(
      (__ \ 'services).json.pick.map {
        case JsArray(values) => JsArray(values.map {
          case JsString("01") => JsString("residential")
          case JsString("02") => JsString("commercial")
          case JsString("03") => JsString("auctioneering")
          case JsString("04") => JsString("relocation")
          case JsString("05") => JsString("businessTransfer")
          case JsString("06") => JsString("assetManagement")
          case JsString("07") => JsString("landManagement")
          case JsString("08") => JsString("developmentCompany")
          case JsString("09") => JsString("socialHousingProvision")
          case JsString("10") => JsString("lettings")
          case _ => JsNull
        })
        case _ => JsArray()
      })

    val redressTransform = (__ \ 'data ++ redressScheme).json.copyFrom(
      (__ \ 'propertyRedressScheme).readNullable[JsValue].map {
        case Some(JsString("01")) => JsString("propertyOmbudsmanLimited")
        case Some(JsString("02")) => JsString("ombudsmanServices")
        case Some(JsString("03")) => JsString("propertyRedressScheme")
        case Some(JsString("04")) => JsString("other")
        case _ => JsNull}
    )

    def readPathOrReturn(path: JsPath, returnValue: JsValue) =
      path.readNullable[JsValue].map(_.getOrElse(returnValue))

    import play.api.libs.json.Reads._
    import play.api.libs.functional.syntax._

    val oldModelTransformer:Reads[JsObject] = (servicesTransform and redressTransform and
      (__ \ 'data ++ penalisedEstateAgentsAct).json.copyFrom(readPathOrReturn(__ \ 'penalisedUnderEstateAgentsAct, JsNull)) and
      (__ \ 'data ++ penalisedEstateAgentsActDetail).json.copyFrom(readPathOrReturn( __ \ 'penalisedUnderEstateAgentsActDetails, JsNull)) and
      (__ \ 'data ++ penalisedProfessionalBody).json.copyFrom(readPathOrReturn(__ \ 'penalised, JsNull)) and
      (__ \ 'data ++ penalisedProfessionalBodyDetail).json.copyFrom(readPathOrReturn(__ \ 'professionalBody,JsNull)) and
      (__ \ 'hasAccepted).json.copyFrom((__ \ 'hasAccepted).json.pick) and
      (__ \ 'hasChanged).json.copyFrom((__ \ 'hasChanged).json.pick)
    ) reduce

    val jsonReads = (
        (__ \ "data").read[JsObject] and
        (__ \ "hasChanged").readNullable[Boolean].map(_.getOrElse(false)) and
        (__ \ "hasAccepted" ).readNullable[Boolean].map(_.getOrElse(false))
      )(Eab.apply _)

    (__ \ "services").readNullable[List[String]]
      .flatMap(_ => oldModelTransformer) andThen jsonReads orElse jsonReads
  }

  implicit lazy val writes: OWrites[Eab] = {
    import play.api.libs.functional.syntax._
    (
      (__ \ "data").write[JsObject] and
      (__ \ "hasChanged").write[Boolean] and
      (__ \ "hasAccepted").write[Boolean]
    ) (unlift(Eab.unapply))
  }

  implicit val formatOption = Reads.optionWithNull[Eab]
}


