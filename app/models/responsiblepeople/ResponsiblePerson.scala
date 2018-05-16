/*
 * Copyright 2018 HM Revenue & Customs
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

package models.responsiblepeople

import config.ApplicationConfig
import models.registrationprogress.{Completed, NotStarted, Section, Started}
import models.responsiblepeople.TimeAtAddress.{SixToElevenMonths, ZeroToFiveMonths}
import org.joda.time.LocalDate
import play.Logger
import play.api.libs.json.Reads
import typeclasses.MongoKey
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.StatusConstants

import scala.collection.Seq

case class ResponsiblePerson(personName: Option[PersonName] = None,
                             legalName: Option[PreviousName] = None,
                             legalNameChangeDate: Option[LocalDate] = None,
                             knownBy: Option[KnownBy] = None,
                             personResidenceType: Option[PersonResidenceType] = None,
                             ukPassport: Option[UKPassport] = None,
                             nonUKPassport: Option[NonUKPassport] = None,
                             dateOfBirth: Option[DateOfBirth] = None,
                             contactDetails: Option[ContactDetails] = None,
                             addressHistory: Option[ResponsiblePersonAddressHistory] = None,
                             positions: Option[Positions] = None,
                             saRegistered: Option[SaRegistered] = None,
                             vatRegistered: Option[VATRegistered] = None,
                             experienceTraining: Option[ExperienceTraining] = None,
                             training: Option[Training] = None,
                             hasAlreadyPassedFitAndProper: Option[Boolean] = None,
                             hasChanged: Boolean = false,
                             hasAccepted: Boolean = false,
                             lineId: Option[Int] = None,
                             status: Option[String] = None,
                             endDate: Option[ResponsiblePersonEndDate] = None,
                             soleProprietorOfAnotherBusiness: Option[SoleProprietorOfAnotherBusiness] = None
                            ) {

  def personName(p: PersonName): ResponsiblePerson =
    this.copy(personName = Some(p), hasChanged = hasChanged || !this.personName.contains(p),
      hasAccepted = hasAccepted && this.personName.contains(p))

  def legalName(p: PreviousName): ResponsiblePerson =
    this.copy(legalName = Some(p), hasChanged = hasChanged || !this.legalName.contains(p),
      hasAccepted = hasAccepted && this.legalName.contains(p))

  def legalNameChangeDate(p: LocalDate): ResponsiblePerson =
    this.copy(legalNameChangeDate = Some(p), hasChanged = hasChanged || !this.legalNameChangeDate.contains(p),
      hasAccepted = hasAccepted && this.legalNameChangeDate.contains(p))

  def knownBy(p: KnownBy): ResponsiblePerson =
    this.copy(knownBy = Some(p), hasChanged = hasChanged || !this.knownBy.contains(p),
      hasAccepted = hasAccepted && this.knownBy.contains(Some(p)))

  def personResidenceType(p: PersonResidenceType): ResponsiblePerson =
    this.copy(personResidenceType = Some(p), hasChanged = hasChanged || !this.personResidenceType.contains(p),
      hasAccepted = hasAccepted && this.personResidenceType.contains(p))

  def personResidenceType(p: Option[PersonResidenceType]): ResponsiblePerson =
    this.copy(personResidenceType = p, hasChanged = hasChanged || this.personResidenceType != p,
      hasAccepted = hasAccepted && this.personResidenceType.equals(p))

  def contactDetails(p: ContactDetails): ResponsiblePerson =
    this.copy(contactDetails = Some(p), hasChanged = hasChanged || !this.contactDetails.contains(p),
      hasAccepted = hasAccepted && this.contactDetails.contains(p))

  def saRegistered(p: SaRegistered): ResponsiblePerson =
    this.copy(saRegistered = Some(p), hasChanged = hasChanged || !this.saRegistered.contains(p),
      hasAccepted = hasAccepted && this.saRegistered.contains(p))

  def addressHistory(p: ResponsiblePersonAddressHistory): ResponsiblePerson =
    this.copy(addressHistory = Some(p), hasChanged = hasChanged || !this.addressHistory.contains(p),
      hasAccepted = hasAccepted && this.addressHistory.contains(p))

  def positions(p: Positions): ResponsiblePerson =
    this.copy(positions = Some(p), hasChanged = hasChanged || !this.positions.contains(p),
      hasAccepted = hasAccepted && this.positions.contains(p))

  def soleProprietorOfAnotherBusiness(p: SoleProprietorOfAnotherBusiness): ResponsiblePerson =
    this.copy(soleProprietorOfAnotherBusiness = Some(p), hasChanged = hasChanged || !this.soleProprietorOfAnotherBusiness.contains(p),
      hasAccepted = hasAccepted && this.soleProprietorOfAnotherBusiness.contains(p))

  def vatRegistered(p: VATRegistered): ResponsiblePerson =
    this.copy(vatRegistered = Some(p), hasChanged = hasChanged || !this.vatRegistered.contains(p),
      hasAccepted = hasAccepted && this.vatRegistered.contains(p))

  def experienceTraining(p: ExperienceTraining): ResponsiblePerson =
    this.copy(experienceTraining = Some(p), hasChanged = hasChanged || !this.experienceTraining.contains(p),
      hasAccepted = hasAccepted && this.experienceTraining.contains(p))

  def training(p: Training): ResponsiblePerson =
    this.copy(training = Some(p), hasChanged = hasChanged || !this.training.contains(p),
      hasAccepted = hasAccepted && this.training.contains(p))

  def hasAlreadyPassedFitAndProper(p: Option[Boolean]): ResponsiblePerson =
    this.copy(hasAlreadyPassedFitAndProper = p, hasChanged = hasChanged || !this.hasAlreadyPassedFitAndProper.equals(p),
      hasAccepted = hasAccepted && this.hasAlreadyPassedFitAndProper.equals(p))

  def ukPassport(p: UKPassport): ResponsiblePerson =
    this.copy(ukPassport = Some(p), hasChanged = hasChanged || !this.ukPassport.contains(p),
      hasAccepted = hasAccepted && this.ukPassport.contains(p))

  def nonUKPassport(p: NonUKPassport): ResponsiblePerson =
    this.copy(nonUKPassport = Some(p), hasChanged = hasChanged || !this.nonUKPassport.contains(p),
      hasAccepted = hasAccepted && this.nonUKPassport.contains(p))

  def dateOfBirth(p: DateOfBirth): ResponsiblePerson =
    this.copy(dateOfBirth = Some(p), hasChanged = hasChanged || !this.dateOfBirth.contains(p),
      hasAccepted = hasAccepted && this.dateOfBirth.contains(p))

  def status(p: String): ResponsiblePerson =
    this.copy(status = Some(p), hasChanged = hasChanged || !this.status.contains(p),
      hasAccepted = hasAccepted && this.status.contains(p))

  def checkVatField(otherBusinessSP: Option[SoleProprietorOfAnotherBusiness]): Boolean = {
    otherBusinessSP.fold(true) { x =>
      x.soleProprietorOfAnotherBusiness match {
        case true => this.vatRegistered.isDefined
        case false => this.vatRegistered.isEmpty
      }
    }
  }

  def isComplete: Boolean = {
    Logger.debug(s"[ResponsiblePeople][isComplete] $this")

    this match {
      case ResponsiblePerson(Some(_),Some(_),Some(_),Some(_),Some(_), _, _, _,Some(_),Some(_), Some(pos),Some(_), _,Some(_),Some(_), _, _, true, _, _, _, otherBusinessSP)
        if pos.startDate.isDefined & checkVatField(otherBusinessSP) & validateAddressHistory => true
      case ResponsiblePerson(Some(_),Some(pName),None,Some(_),Some(_), _, _, _,Some(_),Some(_), Some(pos),Some(_), _,Some(_),Some(_), _, _, true, _, _, _, otherBusinessSP)
        if pos.startDate.isDefined & checkVatField(otherBusinessSP) & validateAddressHistory && !pName.hasPreviousName.get => true
      case _ => false
    }

  }

  private def validateAddressHistory: Boolean = {
    this.addressHistory.fold(false) { hist =>
      (
        hist.currentAddress.flatMap(_.timeAtAddress),
        hist.additionalAddress.flatMap(_.timeAtAddress),
        hist.additionalExtraAddress.flatMap(_.timeAtAddress)
        ) match {
        case (Some(ZeroToFiveMonths | SixToElevenMonths), None, None) => false
        case (Some(ZeroToFiveMonths | SixToElevenMonths), Some(ZeroToFiveMonths | SixToElevenMonths), None) => false
        case _ => true
      }
    }
  }

  def isNominatedOfficer: Boolean = positions match {
    case Some(pos) => pos.isNominatedOfficer
    case None => false
  }
}

object ResponsiblePerson {

  def anyChanged(newModel: Seq[ResponsiblePerson]): Boolean =
    newModel exists {
      _.hasChanged
    }

  implicit val formatOption = Reads.optionWithNull[Seq[ResponsiblePerson]]

  def filter(rp: Seq[ResponsiblePerson]): Seq[ResponsiblePerson] =
    rp.filterNot(_.status.contains(StatusConstants.Deleted)).filterNot(_ == ResponsiblePerson())

  def filterWithIndex(rp: Seq[ResponsiblePerson]): Seq[(ResponsiblePerson, Int)] =
    rp.zipWithIndex.filterNot(_._1.status.contains(StatusConstants.Deleted)).filterNot(_._1 == ResponsiblePerson())

  def section(implicit cache: CacheMap): Section = {

    val messageKey = "responsiblepeople"
    val notStarted = Section(messageKey, NotStarted, false, controllers.responsiblepeople.routes.ResponsiblePeopleAddController.get())

    cache.getEntry[Seq[ResponsiblePerson]](key).fold(notStarted) { rp =>

      if (filter(rp).equals(Nil)) {
        Section(messageKey, NotStarted, anyChanged(rp), controllers.responsiblepeople.routes.ResponsiblePeopleAddController.get())
      } else {
        filter(rp) match {
          case responsiblePeople if responsiblePeople.nonEmpty && responsiblePeople.forall {
            _.isComplete
          } => Section(messageKey, Completed, anyChanged(rp), controllers.responsiblepeople.routes.YourAnswersController.get())
          case _ =>
            val index = rp.indexWhere {
              case model if !model.isComplete && !model.status.contains(StatusConstants.Deleted) => true
              case _ => false
            }
            Section(messageKey, Started, anyChanged(rp), controllers.responsiblepeople.routes.WhoMustRegisterController.get(index + 1))
        }
      }
    }

  }

  def findResponsiblePersonByName(name: String, responsiblePeople: Seq[ResponsiblePerson]): Option[(ResponsiblePerson, Int)] = {
    responsiblePeople.zipWithIndex.filter {
      case (p, _) => p.personName.isDefined & !p.status.contains(StatusConstants.Deleted)
    } find {
      case (p, _) => p.personName.fold(false)(_.fullNameWithoutSpace equals name)
    }
  }

  import play.api.libs.functional.syntax._
  import play.api.libs.json._
  import utils.MappingUtils._

  val flowChangeOfficer = "changeofficer"
  val flowFromDeclaration = "fromDeclaration"

  val key = "responsible-people"

  implicit val mongoKey = new MongoKey[ResponsiblePerson] {
    override def apply(): String = key
  }

  def oldPreviousNameReader: Reads[Option[PreviousName]] =
    (__ \ "personName" \ "previousName").readNullable[PreviousName] orElse constant(None)

  def oldPreviousNameChangeDateReader: Reads[Option[LocalDate]] =
    (__ \ "personName" \ "previousName" \ "date").readNullable[LocalDate] orElse constant(None)

  def oldKnownByReader: Reads[Option[KnownBy]] =
    (__ \ "personName" \ "otherNames").readNullable[String] map { maybeName =>
      maybeName.fold[Option[KnownBy]](None)(name => Some(KnownBy(Some(true), Some(name))))
    } orElse constant(None)

  implicit val writes: Writes[ResponsiblePerson] = Json.writes[ResponsiblePerson]

  implicit val reads: Reads[ResponsiblePerson] = {
    (
      (__ \ "personName").readNullable[PersonName] and
        ((__ \ "legalName").readNullable[PreviousName] flatMap {
          case None => oldPreviousNameReader
          case x => constant(x)
        }) and
        ((__ \ "legalNameChangeDate").readNullable[LocalDate] flatMap {
          case None => oldPreviousNameChangeDateReader
          case x => constant(x)
        }) and
        ((__ \ "knownBy").readNullable[KnownBy] flatMap {
          case None => oldKnownByReader
          case x => constant(x)
        }) and
        (__ \ "personResidenceType").readNullable[PersonResidenceType] and
        (__ \ "ukPassport").readNullable[UKPassport] and
        (__ \ "nonUKPassport").readNullable[NonUKPassport] and
        (__ \ "dateOfBirth").readNullable[DateOfBirth] and
        (__ \ "contactDetails").readNullable[ContactDetails] and
        (__ \ "addressHistory").readNullable[ResponsiblePersonAddressHistory] and
        (__ \ "positions").readNullable[Positions] and
        (__ \ "saRegistered").readNullable[SaRegistered] and
        (__ \ "vatRegistered").readNullable[VATRegistered] and
        (__ \ "experienceTraining").readNullable[ExperienceTraining] and
        (__ \ "training").readNullable[Training] and
        (__ \ "hasAlreadyPassedFitAndProper").readNullable[Boolean] and
        (__ \ "hasChanged").readNullable[Boolean].map(_.getOrElse(false)) and
        (__ \ "hasAccepted").readNullable[Boolean].map(_.getOrElse(false)) and
        (__ \ "lineId").readNullable[Int] and
        (__ \ "status").readNullable[String] and
        (__ \ "endDate").readNullable[ResponsiblePersonEndDate] and
        (__ \ "soleProprietorOfAnotherBusiness").readNullable[SoleProprietorOfAnotherBusiness]
      ).tupled.map { t =>
      val r = (ResponsiblePerson.apply _).tupled(t)

      if (hasUkPassportNumber(r)) {
        r.copy(nonUKPassport = None)
      } else {
        if (!hasUkPassportNumber(r) && !hasNonUkPassportNumber(r) && !hasDateOfBirth(r)) {
          r.copy(ukPassport = None, nonUKPassport = None)
        } else r
      }
    }
  }

  private def hasUkPassportNumber(rp: ResponsiblePerson): Boolean = rp.ukPassport match {
    case Some(UKPassportYes(_)) => true
    case _ => false
  }

  private def hasNonUkPassportNumber(rp: ResponsiblePerson): Boolean = rp.nonUKPassport match {
    case Some(NonUKPassportYes(_)) => true
    case _ => false
  }

  private def hasDateOfBirth(rp: ResponsiblePerson): Boolean = rp.dateOfBirth.isDefined

  def default(responsiblePeople: Option[ResponsiblePerson]): ResponsiblePerson =
    responsiblePeople.getOrElse(ResponsiblePerson())

  implicit class FilterUtils(people: Seq[ResponsiblePerson]) {
    def filterEmpty: Seq[ResponsiblePerson] = people.filterNot {
      case _@ResponsiblePerson(None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, _, _, _, _, _, _) => true
      case _ => false
    }
  }

}