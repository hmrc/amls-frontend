/*
 * Copyright 2024 HM Revenue & Customs
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

package models.eab

import config.ApplicationConfig
import models.registrationprogress.{Completed, NotStarted, Section, Started, TaskRow, Updated}
import play.api.i18n.Messages
import play.api.libs.json._
import play.api.mvc.Call
import typeclasses.MongoKey
import services.cache.Cache

case class Eab(data: JsObject = Json.obj(), hasChanged: Boolean = false, hasAccepted: Boolean = false) {

  /** Provides a means of setting data that will update the hasChanged flag
    *
    * Set data via this method and NOT directly in the constructor
    */
  def data(p: JsObject): Eab =
    this.copy(data = p, hasChanged = hasChanged || this.data != p, hasAccepted = hasAccepted && this.data == p)

  def get[A](path: JsPath)(implicit rds: Reads[A]): Option[A] =
    Reads.optionNoError(Reads.at(path)).reads(data).getOrElse(None)

  def valueAt(path: JsPath): String =
    get[JsValue](path).getOrElse(Eab.notPresent).toString.toLowerCase()

  def accept: Eab = this.copy(hasAccepted = true)

  def isComplete: Boolean =
    isServicesComplete &&
      isRedressSchemeComplete &&
      isProtectionSchemeComplete &&
      isEstateAgentActPenaltyComplete &&
      isProfessionalBodyPenaltyComplete &&
      hasAccepted

  private[eab] def isServicesComplete: Boolean = (data \ "eabServicesProvided").as[List[String]].nonEmpty

  private[eab] def isRedressSchemeComplete: Boolean = {
    val services = (data \ "eabServicesProvided").as[List[String]]
    val scheme   = get[String](Eab.redressScheme)
    (services, scheme) match {
      case (x, _) if !x.contains("residential")                              => true
      case (_, Some(x)) if x.nonEmpty && x.contains("propertyRedressScheme") => true
      case (_, Some(x)) if x.nonEmpty && x.contains("propertyOmbudsman")     => true
      case (_, Some(x)) if x.nonEmpty && x.contains("notRegistered")         => true
      case _                                                                 => false
    }
  }

  private[eab] def isProtectionSchemeComplete: Boolean = {
    val services = (data \ "eabServicesProvided").as[List[String]]
    val scheme   = get[Boolean](Eab.clientMoneyProtectionScheme)
    (services, scheme) match {
      case (x, _) if !x.contains("lettings") => true
      case (_, Some(_))                      => true
      case _                                 => false
    }
  }

  private[eab] def booleanAndDetailComplete(boolOpt: Option[Boolean], detailOpt: Option[String]): Boolean =
    (boolOpt, detailOpt) match {
      case (Some(true), Some(detail)) if detail.nonEmpty => true
      case (Some(false), _)                              => true
      case _                                             => false
    }

  private[eab] def isEstateAgentActPenaltyComplete: Boolean =
    booleanAndDetailComplete(
      get[Boolean](Eab.penalisedEstateAgentsAct),
      get[String](Eab.penalisedEstateAgentsActDetail)
    )

  private[eab] def isProfessionalBodyPenaltyComplete: Boolean =
    booleanAndDetailComplete(
      get[Boolean](Eab.penalisedProfessionalBody),
      get[String](Eab.penalisedProfessionalBodyDetail)
    )

  def isInvalidRedressScheme: Boolean = {
    val scheme = get[String](Eab.redressScheme)
    scheme match {
      case Some(x) if x.nonEmpty && x.contains("other")             => true
      case Some(x) if x.nonEmpty && x.contains("ombudsmanServices") => true
      case _                                                        => false
    }
  }

  def services: Option[List[String]] =
    get[List[String]](Eab.eabServicesProvided)
}

object Eab {

  val eabServicesProvided             = JsPath \ "eabServicesProvided"
  val dateOfChange                    = JsPath \ "dateOfChange"
  val redressScheme                   = JsPath \ "redressScheme"
  val redressSchemeDetail             = JsPath \ "redressSchemeDetail"
  val clientMoneyProtectionScheme     = JsPath \ "clientMoneyProtectionScheme"
  val penalisedEstateAgentsAct        = JsPath \ "penalisedEstateAgentsAct"
  val penalisedEstateAgentsActDetail  = JsPath \ "penalisedEstateAgentsActDetail"
  val penalisedProfessionalBody       = JsPath \ "penalisedProfessionalBody"
  val penalisedProfessionalBodyDetail = JsPath \ "penalisedProfessionalBodyDetail"
  val notPresent                      = "null"

  val redirectCallType = "GET"
  val key              = "estate-agent-business"

  private def generateRedirect(destinationUrl: String) =
    Call(redirectCallType, destinationUrl)

  def taskRow(appConfig: ApplicationConfig)(implicit cache: Cache, messages: Messages): TaskRow = {
    val messageKey = "eab"
    val notStarted = TaskRow(
      messageKey,
      generateRedirect(appConfig.eabWhatYouNeedUrl).url,
      hasChanged = false,
      NotStarted,
      TaskRow.notStartedTag
    )
    cache.getEntry[Eab](key).fold(notStarted) { model =>
      if (model.isComplete && model.hasChanged && model.hasAccepted) {
        TaskRow(
          messageKey,
          generateRedirect(appConfig.eabSummaryUrl).url,
          hasChanged = true,
          status = Updated,
          tag = TaskRow.updatedTag
        )
      } else if (model.isComplete && model.hasAccepted) {
        TaskRow(
          messageKey,
          generateRedirect(appConfig.eabSummaryUrl).url,
          model.hasChanged,
          Completed,
          TaskRow.completedTag
        )
      } else {
        TaskRow(
          messageKey,
          generateRedirect(appConfig.eabWhatYouNeedUrl).url,
          model.hasChanged,
          Started,
          TaskRow.incompleteTag
        )
      }
    }
  }

  implicit val mongoKey: MongoKey[Eab] = new MongoKey[Eab] {
    override def apply(): String = key
  }

  implicit lazy val reads: Reads[Eab] = {

    val servicesTransform =
      (__ \ Symbol("data") ++ eabServicesProvided).json.copyFrom((__ \ Symbol("services")).json.pick.map {
        case JsArray(values) =>
          JsArray(values.map {
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
            case _              => JsNull
          })
        case _               => JsArray()
      })

    val isRedressTransform = (__ \ Symbol("data") ++ redressScheme).json.copyFrom(
      (__ \ Symbol("isRedress"))
        .readNullable[JsValue]
        .map {
          case Some(JsBoolean(false)) => Some(JsString("notRegistered"))
          case _                      => None
        }
        .filter(redressScheme => redressScheme.isDefined)
        .orElse(
          (__ \ Symbol("propertyRedressScheme")).readNullable[JsValue].map {
            case Some(JsString("01")) => Some(JsString("propertyOmbudsman"))
            case Some(JsString("02")) => Some(JsString("ombudsmanServices"))
            case Some(JsString("03")) => Some(JsString("propertyRedressScheme"))
            case Some(JsString("04")) => Some(JsString("other"))
            case _                    => None
          }
        )
        .map {
          case Some(redressScheme) => redressScheme
          case None                => JsNull
        }
    )

    def readPathOrReturn(path: JsPath, returnValue: JsValue) =
      path.readNullable[JsValue].map(_.getOrElse(returnValue))

    import play.api.libs.functional.syntax._
    import play.api.libs.json.Reads._
    import scala.language.postfixOps

    val oldModelTransformer: Reads[JsObject] = (servicesTransform and isRedressTransform and
      (__ \ Symbol("data") ++ dateOfChange).json.copyFrom(readPathOrReturn(__ \ Symbol("dateOfChange"), JsNull)) and
      (__ \ Symbol("data") ++ penalisedEstateAgentsAct).json
        .copyFrom(readPathOrReturn(__ \ Symbol("penalisedUnderEstateAgentsAct"), JsNull)) and
      (__ \ Symbol("data") ++ penalisedEstateAgentsActDetail).json
        .copyFrom(readPathOrReturn(__ \ Symbol("penalisedUnderEstateAgentsActDetails"), JsNull)) and
      (__ \ Symbol("data") ++ penalisedProfessionalBody).json
        .copyFrom(readPathOrReturn(__ \ Symbol("penalised"), JsNull)) and
      (__ \ Symbol("data") ++ penalisedProfessionalBodyDetail).json
        .copyFrom(readPathOrReturn(__ \ Symbol("professionalBody"), JsNull)) and
      (__ \ Symbol("data") ++ clientMoneyProtectionScheme).json
        .copyFrom(readPathOrReturn(__ \ Symbol("clientMoneyProtection"), JsNull)) and
      (__ \ Symbol("hasAccepted")).json.copyFrom((__ \ Symbol("hasAccepted")).json.pick) and
      (__ \ Symbol("hasChanged")).json.copyFrom((__ \ Symbol("hasChanged")).json.pick)) reduce

    val jsonReads = (
      (__ \ "data").read[JsObject] and
        (__ \ "hasChanged").readNullable[Boolean].map(_.getOrElse(false)) and
        (__ \ "hasAccepted").readNullable[Boolean].map(_.getOrElse(false))
    )(Eab.apply _)

    (__ \ "services")
      .readNullable[List[String]]
      .flatMap(_ => oldModelTransformer) andThen jsonReads orElse jsonReads
  }

  implicit lazy val writes: OWrites[Eab] = {
    import play.api.libs.functional.syntax._
    (
      (__ \ "data").write[JsObject] and
        (__ \ "hasChanged").write[Boolean] and
        (__ \ "hasAccepted").write[Boolean]
    )(unlift(Eab.unapply))
  }

  implicit val formatOption: Reads[Option[Eab]] = Reads.optionWithNull[Eab]
}
